#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform DbaShockConfig {
    float Radius;
    float Amount;
};

out vec4 fragColor;

void main() {
    float aspect = OutSize.x / OutSize.y;
    vec2 p = texCoord - 0.5;
    p.x *= aspect;
    float d = length(p);
    float x = (d - Radius) * 14.0;
    float ring = exp(-x * x) * Amount;
    vec2 dir = d > 0.0001 ? p / d : vec2(0.0);
    vec2 offset = dir * ring * 0.035;
    offset.x /= aspect;
    vec3 c;
    // Small chromatic split along the ring
    c.r = texture(InSampler, texCoord - offset * 1.15).r;
    c.g = texture(InSampler, texCoord - offset).g;
    c.b = texture(InSampler, texCoord - offset * 0.85).b;
    c += vec3(1.0, 0.95, 0.85) * ring * 0.12;
    fragColor = vec4(c, 1.0);
}
