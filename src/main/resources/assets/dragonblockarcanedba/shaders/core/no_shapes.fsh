#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 localPos;
in vec3 viewNormal;
in vec3 viewPos;

out vec4 fragColor;

// Seeded value noise, three octaves used below. Cheap, tileable enough at
// effect scales, and stable: the same world position always hashes to the
// same value, so erosion patterns stay glued to the world while GameTime
// scrolls the sampling offset through them.
float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.17, 0.13));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float vnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i), hash(i + vec3(1.0, 0.0, 0.0)), f.x),
            mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
        mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x),
            mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y),
        f.z);
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 4; i++) {
        value += amplitude * vnoise(p);
        p = p * 2.13 + vec3(7.7, 3.1, 5.3);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);

    // Life input: the caller's vertex alpha, driven by FxEnvelope at every
    // existing call site. As the envelope falls, the dissolve threshold rises
    // through the noise field and the surface erodes in patches.
    float life = clamp(vertexColor.a, 0.0, 1.0);

    // Animated material field. Two fbm reads scrolled in opposite directions
    // by the vanilla Globals clock (GameTime), the same input that animates
    // rendertype_end_portal, so the erosion churns instead of sliding.
    vec3 flow = vec3(GameTime * 0.09, -GameTime * 0.35, GameTime * 0.13);
    float coarse = fbm(localPos * 1.7 + flow);
    float fine = fbm(localPos * 4.3 - flow * 1.7);
    float material = clamp(coarse * 0.72 + fine * 0.48, 0.0, 1.0);

    float threshold = (1.0 - life) * 0.92;
    float body = smoothstep(threshold, threshold + 0.38, material + tex.a * 0.22);
    if (body <= 0.002) {
        discard;
    }

    // Guards: a zero normal or a vertex sitting on the camera would make the
    // normalizes below undefined. Fall back to a head-on read for that
    // fragment instead of producing NaNs on strict drivers.
    float normalLength = length(viewNormal);
    vec3 normal = normalLength > 0.0001 ? viewNormal / normalLength : vec3(0.0, 0.0, 1.0);
    float viewLength = length(viewPos);
    vec3 viewDir = viewLength > 0.0001 ? -viewPos / viewLength : vec3(0.0, 0.0, 1.0);
    float facing = abs(dot(normal, viewDir));
    float fresnel = pow(1.0 - facing, 2.0);

    vec3 color = tex.rgb * vertexColor.rgb * ColorModulator.rgb;
    // Internal motion: the fine octave breathes the brightness, so a held
    // effect never reads as a flat lit sheet.
    color *= 0.86 + 0.28 * fine;

    float alpha = tex.a * life * body;

#ifdef RIM_GLOW
    // Additive variant. Its blend factors are ONE/ONE (javap-verified), so
    // the alpha channel cannot modulate the result: brightness is the
    // envelope. The fresnel rim adds the hot edge energy lives on.
    color += color * fresnel * 1.6;
    color *= alpha;
#endif

#ifdef SOFT_SILHOUETTE
    // Alpha variant. Fade the silhouette where the surface turns away from
    // the camera, so soft bodies have no hard mesh edge.
    alpha *= 0.35 + 0.65 * pow(facing, 1.5);
#endif

    vec4 result = vec4(color, alpha);
    fragColor = apply_fog(result, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
