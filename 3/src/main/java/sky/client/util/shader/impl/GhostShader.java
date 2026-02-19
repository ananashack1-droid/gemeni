package sky.client.util.shader.impl;

import org.joml.Vector3f;
import sky.client.util.shader.Shader;

public class GhostShader extends Shader {

    public GhostShader() {
        super("effects", "ghost");
    }

    public void setUniforms(float time, float verticalOffset, Vector3f color1, Vector3f color2, float alpha) {
        setUniform1f("u_Time", time);
        setUniform1f("u_VerticalOffset", verticalOffset);
        setUniform3f("u_Color1", color1);
        setUniform3f("u_Color2", color2);
        setUniform1f("u_Alpha", alpha);
    }
}