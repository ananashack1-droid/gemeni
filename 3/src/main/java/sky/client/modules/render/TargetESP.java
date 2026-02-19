package sky.client.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sky.client.events.Event;
import sky.client.events.impl.render.EventRender3D;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.SliderSetting;
import sky.client.util.math.MathUtil;

import java.util.ArrayList;
import java.util.List;

@FunctionAnnotation(name = "TargetESP", desc = "Ghosts", type = Type.Render)
public class TargetESP extends Function {

    private final SliderSetting speed = new SliderSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    private final SliderSetting size = new SliderSetting("Размер", 0.2f, 0.05f, 0.5f, 0.01f);

    // Соответствует твоему пути: assets/exosware/images/glow.png
    private static final Identifier GLOW_TEXTURE = Identifier.of("exosware", "images/glow.png");

    private static final List<GhostPhysics> ghostPhysics = new ArrayList<>();
    private static long lastGhostUpdateTimestamp = 0;
    private static long lastTrailUpdateTime = 0;
    private float animAlpha = 0;

    public TargetESP() {
        addSettings(speed, size);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventRender3D e)) return;

        // В 1.21.4 используем метод получения матриц из твоего эвента
        MatrixStack matrices = e.getMatrixStack();

        if (Manager.FUNCTION_MANAGER.attackAura == null) return;
        LivingEntity target = (LivingEntity) Manager.FUNCTION_MANAGER.attackAura.target;

        animAlpha = MathUtil.lerp(animAlpha, (target != null ? 1.0f : 0f), 8.0f);
        if (animAlpha < 0.01f) return;

        drawGhosts2(target, animAlpha, speed.get().floatValue(), matrices, e.getDeltatick().getTickDelta(true));
    }

    private static void initGhosts() {
        ghostPhysics.clear();
        for (int i = 0; i < 3; i++) ghostPhysics.add(new GhostPhysics());
    }

    public void drawGhosts2(LivingEntity lastTarget, float anim, float speed, MatrixStack passedMatrix, float tickDelta) {
        if (lastTarget == null || anim <= 0.01f) return;
        if (ghostPhysics.isEmpty()) initGhosts();

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        double targetX = MathHelper.lerp(tickDelta, lastTarget.lastRenderX, lastTarget.getX());
        double targetY = MathHelper.lerp(tickDelta, lastTarget.lastRenderY, lastTarget.getY());
        double targetZ = MathHelper.lerp(tickDelta, lastTarget.lastRenderZ, lastTarget.getZ());
        targetY += lastTarget.getHeight() / 2.0D;

        long currentTime = System.currentTimeMillis();
        if (lastGhostUpdateTimestamp == 0) lastGhostUpdateTimestamp = currentTime;
        long deltaTime = currentTime - lastGhostUpdateTimestamp;
        lastGhostUpdateTimestamp = currentTime;

        float rotationSpeed = (deltaTime * 0.003f) * speed;

        for (int i = 0; i < ghostPhysics.size(); i++) {
            GhostPhysics ghost = ghostPhysics.get(i);
            ghost.angle += rotationSpeed;
            float currentAngle = ghost.angle + (float) (i * (Math.PI * 2 / 3));

            ghost.position = new Vec3d(
                    targetX + Math.sin(currentAngle) * 0.45,
                    targetY + Math.sin(currentAngle * 2.0) * 0.4,
                    targetZ + Math.cos(currentAngle) * 0.45
            );
        }

        if (currentTime - lastTrailUpdateTime >= 30) {
            lastTrailUpdateTime = currentTime;
            for (GhostPhysics ghost : ghostPhysics) {
                ghost.positionHistory.add(0, ghost.position);
                if (ghost.positionHistory.size() > 12) ghost.positionHistory.remove(12);
            }
        }

        // --- РЕНДЕР СИСТЕМА 1.21.4 ---
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        // Аддитивный блендинг для сочного свечения
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float baseSize = this.size.get().floatValue();

        for (GhostPhysics ghost : ghostPhysics) {
            // Отрисовка хвоста
            for (int i = 0; i < ghost.positionHistory.size(); i++) {
                float trailAnim = 1.0f - ((float) i / ghost.positionHistory.size());
                renderPart(builder, passedMatrix, camera, cameraPos, ghost.positionHistory.get(i), trailAnim * anim * 0.6f, baseSize * trailAnim);
            }
            // Отрисовка основного призрака
            renderPart(builder, passedMatrix, camera, cameraPos, ghost.position, anim, baseSize * 1.1f);
        }

        BuiltBuffer builtBuffer = builder.end();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    private void renderPart(VertexConsumer builder, MatrixStack stack, Camera camera, Vec3d cameraPos, Vec3d pos, float alpha, float scale) {
        stack.push();
        stack.translate((float)(pos.x - cameraPos.x), (float)(pos.y - cameraPos.y), (float)(pos.z - cameraPos.z));

        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        int color = Manager.STYLE_MANAGER.getFirstColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (255 * alpha);

        Matrix4f matrix = stack.peek().getPositionMatrix();

        builder.vertex(matrix, -scale, -scale, 0).texture(0, 0).color(r, g, b, a);
        builder.vertex(matrix, -scale, scale, 0).texture(0, 1).color(r, g, b, a);
        builder.vertex(matrix, scale, scale, 0).texture(1, 1).color(r, g, b, a);
        builder.vertex(matrix, scale, -scale, 0).texture(1, 0).color(r, g, b, a);

        stack.pop();
    }

    private static class GhostPhysics {
        public Vec3d position = Vec3d.ZERO;
        public float angle = 0;
        public final List<Vec3d> positionHistory = new ArrayList<>();
    }
}