package sky.client.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sky.client.events.Event;
import sky.client.events.impl.render.EventRender3D;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.util.IEntity;
import sky.client.util.color.ColorUtil;
import sky.client.util.render.providers.ResourceProvider;

import java.util.ArrayList;
import java.util.List;

@FunctionAnnotation(name = "Trails", type = Type.Render, desc = "Хвостик за игроком")
public class Trails extends Function {

    private static final double MIN_DISTANCE = 0.05;

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventRender3D e)) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

        MatrixStack ms = e.getMatrixStack();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float tickDelta = e.getDeltatick().getTickDelta(true);

        List<Trail> tail = ((IEntity) mc.player).getTrails();
        List<Trail> toRemove = new ArrayList<>();

        float bigSize = 0.7f;
        float smallSize = 0.3f;

        // ===== RENDER =====
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderTexture(0, ResourceProvider.glow);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int lineSize = tail.size();
        for (int i = 0; i < lineSize; i++) {
            Trail point = tail.get(i);

            point.w -= 0.01f;
            if (point.w <= 0) {
                toRemove.add(point);
                continue;
            }

            float start = Math.min(i / 10f * 2f, 1f);

            double posX = point.pos.x - cameraPos.x;
            double posY = point.pos.y - cameraPos.y;
            double posZ = point.pos.z - cameraPos.z;

            ms.push();
            ms.translate(posX, posY, posZ);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

            Matrix4f matrix = ms.peek().getPositionMatrix();

            int color = ColorUtil.getColorStyle(i * 10);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            // Основное свечение (1.0 * showing)
            drawQuad(buffer, matrix, -bigSize / 2f, -bigSize / 2f, -0.01f, bigSize, bigSize, r, g, b, 255);

            // Внутреннее свечение (0.2 * showing)
            drawQuad(buffer, matrix, -smallSize / 2f, -smallSize / 2f, -0.01f, smallSize, smallSize, r, g, b, 51);

            // Лучи вверх
            float ySize = 50 * start;
            for (int y = 1; y < ySize; y++) {
                float val = 1f - (float) y / ySize;
                float lightSize = 0.3f;
                // val * 0.2 * 255 = очень маленькая альфа
                int lightAlpha = (int) (val * 0.2f * 255);
                drawQuad(buffer, matrix, -lightSize / 2f, -lightSize / 2f, -0.01f - y * 0.015f, lightSize, lightSize, r, g, b, lightAlpha);
            }

            ms.pop();
        }

        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        tail.removeAll(toRemove);

        // Добавляем точку с минимальным расстоянием
        if (mc.player.prevX != mc.player.getX() || mc.player.prevZ != mc.player.getZ()) {
            float x = (float) (mc.player.prevX + (mc.player.getX() - mc.player.prevX) * tickDelta);
            float y = (float) (mc.player.prevY + (mc.player.getY() - mc.player.prevY) * tickDelta);
            float z = (float) (mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * tickDelta);

            Vec3d newPos = new Vec3d(x, y, z);

            // Проверка минимального расстояния
            if (tail.isEmpty() || tail.get(tail.size() - 1).pos.distanceTo(newPos) >= MIN_DISTANCE) {
                tail.add(new Trail(newPos));
            }
        }
    }

    private void drawQuad(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float w, float h, int r, int g, int b, int a) {
        buffer.vertex(matrix, x, y + h, z).texture(0, 1).color(r, g, b, a);
        buffer.vertex(matrix, x + w, y + h, z).texture(1, 1).color(r, g, b, a);
        buffer.vertex(matrix, x + w, y, z).texture(1, 0).color(r, g, b, a);
        buffer.vertex(matrix, x, y, z).texture(0, 0).color(r, g, b, a);
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            ((IEntity) mc.player).getTrails().clear();
        }
        super.onDisable();
    }

    public static class Trail {
        public final Vec3d pos;
        public float w = 1.0f;

        public Trail(Vec3d pos) {
            this.pos = pos;
        }
    }
}