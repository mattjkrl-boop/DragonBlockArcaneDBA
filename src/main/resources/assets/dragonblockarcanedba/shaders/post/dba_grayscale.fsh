#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform DbaGrayConfig {
    float Amount;
};

out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    // Slight cool tint on the frozen frame
    vec3 gray = vec3(l * 0.95, l * 0.98, l * 1.06);
    fragColor = vec4(mix(c, gray, Amount), 1.0);
}
