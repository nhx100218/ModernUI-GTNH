#version 120

// Tap budget: the host injects "#define UIB_TAP_BUDGET <13|9>" right after #version,
// and the tap block below is selected by "#if UIB_TAP_BUDGET >= 13" (13 = full tier, byte-identical to the tap code from before tiers existed;
// 9 = eco tier, the 9-tap sunflower-spiral variant). This #ifndef default block is not meant for the host: it keeps the resource
// at full tier 13 when compiled standalone or checked offline; the host-injected define always precedes it, so injection semantics are unchanged.
#ifndef UIB_TAP_BUDGET
#define UIB_TAP_BUDGET 13
#endif

// UI frosted-glass material shader.
//
// Material layering (mirrors iOS UIVisualEffectView composition; the order is not interchangeable):
//   1) blur: sunflower-spiral kernel (continuous radii, kills stray light); tap count selected by UIB_TAP_BUDGET (13 full / 9 eco)
//   2) vibrancy: luma-domain protected saturation boost (not a linear saturation multiply)
//   3) material tint overlay: white/dark translucent layer (after colour correction)
//   4) brightness lift + edge specular + inner top sheen / inner bottom shade
//   5) anti-banding dither: final additive step, before any scaling
//
// Third family (gated by liquidGlass, additive on top of 1)~5); when it is off the refraction offset is exactly 0,
// the rim-light modulation is exactly 1 and the thickness-tint delta is 0 -- numerically identical to the classic tier):
//   6) Liquid Glass: edge convex-lens refraction (SDF-gradient biased sampling) + edge thickness tint ramp
//      + moving rim light (the highlight peak slides along the edge toward the light source; MC has no gyroscope, so light = mouse).
//
// Colour-space contract: the Minecraft framebuffer is sRGB-encoded but everything is mixed linearly; this shader keeps that
// contract and works on raw framebuffer values with no sRGB<->linear round trip (the round trip drops the grey midpoint to
// 0.47 and darkens the whole vanilla UI). Every empirical coefficient below is calibrated in that space.
//
// Compatibility red line: GLSL 1.20 built-ins only. texture2Dbias belongs to ARB_shader_texture_lod
// (a 2009 extension, not a 1.20 built-in); depending on it makes the whole shader fail to compile on machines without that
// extension and silently fall back to the fixed pipeline, which is worse than not using it. Large radii are already
// pre-downsampled by the snapshot downsample + separable filter pass, so a mip bias is redundant anyway.
//
// iosMaterial <= 0.5 falls back to the old "linear saturation multiplier" semantics and adds no tint / specular / dither,
// so existing callers that never opted into material tiers do not silently change appearance.

varying vec2 texCoord;
varying vec2 panelUv;

uniform sampler2D mainTex;
uniform vec2 texelSize;
uniform float blurRadius;
uniform float saturation;
uniform float sourceAlphaPass;
uniform float iosMaterial;
uniform float vibrancy;
uniform vec4 materialTint;
uniform vec3 materialLift;
uniform float edgeHighlight;
uniform float innerLightTop;
uniform float innerShadowBottom;
uniform float noiseAmount;
uniform vec2 panelSizePx;
uniform vec4 cornerRadii;
uniform float kernelJitter;
uniform float liquidGlass;
uniform float refraction;
uniform float edgeTint;
uniform vec2 lightDir;

vec3 applySaturation(vec3 color, float amount) {
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 gray = vec3(luma);
    return clamp(gray + (color - gray) * amount, 0.0, 1.0);
}

// iOS vibrancy: luma-domain protected saturation boost.
//
// Multiplying saturation alone tints shadows and mid-tones together and clips highlights off-colour -- exactly the cheap
// "colour washed over it" feel. Weight by luma: shadows (luma <= 0.224 in this space) stay untouched and
// the brighter the pixel the more saturation it takes -- clarity comes from saturating only what should be saturated.
// t = 1.289*L - 0.289 is the linear-domain form 1.889*L - 0.889 fitted for raw-framebuffer values.
// With vibrancy = 1.0 the multiplier k is exactly 1 (strict identity), which keeps per-tier A/B comparison honest.
vec3 applyVibrancy(vec3 color, float amount) {
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float t = clamp(1.289 * luma - 0.289, 0.0, 1.0);
    float k = 1.0 + (amount - 1.0) * t;
    vec3 gray = vec3(luma);
    return clamp(gray + (color - gray) * k, 0.0, 1.0);
}

// xy is the screen-space outward normal (y points down), z is the signed distance used for coverage.
// A corner arc replaces an existing edge only when it is nearer, so mask, refraction and rim light share one contour.
vec3 applyCornerConstraint(vec3 geometry, vec2 fromCenter, float radius) {
    float radialLength = length(fromCenter);
    float distance = radialLength - radius;
    if (distance > geometry.z)
        return vec3(fromCenter / max(radialLength, 0.0001), distance);
    return geometry;
}

// Radii are pre-normalised by the host against adjacent edge lengths; a legal single large radius may exceed half the short edge.
// Corner order: top-left, top-right, bottom-right, bottom-left. Each corner region is decided independently, never by centre quadrant.
// Zero radius refracts along the nearest straight edge; an inscribed-rectangle approximation makes the normal vanish for the whole panel.
vec3 roundedPanelGeometry(vec2 p, vec2 size, vec4 radii) {
    vec3 geometry = vec3(-1.0, 0.0, -p.x);
    if (p.x - size.x > geometry.z) geometry = vec3(1.0, 0.0, p.x - size.x);
    if (-p.y > geometry.z) geometry = vec3(0.0, -1.0, -p.y);
    if (p.y - size.y > geometry.z) geometry = vec3(0.0, 1.0, p.y - size.y);
    if (p.x < radii.x && p.y < radii.x)
        geometry = applyCornerConstraint(geometry, p - vec2(radii.x), radii.x);
    if (p.x > size.x - radii.y && p.y < radii.y)
        geometry = applyCornerConstraint(geometry, p - vec2(size.x - radii.y, radii.y), radii.y);
    if (p.x > size.x - radii.z && p.y > size.y - radii.z)
        geometry = applyCornerConstraint(geometry, p - (size - vec2(radii.z)), radii.z);
    if (p.x < radii.w && p.y > size.y - radii.w)
        geometry = applyCornerConstraint(geometry, p - vec2(radii.w, size.y - radii.w), radii.w);
    return geometry;
}

// Cheap hash noise: no sin-based hash (driver-specific sin implementations would make the noise distribution hardware-dependent).
float hashNoise(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main(void) {
    // Kernel energy contract: the 13 taps (centre + 12 sunflower-spiral taps) always sum to 1 (/1000 integer form), which keeps frosted glass a
    // "luminance-preserving" operation. The softening commit e5a6b2ae rewrote the kernel and drifted the sum to 1.12, over-exposing
    // frosted glass by 12% and clipping highlights off-colour (the older kernel summed to 1.02, proving normalisation was the intent
    // rather than a style choice); fixed on 2026-09-01, and the sum is anchored by UiBackdropKernelEnergyTest against the
    // source contract (13 taps + sum = 1). Any kernel change must keep both.
    //
    // Kernel shape evolution (2026-09-02 stray-light tuning): regular cross+diagonal -> two-radius Poisson disc -> sunflower spiral.
    // The stray light of the first two versions (a "starry" look at large radii, like astigmatism) came from tap radii having only a few discrete steps:
    // the two-radius disc squeezed its 12 outer taps onto r~0.45 and r~0.9 rings, so the blur read as "centre + two bright rings";
    // rotation is a rigid transform and cannot fix radial banding (a ring is rotationally symmetric, so rotating it changes nothing). The sunflower spiral
    // (r=sqrt(i/12)*1.6 for equal area, golden-angle step 2.39996 rad) puts the 13 taps on 13
    // continuous radii with a golden-angle distribution and Gaussian weights decaying monotonically with radius -- radial energy flattens out and the bright rings vanish;
    // the residual angular arm structure of the spiral is exactly what the per-pixel rotation below breaks up (rotation is useless for rings but works on a spiral).
    // 0.98 is the tap-radius compensation, calibrated against the **weighted RMS radius** (blur strength is decided by the integral, not the extreme):
    // the uncompensated spiral kernel has RMS=0.9294, 0.98 brings it to 0.9108, only -0.07% off the old regular-kernel baseline 0.91148,
    // which preserves the author-side blurRadius feel. That RMS is locked by the kernel guard assertion: changing the kernel silently changes blur
    // strength, so the contract has to be updated in step.
    vec2 radiusStep = texelSize * clamp(blurRadius, 0.0, 128.0) * 0.98;

    // Panel geometry must be computed before sampling: Liquid Glass lens refraction biases the sampling coordinates.
    // Coverage, refraction and rim light share the distance and analytic normal of the full four-corner contour.
    vec2 halfSize = max(panelSizePx * 0.5, vec2(1.0, 1.0));
    vec3 panelGeometry = roundedPanelGeometry(panelUv * panelSizePx, panelSizePx, cornerRadii);
    float signedDistance = panelGeometry.z;
    float edgeDistance = max(-signedDistance, 0.0);
    // Coverage transition of one final screen pixel; it must not step across several logical pixels when the HUD is scaled up.
    // fwidth also tracks ancestor transforms; derivatives are computed before any dynamic branch or discard.
    float edgeWidth = max(fwidth(signedDistance), 0.0001);
    float coverage = clamp(0.5 - signedDistance / edgeWidth, 0.0, 1.0);

    // Rotate the whole sampling disc per pixel: with a fixed kernel every pixel shows the same "starry" pattern at large radii,
    // which reads as plastic or waxy once accumulated. Giving each pixel a deterministic disc rotation angle scatters the structured
    // artefact into high-frequency noise (hidden again by the final dither). Crucially it depends only on gl_FragCoord with no time term,
    // so a static frame never flickers -- better than jittering the offset or introducing a frame-number phase.
    // The old-semantics path (kernelJitter=0) stays an identity basis, pixel-identical before and after the upgrade.
    mat2 kernelBasis = mat2(1.0, 0.0, 0.0, 1.0);
    if (kernelJitter > 0.5) {
        // Hash the offset sampling domain so it is decoupled from the hashNoise(gl_FragCoord.xy) used by the final dither;
        // otherwise the rotation angle and the noise value of the same pixel correlate and reveal a regular pattern.
        float kernelAngle = hashNoise(gl_FragCoord.xy + vec2(37.0, 91.0)) * 6.28318530718;
        float ka = cos(kernelAngle);
        float kb = sin(kernelAngle);
        kernelBasis = mat2(ka, kb, -kb, ka);
    }

    // Liquid Glass edge refraction: the rounded rect behaves like a thick beveled glass edge -- background near the edge is
    // "pulled outward" past the contour and compressed into the rim band, producing the lens feel (the decisive difference
    // between official Liquid Glass and classic frosted glass). Sampling centres are offset along the analytic outward normal
    // of the coverage contour, pushed further the closer to the edge; the centre region is zeroed by lensBevel and stays flat.
    // lensShift is a UV-space offset: refraction is counted in texels (the host already divided the author-side screen-pixel count
    // by downsampleFactor) and multiplied by texelSize to convert to UV, the same convention as radiusStep, so the
    // on-screen feel does not jump when the snapshot downsample tier changes.
    float lensBevel = 0.0;
    vec2 sdfGradient = vec2(0.0);
    vec2 lensShift = vec2(0.0);
    // Rim width is hoisted to the outer scope: the liquid specular band width is taken as a ratio of it (see the border section below).
    float lensBandPx = 0.0;
    if (liquidGlass > 0.5) {
        // Rim width: ratio 0.35 dominates and the clamps only cover extremes. The 28px upper bound stops a large panel from being all edge
        // (a 164px short edge at ratio 0.85 would give a 70px rim, thinning refraction and thickness tint over the whole surface
        // until the look degrades back to plain frosted glass -- Liquid Glass is recognisable precisely because only the edge bulges).
        // Lowering the floor from 8px to 3px is the root cause of "the liquid is invisible" on device: a chat bubble has a 28px short edge, half-height 14,
        // and the 8px floor pushed the rim to 57% of the half-height, making bevel almost constantly 1 inside the bubble -- while a "uniform
        // displacement" is invisible (it is equivalent to translating the sampling coordinates); the gradient is the lens. The floor must stay as small as
        // possible while keeping a flat centre, hence the extra shortHalf*0.5 clamp: for any panel size the centre bevel is exactly 0.
        float lensShortHalf = min(halfSize.x, halfSize.y);
        lensBandPx = min(clamp(lensShortHalf * 0.35, 3.0, 28.0), lensShortHalf * 0.5);
        lensBevel = 1.0 - smoothstep(0.0, lensBandPx, edgeDistance);
        lensBevel = lensBevel * lensBevel;
        sdfGradient = panelGeometry.xy;
        // Screen y grows downward and snapshot V grows upward; flip y only when converting to sampling UV.
        // The light direction still uses the screen-space normal and must not follow the texture flip.
        lensShift = vec2(sdfGradient.x, -sdfGradient.y) * lensBevel * refraction * texelSize;
    }

    // Zero blur keeps refraction/material but samples once; never clamp the radius to 1 or accumulate the same point repeatedly.
    vec4 blurred = texture2D(mainTex, texCoord + lensShift);
    if (blurRadius > 0.0) {
        // Tap block selected by UIB_TAP_BUDGET (the define is injected by the host after #version):
        //   13 tier = byte-identical sunflower-spiral 13-tap code from before tiers existed, weight sum 1000/1000;
        //    9 tier = variant kernel (centre + 8, r=sqrt(i/8)*1.6, golden angle 2.39996, weights likewise integers /1000).
        // 9-tap tier contract: the weight sum is still exactly 1000/1000 (luminance-preserving contract unchanged); farthest tap 1.6001 steps vs 13-tier
        // 1.5996 steps (same order of coverage radius); weighted RMS radius 0.9288 vs 0.9295; after the 0.98 tap-radius
        // compensation the equivalent blur strength differs by -0.07% (same blur strength, fewer taps);
        // fragment samples 9/13 = -30.8%. Both tiers' weights/radii are pinned by UiBackdropKernelEnergyTest,
        // values derived in temp/perf-impl-render/kernel-9tap.py (Python recomputation) + kernel-9tap.json.
#if UIB_TAP_BUDGET >= 13
        blurred *= (161.0 / 1000.0);

        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-0.341, 0.312) * radiusStep) * (139.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(0.057, -0.651) * radiusStep) * (120.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(0.487, 0.635) * radiusStep) * (103.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-0.910, -0.161) * radiusStep) * (89.0 / 1000.0);

        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(0.871, -0.554) * radiusStep) * (77.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-0.294, 1.093) * radiusStep) * (66.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-0.563, -1.084) * radiusStep) * (57.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(1.227, 0.448) * radiusStep) * (49.0 / 1000.0);

        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-1.281, 0.529) * radiusStep) * (43.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(0.619, -1.323) * radiusStep) * (37.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(0.458, 1.462) * radiusStep) * (32.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-1.384, -0.802) * radiusStep) * (27.0 / 1000.0);
#else
        blurred *= (221.0 / 1000.0);

        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-0.417, 0.382) * radiusStep) * (180.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(0.070, -0.797) * radiusStep) * (146.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(0.596, 0.778) * radiusStep) * (119.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-1.114, -0.197) * radiusStep) * (97.0 / 1000.0);

        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(1.067, -0.679) * radiusStep) * (79.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-0.360, 1.338) * radiusStep) * (64.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(-0.690, -1.328) * radiusStep) * (52.0 / 1000.0);
        blurred += texture2D(mainTex, texCoord + lensShift + kernelBasis * vec2(1.503, 0.549) * radiusStep) * (42.0 / 1000.0);
#endif
    }

    vec3 color;
    if (iosMaterial > 0.5) {
        // Material grading order: vibrancy -> tint overlay -> brightness lift. Colour correction first, overlay second,
        // which is what makes it both clear and iOS-milky; the reverse would saturate the tint as well.
        color = applyVibrancy(blurred.rgb, vibrancy);
        // The white tint must be gated by background luma, otherwise a dark background necessarily turns grey: mix(c, 1, a) lifts black from 0 to
        // a (a=0.2 in this tier), i.e. it throws away 20% of the dynamic range -- that is the mathematical essence of "washed-out grey",
        // not a parameter problem. A dark tint only pushes down and does not hurt blacks, so it needs no gate. iOS really switches to a dark
        // material on dark backgrounds (trait aware); here a luma gate approximates that behaviour:
        // dark backgrounds get almost no white (stays clear) and bright backgrounds keep the milkiness (text stays readable).
        // The direction comes from the tint's own luma, adding no uniform.
        float tintLuma = dot(materialTint.rgb, vec3(0.2126, 0.7152, 0.0722));
        float backdropLuma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        float whiteGate = mix(1.0, smoothstep(0.05, 0.55, backdropLuma), step(0.5, tintLuma));
        // Thickness tint is a relative increase of the base material's absorption and must not be composited as an independent alpha.
        // A thin dark large panel has a wide refraction band, and an independent dark overlay would paint the whole rim black;
        // on small buttons the bevel highlight hides it, so buttons alone are not a valid panel-material acceptance test.
        // The luma gate keeps protecting shadows; centre lensBevel=0 and classic-tier edgeTint=0 both keep the original colour.
        float thicknessGate = smoothstep(0.15, 0.55, backdropLuma);
        float thicknessAlpha = materialTint.a * edgeTint * lensBevel * thicknessGate;
        color = mix(color, materialTint.rgb,
                clamp((materialTint.a + thicknessAlpha) * whiteGate, 0.0, 1.0));
        // The brightness compensation is gated too: without the gate the tint does not lift blacks while the lift does, so a grey background is washed out anyway.
        color = color + materialLift * whiteGate;
    } else {
        color = applySaturation(blurred.rgb, saturation);
    }

    // -- Edge specular: the two paths differ in shape, deliberately -----------------------------------
    // Classic tier: the 1.5px hairline of an iOS navigation bar, highlight concentrated on the top edge and decaying to the sides
    //   (band width from the SDF distance, exact at corners; the old min(distance to straight edge) approximation cut the arc segments off).
    // Liquid tier: **Gaussian band with the peak moved inward + counter highlight**, shape taken from the first-hand WebGlass reference
    //   docs/specular.md + docs/tokens.md (--wg-specular-edge 0.05 / --wg-specular-width
    //   0.25 / --wg-specular-back 0.20, and it states explicitly that the counter-highlight stays locked at light-angle+180 degrees).
    float borderBand = 1.0 - smoothstep(0.0, 1.5, edgeDistance);
    float borderWeight = borderBand * mix(0.30, 1.0, 1.0 - clamp(panelUv.y, 0.0, 1.0));
    if (liquidGlass > 0.5) {
        // Root cause of the on-device "hard edge" report: the previous liquid tier wrongly reused the classic shape --
        //   1 - smoothstep(0, 2px, d) puts its **peak exactly on the physical contour**, only 2px wide.
        // Measured there: luma 42 -> 145 within 1px (blue clipped straight to 255), which reads as "a white line
        // drawn along the contour" rather than "glass bulging at the edge". The reference implementation has two mechanisms, each solving half:
        //   (a) specular-edge: move the peak **inward** so the contour line is not the brightest point -> removes the outline feel;
        //   (b) specular-width: take the band as a ratio of the bezel (default 0.25) instead of a fixed 2px -> the same
        //       energy spread over a wider shoulder, so "soft" comes from distribution rather than lower total brightness.
        // Gaussian instead of smoothstep: the latter has zero slope at the band end but still peaks on the edge, the former has natural shoulders.
        float specBandPx = max(2.5, lensBandPx * 0.25);
        float specT = (edgeDistance - specBandPx * 0.35) / specBandPx;
        float specLobe = exp(-4.0 * specT * specT);
        // Moving rim light: MC has no gyroscope, the host uses the mouse as the light source (official semantics: lighting responds to
        // device motion). pow 1.5 gives the highlight direction without shrinking it to a point.
        float nDotL = dot(sdfGradient, lightDir);
        float primary = pow(max(nDotL, 0.0), 1.5);
        // Counter highlight: the weak reflection on the back-lit side of real glass. Without it the back-lit rim is only "dead" and "dark"
        // (half of the earlier "black and lustreless" report). Strength uses the reference default 0.20.
        float counter = 0.20 * pow(max(-nDotL, 0.0), 1.5);
        // 0.25 ambient floor: keeps a hint of polish even on unlit sides, so no angle ends up completely dark.
        borderWeight = specLobe * (0.25 + primary + counter);
    }

    // Inner top sheen + inner bottom shade: approximation of specular reflection and thickness (pow3 keeps energy at the edge).
    float topGlow = pow(1.0 - clamp(panelUv.y, 0.0, 1.0), 3.0) * innerLightTop;
    float bottomShade = pow(clamp(panelUv.y, 0.0, 1.0), 3.0) * innerShadowBottom;

    color = color + vec3(topGlow) - vec3(bottomShade) + vec3(borderWeight * edgeHighlight);

    // Anti-banding dither: must be the final additive step, after the large-radius blur and before any scaling or gamma.
    // An 8-bit framebuffer necessarily shows quantisation bands on a smooth gradient. TPDF (sum of two independent uniform sources,
    // triangular distribution) removes gradient bands better than uniform noise at equal peak; Zed's gradient dither PR uses this method.
    // The second hash uses a transposed+offset domain so the two paths cannot correlate and degrade back to uniform.
    if (noiseAmount > 0.0) {
        float n = hashNoise(gl_FragCoord.xy)
                + hashNoise(gl_FragCoord.yx * 1.03 + vec2(7.0, 13.0)) - 1.0;
        color = clamp(color + n * noiseAmount, 0.0, 1.0);
    }

    // First pass coverage controls RGB replacement; the isolated-layer second pass only rewrites the original snapshot alpha with the RGB write mask off.
    float outputAlpha = mix(coverage, blurred.a * coverage, sourceAlphaPass);
    gl_FragColor = vec4(clamp(color, 0.0, 1.0), outputAlpha);
}