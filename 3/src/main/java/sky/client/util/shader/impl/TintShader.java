package sky.client.util.shader.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import sky.client.manager.IMinecraft;
import sky.client.util.shader.Shader;
import sky.client.util.shader.ShaderHelper;

public class TintShader extends Shader implements IMinecraft {

    private static long initTime = 0L;

    public TintShader() {
        super("effects", "tint");
        initTime = System.nanoTime();
    }

    public static void applyTintPass(Shader shader, SimpleFramebuffer fbo, SimpleFramebuffer copyFbo,
                                     float opacity, int color1, int color2) {
        fbo.beginWrite(true);
        shader.bind();
        shader.setUniform1i("Tex0", 0);
        shader.setUniformBool("UseThemeColors", true);
        shader.setUniform1f("Opacity", opacity);
        shader.setUniform1f("Time", (System.nanoTime() - initTime) / 1_000_000_000.0f);

        shader.setUniform3f("Color1", new org.joml.Vector3f(
                ((color1 >> 16) & 0xFF) / 255f,
                ((color1 >> 8) & 0xFF) / 255f,
                (color1 & 0xFF) / 255f
        ));
        shader.setUniform3f("Color2", new org.joml.Vector3f(
                ((color2 >> 16) & 0xFF) / 255f,
                ((color2 >> 8) & 0xFF) / 255f,
                (color2 & 0xFF) / 255f
        ));
        
        if (mc.player != null) {
            shader.setUniform1f("Yaw", mc.player.getYaw());
            shader.setUniform1f("Pitch", mc.player.getPitch());
        }
        RenderSystem.bindTexture(copyFbo.getColorAttachment());
        ShaderHelper.drawFullScreenQuad();
        shader.unbind();
    }

    public static void applyTintPass(Shader shader, SimpleFramebuffer fbo, SimpleFramebuffer copyFbo,
                                     float opacity, float saturation, float brightness) {
        fbo.beginWrite(true);
        shader.bind();
        shader.setUniform1i("Tex0", 0);
        shader.setUniformBool("UseThemeColors", false);
        shader.setUniform2f("SV", saturation, brightness);
        shader.setUniform1f("Opacity", opacity);
        shader.setUniform1f("Time", (System.nanoTime() - initTime) / 1_000_000_000.0f);
        if (mc.player != null) {
            shader.setUniform1f("Yaw", mc.player.getYaw());
            shader.setUniform1f("Pitch", mc.player.getPitch());
        }
        RenderSystem.bindTexture(copyFbo.getColorAttachment());
        ShaderHelper.drawFullScreenQuad();
        shader.unbind();
    }
}
