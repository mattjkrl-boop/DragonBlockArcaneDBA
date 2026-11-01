#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform DbaDangerConfig {
    float Amount;
};

out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    vec2 p = texCoord - 0.5;
    p.x *= OutSize.x / OutSize.y;
    float d = length(p);
    float rim = smoothstep(0.28, 0.9, d);
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    // Bleed the edges to deep red and pull the center toward gray as it gets worse
    vec3 hurt = mix(c, vec3(l), 0.25 * Amount);
    hurt = mix(hurt, vec3(0.28, 0.02, 0.03), rim * (0.35 + 0.45 * Amount));
    hurt *= 1.0 - rim * 0.25 * Amount;
    fragColor = vec4(mix(c, hurt, Amount), 1.0);
}
