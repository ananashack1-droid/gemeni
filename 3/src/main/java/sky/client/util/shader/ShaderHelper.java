package sky.client.util.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import sky.client.manager.IMinecraft;
import sky.client.util.shader.impl.*;

public class ShaderHelper implements IMinecraft {
    private static GaussianShader gaussianShader;
    private static DepthShader depthShader;
    private static PassThroughShader passThroughShader;
    private static TintShader tintShader;
    private static GhostShader ghostShader;

    private static SimpleFramebuffer copyFbo;
    private static SimpleFramebuffer fbo1;
    private static SimpleFramebuffer fbo2;
    private static SimpleFramebuffer depthFbo;
    private static SimpleFramebuffer tintFbo;

    private static boolean initialized = false;

    public static void initShadersIfNeeded() {
        if (initialized) return;
        try {
            gaussianShader = new GaussianShader();
            depthShader = new DepthShader();
            passThroughShader = new PassThroughShader();
            tintShader = new TintShader();
            ghostShader = new GhostShader();
            initialized = true;
        } catch (Exception e) {
            System.err.println("[ExosWare] Failed to initialize shaders!");
            e.printStackTrace();
        }
    }

    public static void checkFramebuffers() {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (copyFbo == null || copyFbo.textureWidth != width || copyFbo.textureHeight != height) {
            if (copyFbo != null) {
                copyFbo.delete();
                fbo1.delete();
                fbo2.delete();
                depthFbo.delete();
                tintFbo.delete();
            }
            copyFbo = new SimpleFramebuffer(width, height, true);
            fbo1 = new SimpleFramebuffer(width, height, true);
            fbo2 = new SimpleFramebuffer(width, height, true);
            depthFbo = new SimpleFramebuffer(width, height, true);
            tintFbo = new SimpleFramebuffer(width, height, true);
        }
    }

    public static void drawFullScreenQuad() {
        RenderSystem.assertOnRenderThread();
        BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(-1.0f, -1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, -1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(-1.0f, 1.0f, 0.0f);
        BufferRenderer.draw(bufferBuilder.end());
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static GaussianShader getGaussianShader() {
        return gaussianShader;
    }

    public static DepthShader getDepthShader() {
        return depthShader;
    }

    public static PassThroughShader getPassThroughShader() {
        return passThroughShader;
    }

    public static TintShader getTintShader() {
        return tintShader;
    }

    public static GhostShader getGhostShader() {
        return ghostShader;
    }

    public static SimpleFramebuffer getCopyFbo() {
        return copyFbo;
    }

    public static SimpleFramebuffer getFbo1() {
        return fbo1;
    }

    public static SimpleFramebuffer getFbo2() {
        return fbo2;
    }

    public static SimpleFramebuffer getDepthFbo() {
        return depthFbo;
    }

    public static SimpleFramebuffer getTintFbo() {
        return tintFbo;
    }
}
