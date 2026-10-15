import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Generates the mod's pixel-art images:
 * <ul>
 *   <li>icon.png: a top-down map like Xaero's, with the four distance rings around the player's arrow and a zombie
 *       between them. Drawn at 64x64 and scaled to 128x128 so it stays crisp in Mod Menu;</li>
 *   <li>textures/gui/buttons.png: the map button icons (rings on, rings off, settings), white with the same dark
 *       drop shadow as Xaero's own buttons.</li>
 * </ul>
 * Run from the project folder: java tools/MakeAssets.java
 */
public class MakeAssets {
    static final int N = 64;
    static final int[] PX = new int[N * N];
    static final Random RND = new Random(2610);
    static final double CX = 32, CY = 32;

    public static void main(String[] args) throws Exception {
        terrain();
        rings();
        zombie(43, 12);
        playerArrow(CX, CY);
        badgeFrame();
        write(scale(2), "src/main/resources/assets/spawnradius/icon.png");
        write(buttonAtlas(), "src/main/resources/assets/spawnradius/textures/gui/buttons.png");
    }

    // ---------------------------------------------------------------- picture

    /** Grass in 2x2 "block" cells, darker forest patches, and a river with sandy banks. */
    static void terrain() {
        int[] grass = {0xFF5C9A3C, 0xFF66A645, 0xFF538D36, 0xFF6EAE4B, 0xFF5A953A};
        int[] forest = {0xFF3E7230, 0xFF356628, 0xFF467C35, 0xFF2F5C24};
        double[][] woods = {{12, 14, 7}, {52, 50, 8}, {50, 8, 5}, {9, 52, 5}};
        for (int y = 0; y < N; y += 2) {
            for (int x = 0; x < N; x += 2) {
                int c = grass[RND.nextInt(grass.length)];
                for (double[] w : woods) {
                    double d = Math.hypot(x + 1 - w[0], y + 1 - w[1]) + RND.nextDouble() * 2.5;
                    if (d < w[2]) c = forest[RND.nextInt(forest.length)];
                }
                fill(x, y, x + 2, y + 2, c);
            }
        }
        // a river from the top left to the bottom right, bending around the middle
        for (int y = 0; y < N; y++) {
            double centre = 4 + y * 0.42 + 5 * Math.sin(y / 7.0);
            for (int x = 0; x < N; x++) {
                double d = Math.abs(x + 0.5 - centre);
                if (d < 2.6) set(x, y, d < 1.6 ? 0xFF3F68C2 : 0xFF5A86D8);
                else if (d < 3.6) set(x, y, 0xFFD8C98A);
            }
        }
    }

    /** 24, 32, 64 and 128 blocks, squeezed so the small ones stay apart. Same colours as the mod's defaults. */
    static void rings() {
        double[][] rings = {{27, 0xFF5BE06A}, {18, 0xFF4FC3F7}, {11, 0xFFFFD23F}, {6.5, 0xFFFF5050}};
        // beyond the outer ring mobs despawn at once: a little darker there
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                if (Math.hypot(x + 0.5 - CX, y + 0.5 - CY) > 27) set(x, y, 0x38000000);
            }
        }
        for (double[] r : rings) arc(CX, CY, r[0], 3.6, -180, 180, 0xC8101410);
        for (double[] r : rings) arc(CX, CY, r[0], 1.7, -180, 180, (int) (long) r[1]);
        // light on the upper half of each ring
        for (double[] r : rings) arc(CX, CY, r[0], 0.7, -170, -10, mix((int) (long) r[1], 0xFFFFFFFF, 0.35));
    }

    /** A zombie face between the rings, where mobs can spawn: 8x8 like the real skin, with an outline. */
    static void zombie(int x0, int y0) {
        String[] face = {
            "hhhhhhhh",
            "hgggggGh",
            "gggGgggg",
            "gkkggkkg",
            "gggddggg",
            "ggGddGgg",
            "gmmmmmmg",
            "gggggggg",
        };
        fill(x0 + 1, y0 + 1, x0 + 10, y0 + 10, 0x70000000); // shadow
        fill(x0 - 1, y0 - 1, x0 + 9, y0 + 9, 0xFF0B160A);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int c = switch (face[y].charAt(x)) {
                    case 'h' -> 0xFF35602E;
                    case 'G' -> 0xFF4A7B3E;
                    case 'k' -> 0xFF0E0E0E;
                    case 'd' -> 0xFF2F5228;
                    case 'm' -> 0xFF26401F;
                    default -> 0xFF5C9149;
                };
                set(x0 + x, y0 + y, c);
            }
        }
    }

    /** Xaero-style player arrow pointing up: light body, darker right half, dark outline. */
    static void playerArrow(double cx, double cy) {
        boolean[][] in = new boolean[N][N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double u = x + 0.5 - cx, v = y + 0.5 - cy;
                // triangle from the tip (0,-5) to the corners (+-4,+4), with a notch at the back
                boolean triangle = v >= -5 && v <= 4 && Math.abs(u) <= (v + 5) * 4.0 / 9.0;
                boolean notch = v > 4 - (4 - Math.abs(u)) * 0.75 + 0.01 && v <= 4;
                in[y][x] = triangle && !notch;
            }
        }
        for (int y = 1; y < N - 1; y++) {
            for (int x = 1; x < N - 1; x++) {
                if (!in[y][x] && (in[y - 1][x] || in[y + 1][x] || in[y][x - 1] || in[y][x + 1])) set(x, y, 0xFF101010);
            }
        }
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                if (in[y][x]) set(x, y, x + 0.5 < cx ? 0xFFFFFFFF : 0xFFC9CED6);
            }
        }
    }

    // ---------------------------------------------------------------- map buttons

    static final int WHITE = 0xFFFFFFFF;
    static final int SHADOW = 0xFF3F3F3F; // same drop-shadow colour Xaero's own GUI icons use

    /** Midpoint circle outline, integer pixels. */
    static Set<Long> circle(int cx, int cy, int r) {
        Set<Long> pts = new HashSet<>();
        int x = r, y = 0, d = 1 - r;
        while (x >= y) {
            int[][] o = {{x, y}, {y, x}, {-y, x}, {-x, y}, {-x, -y}, {-y, -x}, {y, -x}, {x, -y}};
            for (int[] p : o) pts.add(key(cx + p[0], cy + p[1]));
            y++;
            if (d < 0) {
                d += 2 * y + 1;
            } else {
                x--;
                d += 2 * (y - x) + 1;
            }
        }
        return pts;
    }

    static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    static Set<Long> ringsOn() {
        Set<Long> p = circle(7, 7, 7);
        p.addAll(circle(7, 7, 3));
        for (int[] c : new int[][]{{7, 7}, {6, 7}, {8, 7}, {7, 6}, {7, 8}}) p.add(key(c[0], c[1]));
        return p;
    }

    /** Outer ring only, crossed by a slash ("disabled"). */
    static Set<Long> ringsOff() {
        Set<Long> p = circle(7, 7, 7);
        for (int i = 2; i < 13; i++) p.add(key(i, 14 - i));
        return p;
    }

    static Set<Long> sliders() {
        Set<Long> p = new HashSet<>();
        int[][] rows = {{2, 4}, {7, 10}, {12, 6}};
        for (int[] row : rows) {
            for (int x = 1; x < 14; x++) p.add(key(x, row[0]));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) p.add(key(row[1] + dx, row[0] + dy));
            }
        }
        return p;
    }

    /** 15x15 white glyph + 1px drop shadow, placed in a 16x16 cell of the 64x64 atlas. */
    static void glyph(BufferedImage atlas, int cellX, Set<Long> pixels) {
        for (long k : pixels) {
            int x = (int) (k >> 32), y = (int) k;
            if (x + 1 < 16 && y + 1 < 16 && !pixels.contains(key(x + 1, y + 1))) atlas.setRGB(cellX + x + 1, y + 1, SHADOW);
        }
        for (long k : pixels) atlas.setRGB(cellX + (int) (k >> 32), (int) k, WHITE);
    }

    static BufferedImage buttonAtlas() {
        BufferedImage atlas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        glyph(atlas, 0, ringsOn());
        glyph(atlas, 16, ringsOff());
        glyph(atlas, 32, sliders());
        return atlas;
    }

    // ---------------------------------------------------------------- shared with the other mods' icon tools

    // ---------------------------------------------------------------- badge

    static final int CORNER = 8;

    /** Is the pixel inside the rounded square, shrunk by {@code inset} pixels? */
    static boolean inBadge(int x, int y, double inset) {
        double px = x + 0.5, py = y + 0.5;
        double lo = inset, hi = N - inset, r = Math.max(1, CORNER - inset);
        if (px < lo || py < lo || px > hi || py > hi) return false;
        double cx = Math.max(lo + r, Math.min(hi - r, px));
        double cy = Math.max(lo + r, Math.min(hi - r, py));
        return (px - cx) * (px - cx) + (py - cy) * (py - cy) <= r * r;
    }

    /** 2px dark rim, a lit inner edge at the top left and a shaded one at the bottom right; outside is cleared. */
    static void badgeFrame() {
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int i = y * N + x;
                if (!inBadge(x, y, 0)) {
                    PX[i] = 0;
                } else if (!inBadge(x, y, 2)) {
                    PX[i] = 0xFF0A0B12;
                } else if (!inBadge(x, y, 3)) {
                    boolean lit = x + y < N - 1;
                    PX[i] = blend(PX[i], lit ? 0x40FFFFFF : 0x50000000);
                }
            }
        }
    }

    // ---------------------------------------------------------------- drawing helpers

    static void set(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        PX[y * N + x] = blend(PX[y * N + x], argb);
    }

    static void fill(int x0, int y0, int x1, int y1, int argb) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) set(x, y, argb);
        }
    }

    /** Pixels whose centre is within {@code width/2} of the circle of radius r, between two angles (degrees, 0 = right, y down). */
    static void arc(double cx, double cy, double r, double width, double fromDeg, double toDeg, int argb) {
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (Math.abs(d - r) > width / 2) continue;
                double a = Math.toDegrees(Math.atan2(dy, dx));
                if (a >= fromDeg && a <= toDeg) set(x, y, argb);
            }
        }
    }

    static int blend(int dst, int src) {
        int sa = src >>> 24;
        if (sa == 255) return src;
        if (sa == 0) return dst;
        int da = dst >>> 24;
        double a = sa / 255.0, b = da / 255.0 * (1 - a), out = a + b;
        if (out <= 0) return 0;
        int r = (int) Math.round((((src >> 16) & 255) * a + ((dst >> 16) & 255) * b) / out);
        int g = (int) Math.round((((src >> 8) & 255) * a + ((dst >> 8) & 255) * b) / out);
        int bl = (int) Math.round(((src & 255) * a + (dst & 255) * b) / out);
        return ((int) Math.round(out * 255) << 24) | (r << 16) | (g << 8) | bl;
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    static BufferedImage scale(int factor) {
        BufferedImage img = new BufferedImage(N * factor, N * factor, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N * factor; y++) {
            for (int x = 0; x < N * factor; x++) img.setRGB(x, y, PX[(y / factor) * N + x / factor]);
        }
        return img;
    }

    static void write(BufferedImage img, String path) throws Exception {
        File out = new File(path);
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("Wrote " + out);
    }

}
