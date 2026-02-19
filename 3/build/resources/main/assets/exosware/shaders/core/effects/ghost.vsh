#version 330 core

layout(location = 0) in vec3 a_Pos;
layout(location = 1) in vec3 a_Normal;

uniform mat4 u_ModelViewMat;
uniform mat4 u_ProjMat;
uniform float u_Time;
uniform float u_VerticalOffset;

out vec3 v_Normal;
out float v_Fade;

void main() {
    vec3 pos = a_Pos;

    pos.y += sin(u_Time * 2.0 + pos.x * 5.0) * 0.2;
    pos.y += u_VerticalOffset;

    gl_Position = u_ProjMat * u_ModelViewMat * vec4(pos, 1.0);
    v_Normal = mat3(transpose(inverse(u_ModelViewMat))) * a_Normal;
    v_Fade = sin((a_Pos.x + u_Time) * 3.14159);
}
