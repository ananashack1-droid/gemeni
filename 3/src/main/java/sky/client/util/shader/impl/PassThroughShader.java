package sky.client.util.shader.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import sky.client.manager.IMinecraft;
import sky.client.util.shader.Shader;
import sky.client.util.shader.ShaderHelper;

public class PassThroughShader extends Shader implements IMinecraft {
    
    public PassThroughShader() {
        super("effects", "passthrough");
    }

    public static void renderToScreen(Shader shader, SimpleFramebuffer fbo) {
        mc.getFramebuffer().beginWrite(false);
        shader.bind();
        shader.setUniform1i("Tex0", 0);
        shader.setUniformBool("Alpha", true);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.bindTexture(fbo.getColorAttachment());
        ShaderHelper.drawFullScreenQuad();

        RenderSystem.disableBlend();
        shader.unbind();
    }
}
