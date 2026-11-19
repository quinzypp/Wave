#version 330
#extension GL_ARB_separate_shader_objects : require

// The old client's OutlineUtil COMPOSITE shader, 1:1. Outside the mask: a line where a neighbour (within lineWidth px)
// is inside, plus the blurred-mask glow. Inside the mask: nothing (no fill). Alpha-blended onto the world.

uniform sampler2D MaskSampler;
uniform sampler2D GlowSampler;

layout(std140) uniform EspComposite {
    vec4 LineColor;
    vec4 Config; // xy = texel size, z = line width (px), w = glow strength
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    if (texture(MaskSampler, uv).r > 0.5) { fragColor = vec4(0.0); return; }
    float lineWidth = Config.z;
    float edge = 0.0;
    for (float x = -lineWidth; x <= lineWidth; x += 1.0) {
        for (float y = -lineWidth; y <= lineWidth; y += 1.0) {
            edge = max(edge, texture(MaskSampler, uv + vec2(x, y) * Config.xy).r);
        }
    }
    float lineA = LineColor.a * edge;
    float glowA = clamp(texture(GlowSampler, uv).r * Config.w, 0.0, 1.0);
    float outA = lineA + glowA * (1.0 - lineA);
    if (outA <= 0.0) { fragColor = vec4(0.0); return; }
    fragColor = vec4(LineColor.rgb * lineA / outA, outA);
}
