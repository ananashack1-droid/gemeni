package sky.client.util.shader.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.joml.Vector3f;
import sky.client.util.shader.Shader;
import sky.client.util.shader.ShaderHelper;

public class GaussianShader extends Shader {
    
    public GaussianShader() {
        super("blurs", "gaussian");
    }

    public static void applyGaussianBlur(Shader shader, SimpleFramebuffer fbo1, SimpleFramebuffer fbo2, 
                                         SimpleFramebuffer tintFbo, SimpleFramebuffer copyFbo, 
                                         float strength, boolean linearSampling, boolean usePuke) {
        Vector3f gaussian = calculateGaussian(strength);
        int support = (int) Math.ceil(strength * 3);

        shader.bind();
        shader.setUniform1i("Tex0", 0);
        shader.setUniformBool("Alpha", false);
        shader.setUniform3f("Gaussian", gaussian);
        shader.setUniform1i("Support", support);
        shader.setUniformBool("LinearSampling", linearSampling);

        fbo1.beginWrite(true);
        shader.setUniform2f("Direction", 1.0f, 0.0f);
        shader.setUniform2f("TexelSize", 1.0f / fbo1.textureWidth, 1.0f / fbo1.textureHeight);
        int sourceTexture = usePuke ? tintFbo.getColorAttachment() : copyFbo.getColorAttachment();
        RenderSystem.bindTexture(sourceTexture);
        ShaderHelper.drawFullScreenQuad();

        fbo2.beginWrite(true);
        shader.setUniform2f("Direction", 0.0f, 1.0f);
        shader.setUniform2f("TexelSize", 1.0f / fbo2.textureWidth, 1.0f / fbo2.textureHeight);
        RenderSystem.bindTexture(fbo1.getColorAttachment());
        ShaderHelper.drawFullScreenQuad();
        
        shader.unbind();
    }

    public static Vector3f calculateGaussian(float sigma) {
        final float sqrt2PI = 2.50662f;
        float y = (float) Math.exp(-0.5f / (sigma * sigma));
        return new Vector3f(1 / (sqrt2PI * sigma), y, y * y);
    }
}
