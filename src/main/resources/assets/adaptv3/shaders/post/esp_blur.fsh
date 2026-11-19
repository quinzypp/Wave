#version 330
#extension GL_ARB_separate_shader_objects : require

// The old client's OutlineUtil BLUR shader, 1:1: separable gaussian on the mask (red channel).

uniform sampler2D InSampler;

layout(std140) uniform EspBlur {
    vec4 Config; // xy = texelSize * direction, z = radius (px)
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    float radius = Config.z;
    float sigma = max(radius / 2.0, 0.5);
    float sum = 0.0, total = 0.0;
    for (float i = -radius; i <= radius; i += 1.0) {
        float weight = exp(-(i * i) / (2.0 * sigma * sigma));
        sum += texture(InSampler, uv + i * Config.xy).r * weight;
        total += weight;
    }
    float v = sum / total;
    fragColor = vec4(v, v, v, 1.0);
}
