#version 330
#extension GL_ARB_separate_shader_objects : require

// AdaptV3 loading screen: the Adapt "A" revealed through liquid glass. A droplet of glass grows on a spring, spreads
// into a rounded tile while the logo glides out of its magnification (the rim bends it into colour), a soft band of
// light crosses the face, then the glass lets go and leaves the bare logo above a thin progress bar.
// Ported from the "Glass Icon Reveal" WebGL shader (zanwei/glass-icon-reveal, MIT) - its dark look, with the logo
// drawn from an analytic signed distance field and the colours moved onto Adapt's Pearl palette. All motion is
// computed on the CPU (LogoReveal.java); this shader only turns those numbers into pixels.

layout(std140) uniform LogoReveal {
    vec4 P[9];
};

#define uRes        P[0].xy
#define uTile       P[0].z
#define uCover      P[0].w
#define uProgress   P[1].x
#define uBarAlpha   P[1].y
#define uPlateScale P[1].z
#define uShapeN     P[1].w
#define uOptics     P[2].x
#define uPlateAlpha P[2].y
#define uLogoHalf   P[2].z
#define uSweep      P[2].w
#define uLightAng   P[3].x
#define uBold       P[3].y
#define uBlur       P[3].z
#define uFrost      P[3].w
#define uDome       P[4].x
#define uDrop       P[4].y
#define uRim        P[4].z
#define uHalo       P[4].w
#define uGhost      P[5].x
#define uRipR       P[5].y
#define uRipA       P[5].z
#define uBare       P[5].w
#define uReach      P[6].x
#define uCornerOn   P[6].y
#define uDissolve   P[6].z
#define uBevelW     P[6].w
// hand-off to the main menu: the A's centre and half height in pixels, and whether it is flying
#define uFly        P[7].xyz
#define uFlyOn      P[7].w
#define uFlyAlpha   P[8].x
#define uFlyWhite   P[8].y

// fixed tuning of the dark look
const float uCornerN = 5.0;
const float uCornerPull = 1.4;
const float uStreak = 1.2;
const float uWiden = 1.2;
const float uSlab = 0.225;
const float uEtaSpread = 0.06;
const float uFocus = 0.4;
const float uFocusK = 1.0;
const float uFocusSplit = 0.0252;
const float uRimLine = 0.09;
const float uGlow = 0.0054;
const float uFaceDisp = 0.0108;
const float uVivid = 0.85;
const float uDisp = 0.9;
const float uDouble = 1.0;
const float uSpec = 1.0;
const float uSheen = 1.0;

// Pearl palette: the menu backdrop's dark violet, a deeper plate under the glass, and a pale pearl logo
const vec3 uBg = vec3(0.075, 0.067, 0.098);
const vec3 uPlateCol = vec3(0.045, 0.040, 0.062);
const vec3 uLogoCol = vec3(0.880, 0.850, 0.930);
const vec3 haloCol = vec3(0.700, 0.660, 0.800);
const vec3 rimCol = vec3(0.860, 0.840, 0.970);

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

float px; // one device pixel in plate units

vec2 rot(vec2 v, float a) { float c = cos(a), s = sin(a); return vec2(c * v.x - s * v.y, s * v.x + c * v.y); }

// ---- the Adapt "A" as a signed distance field (+ outside), in a box spanning [-1, 1] ----

const vec2 A_OUT[8] = vec2[8](
    vec2(-0.2, 1.0), vec2(-0.95, -1.0), vec2(-0.5, -1.0), vec2(-0.32, -0.52),
    vec2(0.32, -0.52), vec2(0.5, -1.0), vec2(0.95, -1.0), vec2(0.2, 1.0));
// the counter, drawn a touch oversize: rounding the outline below grows the letter (and shrinks this) by R
const vec2 A_HOLE[3] = vec2[3](vec2(0.0, 0.45), vec2(-0.29, -0.25), vec2(0.29, -0.25));

float sdOuter(vec2 p) {
    float d = dot(p - A_OUT[0], p - A_OUT[0]);
    float s = 1.0;
    for (int i = 0; i < 8; i++) {
        int j = i == 0 ? 7 : i - 1;
        vec2 e = A_OUT[j] - A_OUT[i];
        vec2 w = p - A_OUT[i];
        vec2 b = w - e * clamp(dot(w, e) / dot(e, e), 0.0, 1.0);
        d = min(d, dot(b, b));
        bvec3 c = bvec3(p.y >= A_OUT[i].y, p.y < A_OUT[j].y, e.x * w.y > e.y * w.x);
        if (all(c) || all(not(c))) s = -s;
    }
    return s * sqrt(d);
}

float sdHole(vec2 p) {
    float d = dot(p - A_HOLE[0], p - A_HOLE[0]);
    float s = 1.0;
    for (int i = 0; i < 3; i++) {
        int j = i == 0 ? 2 : i - 1;
        vec2 e = A_HOLE[j] - A_HOLE[i];
        vec2 w = p - A_HOLE[i];
        vec2 b = w - e * clamp(dot(w, e) / dot(e, e), 0.0, 1.0);
        d = min(d, dot(b, b));
        bvec3 c = bvec3(p.y >= A_HOLE[i].y, p.y < A_HOLE[j].y, e.x * w.y > e.y * w.x);
        if (all(c) || all(not(c))) s = -s;
    }
    return s * sqrt(d);
}

// the A with softly rounded outer corners, rescaled so it still spans [-1, 1]
float sdA(vec2 p) {
    const float R = 0.07;
    vec2 q = p * (1.0 + R);
    return (max(sdOuter(q), -sdHole(q)) - R) / (1.0 + R);
}

// aa: footprint of one screen pixel at q, plate units.
vec4 logoAt(vec2 q, float blur, float aa) {
    float L = uLogoHalf;
    float d = sdA(q / L) * L;
    float w = 0.75 * aa + blur;
    float a = 1.0 - smoothstep(-w, w, d - uBold * L);
    return vec4(uLogoCol, a);
}

// Superellipse field. Returns signed distance (plate units, - inside); g = 1 on the rim.
float shape(vec2 p, float scale, float n, out vec2 nOut, out float g, out vec2 dg) {
    vec2 pp = p / scale;
    vec2 a = max(abs(pp), vec2(1e-6));
    g = pow(pow(a.x, n) + pow(a.y, n), 1.0 / n);
    dg = sign(pp) * pow(a, vec2(n - 1.0)) * pow(max(g, 1e-6), 1.0 - n);
    float dgl = max(length(dg), 1e-6);
    nOut = dg / dgl;
    return (g - 1.0) / dgl * scale;
}

// Curvature radius (plate units) of the shape's contour through p.
float contourRadius(vec2 p, float scale, float n) {
    vec2 a = max(abs(p / scale), vec2(1e-4));
    vec2 a1 = pow(a, vec2(n - 1.0));
    vec2 a2 = pow(a, vec2(n - 2.0));
    float k = (n - 1.0) * (a2.x * a1.y * a1.y + a2.y * a1.x * a1.x) / pow(max(dot(a1, a1), 1e-30), 1.5);
    return scale / max(k, 1e-3);
}

// Where the glass at x = s / bwF across its rounded edge looks: a slab uSlab above the logo with a quarter-round edge
// of radius bwF. A straight-down sight line bends inward by Snell's law, so the outer rim looks deepest.
vec2 refractSample(float x, float bwF, float eta, float R, vec2 dirOut, vec2 p, float dome, float offMul) {
    float u = clamp(1.0 - x, 0.0, 0.995);
    float th = asin(u), thr = asin(u / eta);
    float z = uSlab + bwF * sqrt(1.0 - u * u);
    float off = offMul * z * tan(th - thr);
    off = off / (1.0 + off / (6.0 * R));
    return p - (dirOut * off + p * dome) * uOptics;
}

float bump(float x, float c, float w) { float d = (x - c) / w; return exp(-0.5 * d * d); }

// Visible spectrum, red (0) to violet (1).
vec3 spectrum(float x) {
    return vec3(bump(x, 0.34, 0.2) + 0.08 * bump(x, 0.93, 0.05), bump(x, 0.58, 0.1), bump(x, 0.8, 0.11));
}

float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }

void main() {
    vec2 fragCoord = texCoord * uRes;
    px = 2.0 / uTile;
    vec2 p = (fragCoord - uRes * 0.5) * px;

    bool flying = uFlyOn > 0.5;
    vec4 lg = logoAt(p, uBlur, px);
    // while the A flies to the menu's wordmark it is drawn on its own layer below, not on the backdrop
    vec3 plain = mix(uBg, lg.rgb, flying ? 0.0 : lg.a * uBare);

    vec2 nOut, dg; float g;
    float sd = shape(p, uPlateScale, uShapeN, nOut, g, dg);
    float s = -sd;
    float bw = max(uBevelW, 0.004) * uPlateScale;
    float x = clamp(s / bw, 0.0, 1.0);
    float R = contourRadius(p, uPlateScale, uShapeN);
    vec2 pq = p * p;
    float diag = 4.0 * pq.x * pq.y / max((pq.x + pq.y) * (pq.x + pq.y), 1e-6);
    float dome = uDome * (1.0 - min(g * g, 1.0));
    vec2 dirOut = normalize(mix(nOut, p / max(length(p), 1e-4), 0.2));
    // Rounded corners curve both ways, so they draw content in from deeper than the sides.
    float cornerness = max(smoothstep(0.1, 0.9, 4.0 * nOut.x * nOut.x * nOut.y * nOut.y), smoothstep(0.15, 0.75, diag));
    float offMul = 1.0 + 0.4 * max(uCornerPull - 1.0, 0.0) * cornerness * uCornerOn;
    // Around a corner every sight line meets at one focus a little way in along the diagonal.
    vec2 focus = sign(p) * (pow(0.5, 1.0 / uShapeN) - uFocus * 0.70710678) * uPlateScale;
    float focusW = cornerness * uOptics * uCornerOn;
    float bwF = max(uReach, 0.004) * (1.0 + uWiden * cornerness * uCornerOn);
    float xf = clamp(s / bwF, 0.0, 1.0);
    // Footprint of one pixel after refraction, measured in uniform control flow.
    vec2 qc = refractSample(xf, bwF, 1.5, R, dirOut, p, dome, offMul);
    float foot = max(max(length(dFdx(qc)), length(dFdy(qc))), 0.25 * px);

    // Backdrop: anticipation ghost, ripple and glow.
    vec3 bg = uBg;
    vec3 lift = uBg + vec3(0.06);
    if (uGhost > 0.001) {
        vec2 gn, gdg; float gg;
        float gsd = shape(p, 1.0, uCornerN, gn, gg, gdg);
        bg = mix(bg, lift, uGhost * 0.12 * (1.0 - smoothstep(-0.16, 0.14, gsd)));
    }
    float outside = max(sd, 0.0);
    if (uRipA > 0.001) {
        float d0 = (uRipR - 1.0) * uPlateScale;
        float rx = (sd - d0) / 0.022;
        float ring = exp(-rx * rx);
        bg += uRipA * ring * (0.04 * (0.55 + 0.45 * dot(nOut, normalize(vec2(-0.5, 0.8))))) * 0.5;
    }
    bg = mix(bg, haloCol, clamp(uHalo * (0.30 * exp(-outside / (0.025 * uPlateScale)) + 0.07 * exp(-outside / (0.12 * uPlateScale))), 0.0, 1.0) * step(0.0, sd));

    vec3 col = bg;
    float soft = px + uBlur * 0.8 + 0.06 * uDissolve * uPlateScale;
    if (sd < soft) {
        // Lighting surface: a superellipse quarter, flat toward the face and steep at the rim.
        float y1 = 1.0 - x;
        const float K = 2.6;
        float base1 = max(1.0 - pow(y1, K), 1e-4);
        float db = min(pow(y1, K - 1.0) * pow(base1, 1.0 / K - 1.0), 25.0);
        // A second, lower step just inside the first draws the faint inner line.
        float s1 = bw * 0.62;
        float w2 = bw * 0.22;
        float t2 = clamp((s - s1) / w2, -1.0, 1.0);
        float dh2 = uDouble * bw * 0.16;
        float dd = dh2 * 0.5 * cos(t2 * 1.5707963) * 1.5707963 / w2 * step(abs(t2), 0.999);
        vec2 gDome = -uDome * 2.0 * g * dg * step(g, 1.0);
        vec2 grad = -(db + dd) * nOut + gDome;

        vec3 back = uPlateCol;
        vec3 liftG = back + vec3(0.06);
        float rblur = uBlur + 0.05 * length(qc - p) + min(uGlow * uOptics * (1.0 - xf) * foot / px, 0.04);

        // Each wavelength refracts by its own index, so everything the edge draws in fans into colour while the face
        // stays crisp. Around a corner the samples also smear along the rim.
        float cornerW = smoothstep(0.1, 0.9, 4.0 * nOut.x * nOut.x * nOut.y * nOut.y);
        float streak = 0.35 * uStreak * bw * mix(0.15, 1.0, cornerW) * pow(1.0 - xf, 1.5) * uOptics;
        vec2 tangent = vec2(-nOut.y, nOut.x);
        vec3 acc = vec3(0.0), wsum = vec3(0.0);
        for (int i = 0; i < 20; i++) {
            float lam = (float(i) + 0.5) / 20.0;
            float c = uDisp * (1.0 - 2.0 * lam);
            vec2 along = tangent * (streak * 2.0 * (fract(float(i) * 0.618034 + 0.31) - 0.5));
            float cf = uDisp * (1.0 - 2.0 * lam) * (lam > 0.5 ? 1.0 : 0.5);
            vec2 sp = refractSample(xf, bwF, 1.5 + uEtaSpread * c, R, dirOut, p + along, dome, offMul);
            vec2 focusL = focus - sign(p) * (0.70710678 * uFocusSplit * c * uPlateScale);
            float wFocus = pow(1.0 - xf, uFocusK) * smoothstep(0.0, uRimLine * (1.0 + 0.6 * c), xf);
            sp = mix(sp, mix(p + along, focusL, wFocus), focusW);
            float faceOnly = smoothstep(0.7, 1.0, xf) * uOptics;
            sp *= 1.0 + uFaceDisp * cf * faceOnly;
            vec4 l = logoAt(sp, rblur + 0.12 * streak + uFaceDisp * 0.5 * max(c, 0.0) * faceOnly, foot);
            vec3 w = spectrum(lam);
            acc += mix(back, l.rgb, l.a) * w;
            wsum += w;
        }
        vec3 trans = acc / wsum;
        float tl = dot(trans, vec3(0.2126, 0.7152, 0.0722));
        trans = clamp(tl + (trans - tl) * (1.0 + uVivid * uOptics), 0.0, 1.0);
        // A young droplet is milky; the haze only ever lightens.
        float hazeLum = dot(trans, vec3(0.2126, 0.7152, 0.0722));
        float hazeGate = 1.0 - smoothstep(0.25, 0.75, hazeLum);
        trans = mix(trans, liftG, uFrost * 0.45 * hazeGate);

        float glassAmt = pow(1.0 - uDissolve, 1.5);
        float lit = uSpec * glassAmt;

        // Dark glass: a rim light that grows toward the steep edge, and a small key highlight.
        vec3 Nl = normalize(vec3(-grad, 1.0));
        float edge = 1.0 - Nl.z;
        vec2 ldir = rot(normalize(vec2(-0.62, 0.78)), uLightAng);
        float side = dot(nOut, ldir);
        float fres = pow(edge, 1.5);
        vec3 rim = rimCol * fres * (0.35 + 0.65 * max(side, -side * 0.8)) * uRim * 0.65;
        vec3 Hv = normalize(normalize(vec3(-0.45, 0.6, 0.66)) + vec3(0.0, 0.0, 1.0));
        float spc = pow(max(dot(Nl, Hv), 0.0), 140.0) * 0.8 * smoothstep(0.0, 0.25, y1);
        vec3 glass = trans * (1.0 - 0.25 * fres) + (rim + vec3(spc)) * lit;
        glass = mix(glass, rimCol, uDrop * 0.4 * uSpec * (1.0 - smoothstep(0.0, 0.1 * uPlateScale, s)));

        // While the glass holds the logo a soft band of light crosses the face, brightest near the rim.
        float sweepD = dot(p / uPlateScale, normalize(vec2(0.8, 0.6))) - uSweep;
        float sheen = uSheen * glassAmt * exp(-sweepD * sweepD / 0.03) * (0.55 + 0.45 * (1.0 - x));
        glass = mix(glass, vec3(1.0), sheen * 0.22);

        float cov = clamp(0.5 - sd / soft, 0.0, 1.0);
        col = mix(bg, glass, cov);
    }

    vec3 outc = mix(plain, col, uPlateAlpha);

    // progress bar: a thin rounded pearl line below the logo
    if (uBarAlpha > 0.001) {
        vec2 bc = vec2(0.0, -1.3);
        float halfW = 0.85;
        float halfH = 1.5 * px;
        vec2 bq = p - bc;
        vec2 bd = abs(bq) - vec2(halfW - halfH, 0.0);
        float bar = length(max(bd, 0.0)) + min(max(bd.x, bd.y), 0.0) - halfH;
        float inBar = 1.0 - smoothstep(-px * 0.5, px * 0.5, bar);
        float filled = 1.0 - smoothstep(-px * 0.5, px * 0.5, bq.x - (-halfW + 2.0 * halfW * uProgress));
        vec3 barCol = mix(mix(uBg, uLogoCol, 0.14), mix(uBg, uLogoCol, 0.85), filled);
        outc = mix(outc, barCol, inBar * uBarAlpha);
    }

    outc += (hash(fragCoord) - 0.5) / 255.0;
    if (flying) {
        // the A at its own place and size (pixels), over the fading backdrop: one layer composited over another
        float d = sdA((fragCoord - uFly.xy) / uFly.z) * uFly.z;
        float a = (1.0 - smoothstep(-0.75, 0.75, d)) * uFlyAlpha;
        vec3 lc = mix(uLogoCol, vec3(1.0), uFlyWhite);
        float outA = a + uCover * (1.0 - a);
        fragColor = vec4((lc * a + outc * uCover * (1.0 - a)) / max(outA, 1e-4), outA);
    } else {
        fragColor = vec4(outc, uCover);
    }
}
