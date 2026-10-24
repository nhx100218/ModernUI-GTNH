#version 120

//
varying vec2 vLocal;

void main() {
    vLocal = gl_MultiTexCoord0.xy;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}

