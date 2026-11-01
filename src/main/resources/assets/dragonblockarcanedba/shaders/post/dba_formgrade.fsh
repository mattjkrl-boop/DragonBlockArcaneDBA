#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform DbaFormConfig {
    vec3 Tint;
    float Amount;
};

out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    vec2 p = texCoord - 0.5;
    p.x *= OutSize.x / OutSize.y;
    float d = length(p);
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    // Signature grade: pull the frame toward the form's color, keep luma structure,
    // and burn the rim with the tint so the form's aura color owns the screen edges.
    vec3 graded = mix(c, Tint * (0.3 + l * 1.15), 0.38);
    graded = mix(vec3(l), graded, 1.12);
    float rim = smoothstep(0.35, 0.95, d);
    graded += Tint * rim * rim * 0.16;
    fragColor = vec4(mix(c, graded, Amount), 1.0);
}
