#version 120

varying vec2 texCoord;
varying vec2 panelUv;

// panelOrigin/panelSizePx compute the panel-local uv (the coordinate basis for edge specular and inner sheen).
// gl_Vertex and panelOrigin live in the same model space (both come from the host's left/top/right/bottom),
// so the subtraction cancels the GUI scale: panelUv is scale-independent and edge width is in logical pixels.
// panelSizePx is declared with the same name and type in the vertex and fragment stages; GLSL treats it as one program uniform, set once.
uniform vec2 panelOrigin;
uniform vec2 panelSizePx;

void main(void) {
    gl_Position = ftransform();
    texCoord = gl_MultiTexCoord0.xy;
    panelUv = (gl_Vertex.xy - panelOrigin) / max(panelSizePx, vec2(1.0, 1.0));
}
