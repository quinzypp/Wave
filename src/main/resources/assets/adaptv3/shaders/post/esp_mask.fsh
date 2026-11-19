#version 330
#extension GL_ARB_separate_shader_objects : require

// ESP mask: solid coverage of the union of every target's on-screen hitbox silhouette (convex polygons).
// Each edge is (point, outward unit normal) in physical pixels, y down; Hulls[i] = (first edge, edge count).

layout(std140) uniform EspShapes {
    vec4 Info;        // x = hull count, y = screen width, z = screen height
    vec4 Hulls[32];
    vec4 Edges[256];
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 p = vec2(texCoord.x * Info.y, (1.0 - texCoord.y) * Info.z);
    float cover = 0.0;
    int hulls = int(Info.x);
    for (int h = 0; h < hulls; h++) {
        int first = int(Hulls[h].x), count = int(Hulls[h].y);
        // signed distance to a convex polygon: the furthest edge plane
        float sd = -1e9;
        for (int e = 0; e < count; e++) {
            vec4 edge = Edges[first + e];
            sd = max(sd, dot(p - edge.xy, edge.zw));
        }
        // solid, like the old GL_POLYGON mask: a pixel is in when its centre is
        if (sd <= 0.0) cover = 1.0;
    }
    fragColor = vec4(cover, cover, cover, 1.0);
}
