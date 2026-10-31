#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform DbaEclipseConfig {
    float Amount;
};

out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    vec2 p = texCoord - 0.5;
    p.x *= OutSize.x / OutSize.y;
    float d = length(p);
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    // Desaturate toward a deep violet, darken, and add a crimson rim vignette
    vec3 tinted = mix(c, vec3(l) * vec3(0.55, 0.35, 0.75), 0.6) * 0.65;
    float rim = smoothstep(0.35, 0.95, d);
    tinted = mix(tinted, vec3(0.35, 0.02, 0.08), rim * 0.7);
    fragColor = vec4(mix(c, tinted, Amount), 1.0);
}
