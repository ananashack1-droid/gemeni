package sky.client.util.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    public final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public Shader(String path, String shaderName) {
        int program = glCreateProgram();

        try {
            String basePath = "/assets/exosware/shaders/core/" + path + "/";
            String vertexShaderSource = readShaderSource(basePath + shaderName + ".vsh");
            String fragmentShaderSource = readShaderSource(basePath + shaderName + ".fsh");

            glAttachShader(program, createShader(vertexShaderSource, GL_VERTEX_SHADER));
            glAttachShader(program, createShader(fragmentShaderSource, GL_FRAGMENT_SHADER));

            glLinkProgram(program);

            if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
                throw new IllegalStateException("Shader linking failed: " + glGetProgramInfoLog(program));
            }
        } catch (Exception e) {
            glDeleteProgram(program);
            throw new RuntimeException("Failed to create shader: " + shaderName, e);
        }

        this.programId = program;
    }

    private String readShaderSource(String path) throws Exception {
        InputStream stream = Shader.class.getResourceAsStream(path);
        if (stream == null) throw new Exception("Shader file not found: " + path);
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }

    private int createShader(String source, int type) throws Exception {
        int shader = glCreateShader(type);
        if (shader == 0) {
            throw new Exception("Error creating shader of type " + type);
        }
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling shader type " + type + ": " + glGetShaderInfoLog(shader, 1024));
        }
        return shader;
    }

    public void bind() {
        glUseProgram(this.programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    private int getUniformLocation(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(this.programId, n));
    }

    public void setUniform1i(String name, int value) {
        RenderSystem.assertOnRenderThread();
        glUniform1i(getUniformLocation(name), value);
    }

    public void setUniform1f(String name, float value) {
        RenderSystem.assertOnRenderThread();
        glUniform1f(getUniformLocation(name), value);
    }

    public void setUniform2f(String name, float v1, float v2) {
        RenderSystem.assertOnRenderThread();
        glUniform2f(getUniformLocation(name), v1, v2);
    }

    public void setUniform2f(String name, Vector2f vector) {
        RenderSystem.assertOnRenderThread();
        glUniform2f(getUniformLocation(name), vector.x, vector.y);
    }

    public void setUniform3f(String name, Vector3f vector) {
        RenderSystem.assertOnRenderThread();
        glUniform3f(getUniformLocation(name), vector.x, vector.y, vector.z);
    }

    public void setUniformMatrix4f(String name, boolean transpose, Matrix4f matrix) {
        RenderSystem.assertOnRenderThread();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(getUniformLocation(name), transpose, buffer);
        }
    }

    public void setUniformBool(String name, boolean value) {
        setUniform1i(name, value ? 1 : 0);
    }

    public void delete() {
        glDeleteProgram(this.programId);
    }
}
