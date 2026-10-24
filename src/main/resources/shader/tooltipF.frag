#version 120

// SDF 圆角矩形：抗锯齿外缘 + 四角渐变边框环 + 四角渐变填充。输出预乘 alpha。
varying vec2 vLocal;

uniform vec2 uHalfSize;
uniform float uRadius;
uniform float uBorderWidth;
uniform vec4 uF0; // 填充 左上
uniform vec4 uF1; // 填充 右上
uniform vec4 uF2; // 填充 右下
uniform vec4 uF3; // 填充 左下
uniform vec4 uC0; // 边框 左上
uniform vec4 uC1; // 边框 右上
uniform vec4 uC2; // 边框 右下
uniform vec4 uC3; // 边框 左下

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

vec4 sampleCorners(vec4 c0, vec4 c1, vec4 c2, vec4 c3, vec2 t) {
    return mix(mix(c0, c1, t.x), mix(c3, c2, t.x), t.y);
}

void main() {
    float d = sdRoundBox(vLocal, uHalfSize, uRadius);
    float aa = max(fwidth(d), 0.0001) * 0.5;
    float outer = 1.0 - smoothstep(-aa, aa, d);
    float innerD = d + uBorderWidth;
    float inner = 1.0 - smoothstep(-aa, aa, innerD);
    float ring = max(outer - inner, 0.0);

    vec2 t = clamp(vLocal / uHalfSize * 0.5 + 0.5, 0.0, 1.0);
    vec4 fill = sampleCorners(uF0, uF1, uF2, uF3, t);
    vec4 border = sampleCorners(uC0, uC1, uC2, uC3, t);

    vec4 fillP = vec4(fill.rgb, 1.0) * fill.a * inner;
    vec4 borderP = vec4(border.rgb, 1.0) * border.a * ring;
    gl_FragColor = fillP + borderP;
}
