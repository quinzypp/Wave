#version 330
#extension GL_ARB_separate_shader_objects : require

// The main menu's backdrop, in the style of the Adapt logo background: clean matte pearl blobs on dark violet.
// Large blobs hug the screen edges, small droplets wander between them, and any that touch melt together
// (smooth-min union of their distance fields). Each blob is softly lit from the top-left.

layout(std140) uniform MenuNebula {
    vec4 Params; // xy = resolution (px), z = time (s), w = 1 for the soft centre lift
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

// polynomial smooth minimum: blends two distance fields so touching blobs merge into one liquid shape
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// one organic blob: an ellipse whose radius wobbles slowly around its rim
float blob(vec2 p, vec2 c, vec2 r, float t, float seed) {
    vec2 q = (p - c) / r;
    float ang = atan(q.y, q.x);
    float wob = 1.0 + 0.10 * sin(2.0 * ang + t * 0.35 + seed) + 0.06 * sin(3.0 * ang - t * 0.25 + seed * 2.3);
    return (length(q) - wob) * min(r.x, r.y);
}

// slow wandering offset around a home position
vec2 drift(vec2 home, float amp, float t, float seed) {
    return home + amp * vec2(sin(t * 0.13 + seed), cos(t * 0.11 + seed * 1.7));
}

// half the screen's extent in blob space (set in main), so droplets know where the borders are
vec2 gHalf;

// triangle wave in [-1, 1] with slope +-1: a straight path that reflects at both ends
float tri(float u) {
    return abs(mod(u + 1.0, 4.0) - 2.0) - 1.0;
}

// a droplet that travels across the screen and bounces off its borders. Near a wall it squashes flat against it
// (narrower along the wall's normal, wider along the wall) and springs back as it leaves, like a drop of liquid.
float bouncer(vec2 p, vec2 start, vec2 vel, float r, float t, float seed) {
    vec2 bound = gHalf - r * 0.8; // the centre may get this far, letting the drop press slightly into the wall
    vec2 u = start + vel * t / bound + 0.12 * vec2(sin(t * 0.30 + seed), cos(t * 0.27 + seed)); // gently curved path
    vec2 c = vec2(tri(u.x), tri(u.y)) * bound;
    vec2 gap = gHalf - abs(c) - r; // < 0 while the round drop would poke past the edge
    vec2 contact = 1.0 - smoothstep(-0.2 * r, 0.15 * r, gap);
    vec2 rad = r * (1.0 - 0.22 * contact + 0.16 * contact.yx);
    return blob(p, c, rad, t, seed);
}

float field(vec2 p, float t) {
    // large blobs hugging the edges, partly off screen
    float d = blob(p, drift(vec2(-0.78, 0.46), 0.05, t, 0.0), vec2(0.42, 0.24), t, 0.0);
    d = smin(d, blob(p, drift(vec2(-0.92, -0.32), 0.05, t, 1.1), vec2(0.24, 0.36), t, 1.1), 0.12);
    d = smin(d, blob(p, drift(vec2(0.92, 0.40), 0.05, t, 2.2), vec2(0.30, 0.26), t, 2.2), 0.12);
    d = smin(d, blob(p, drift(vec2(1.00, -0.12), 0.06, t, 3.3), vec2(0.18, 0.30), t, 3.3), 0.12);
    d = smin(d, blob(p, drift(vec2(0.25, -0.60), 0.05, t, 4.4), vec2(0.62, 0.15), t, 4.4), 0.12);
    // two mid-size blobs that drift behind the menu card's corners, so its frosted glass always has a crisp
    // pearl edge to blur
    d = smin(d, blob(p, drift(vec2(0.22, -0.14), 0.12, t, 9.9), vec2(0.17, 0.13), t, 9.9), 0.12);
    d = smin(d, blob(p, drift(vec2(-0.20, 0.12), 0.11, t, 11.0), vec2(0.12, 0.10), t, 11.0), 0.12);
    // small droplets that travel the whole screen bouncing off its borders, merging into blobs they pass
    // (stretching a liquid bridge) and pulling apart again
    d = smin(d, bouncer(p, vec2(0.05, 0.75), vec2(0.040, -0.028), 0.070, t, 5.5), 0.11);
    d = smin(d, bouncer(p, vec2(-0.60, 0.10), vec2(-0.034, 0.036), 0.052, t, 6.6), 0.11);
    d = smin(d, bouncer(p, vec2(0.70, -0.45), vec2(0.030, 0.042), 0.052, t, 7.7), 0.11);
    d = smin(d, bouncer(p, vec2(-0.30, -0.70), vec2(-0.046, -0.030), 0.040, t, 8.8), 0.11);
    d = smin(d, bouncer(p, vec2(0.62, 0.25), vec2(-0.038, -0.034), 0.058, t, 12.1), 0.11);
    d = smin(d, bouncer(p, vec2(-0.55, -0.40), vec2(0.036, -0.040), 0.062, t, 13.2), 0.11);
    return d;
}

void main() {
    vec2 res = Params.xy;
    float t = Params.z;
    vec2 fragCoord = texCoord * res;
    float m = min(res.x, res.y);
    vec2 p = (fragCoord - res * 0.5) / m;
    float px = 1.0 / m;
    gHalf = res * 0.5 / m;

    // background: dark violet, lifted softly in the centre behind the menu card
    vec3 bgEdge = vec3(0.075, 0.067, 0.098);
    vec3 bgMid = Params.w > 0.5 ? vec3(0.150, 0.133, 0.180) : bgEdge;
    vec3 col = mix(bgMid, bgEdge, smoothstep(0.0, 0.9, length(p)));

    float d = field(p, t);

    // soft light from the top-left: the field's slope along the light direction, measured over a wide span so
    // every blob gets one broad smooth gradient (lit edge pale, far edge dusky violet)
    vec2 L = normalize(vec2(-0.6, 0.75));
    float s = 0.07;
    float slope = (field(p + L * s, t) - field(p - L * s, t)) / (2.0 * s);
    float diff = clamp(0.5 + 0.55 * slope, 0.0, 1.0);

    vec3 dark = vec3(0.330, 0.285, 0.430);
    vec3 light = vec3(0.820, 0.790, 0.870);
    vec3 fill = mix(dark, light, smoothstep(0.05, 0.95, diff));
    fill *= mix(1.0, 0.82, smoothstep(-0.8, 0.8, p.x - p.y)); // gentle screen-wide diagonal falloff

    // slightly defocused edge (~0.5% of the screen height, about 5px at 1080p): a touch of background blur,
    // still crisp enough for the card's frosted glass to visibly soften it
    float soft = max(px, 0.005);
    float a = smoothstep(soft, -soft, d);
    fragColor = vec4(mix(col, fill, a), 1.0);
}
