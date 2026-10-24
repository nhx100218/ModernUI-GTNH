#version 120

// tooltip 圆角矩形：gl_Vertex 为屏幕坐标，gl_MultiTexCoord0 为相对矩形中心的局部坐标。
varying vec2 vLocal;

void main() {
    vLocal = gl_MultiTexCoord0.xy;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
