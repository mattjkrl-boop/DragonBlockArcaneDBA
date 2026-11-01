#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform DbaSurgeConfig {
    float Amount;
};

out vec4 fragColor;

void main() {
    vec2 p = texCoord - 0.5;
    p.x *= OutSize.x / OutSize.y;
    float d = length(p);
    float rim = smoothstep(0.38, 0.92, d);
    // Chromatic fringe grows toward the edges as the charge builds
    vec2 dir = d > 0.0001 ? (texCoord - 0.5) / d : vec2(0.0);
    vec2 off = dir * rim * 0.006 * Amount;
    vec3 c;
    c.r = texture(InSampler, texCoord + off).r;
    c.g = texture(InSampler, texCoord).g;
    c.b = texture(InSampler, texCoord - off).b;
    // Saturation and contrast lift, then a warm energy burn at the rim
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, 1.0 + 0.35 * Amount);
    c = (c - 0.5) * (1.0 + 0.12 * Amount) + 0.5;
    c += vec3(1.0, 0.82, 0.5) * rim * rim * 0.22 * Amount;
    fragColor = vec4(c, 1.0);
}
