package net.ekical.sotuff.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class UserMedia {
    private static final String NAMESPACE = "so-tuff";
    private static final String PACK_FOLDER_NAME = "SoTuff-UserMedia";

    private static final List<Identifier> USER_SKULLS = new ArrayList<>();
    private static final Map<Identifier, NativeImageBackedTexture> USER_TEX = new HashMap<>();

    private static Path currentRoot = null;

    private UserMedia() {}

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static synchronized void setRootAndRefresh(String rootDir) {
        if (rootDir == null || rootDir.isBlank()) {
            unloadAll();
            currentRoot = null;
            return;
        }
        currentRoot = Paths.get(rootDir).toAbsolutePath().normalize();
        refresh();
    }

    public static synchronized void refresh() {
        unloadAll();
        if (currentRoot == null) return;

        try {
            Path imagesDir = currentRoot.resolve("images");
            List<Path> imageFiles = Files.isDirectory(imagesDir)
                    ? Files.walk(imagesDir)
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
                    })
                    .sorted()
                    .collect(Collectors.toList())
                    : List.of();

            Path soundsDir  = currentRoot.resolve("sounds");
            Path soundsJson = currentRoot.resolve("sounds.json");
            Path rpRoot     = getUserPackRoot();

            buildUserPack(rpRoot, soundsDir, soundsJson, imageFiles);
            System.out.println("[SoTuff] UserMedia: pack updated at " + rpRoot);

            mc().execute(() -> {
                System.out.println("[SoTuff] UserMedia: scheduling resource reload");
                mc().reloadResources().thenRun(() -> {
                    System.out.println("[SoTuff] UserMedia: reload completed -> rescan skull textures");
                    mc().execute(OverlayRenderer::reloadUserImages);
                });
            });
        } catch (Exception e) {
            System.out.println("[SoTuff] UserMedia.refresh error: " + e);
        }
    }

    public static synchronized List<Identifier> getUserSkulls() {
        return Collections.unmodifiableList(USER_SKULLS);
    }

    public static synchronized void unloadAll() {
        List<NativeImageBackedTexture> toClose = new ArrayList<>(USER_TEX.values());
        USER_TEX.clear();
        USER_SKULLS.clear();

        mc().execute(() -> {
            for (var tex : toClose) {
                try { tex.close(); } catch (Throwable ignored) {}
            }
        });
    }

    private static Path getUserPackRoot() throws Exception {
        Path rp = mc().runDirectory.toPath()
                .resolve("resourcepacks")
                .resolve(PACK_FOLDER_NAME);
        Files.createDirectories(rp);
        return rp;
    }

    private static void buildUserPack(Path rpRoot, Path soundsDir, Path soundsJson, List<Path> imageFiles) throws Exception {
        deleteDirectory(rpRoot);
        Files.createDirectories(rpRoot);

        String mcmeta = """
                {
                  "pack": {
                    "pack_format": 48,
                    "description": "Your custom Sounds/Images!"
                  }
                }
                """;
        Files.writeString(rpRoot.resolve("pack.mcmeta"), mcmeta);

        Path assetsNs = rpRoot.resolve("assets").resolve(NAMESPACE);
        Files.createDirectories(assetsNs.resolve("sounds"));
        Files.createDirectories(assetsNs.resolve("textures/skulls"));

        if (Files.isDirectory(soundsDir) && Files.isReadable(soundsJson)) {
            Files.copy(soundsJson, assetsNs.resolve("sounds.json"), StandardCopyOption.REPLACE_EXISTING);
            try (var stream = Files.walk(soundsDir)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    try {
                        Path rel = soundsDir.relativize(p);
                        Path dst = assetsNs.resolve("sounds").resolve(rel);
                        Files.createDirectories(dst.getParent());
                        Files.copy(p, dst, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ioe) {
                        System.out.println("[SoTuff] UserMedia: copy sound failed " + p + " -> " + ioe);
                    }
                });
            }
        } else {
            System.out.println("[SoTuff] UserMedia: sounds missing; skipping sound copy");
        }

        Path skullsDir = assetsNs.resolve("textures/skulls");
        Files.createDirectories(skullsDir);
        
        for (Path imgPath : imageFiles) {
            try (InputStream in = Files.newInputStream(imgPath)) {
                NativeImage img = NativeImage.read(in);
                NativeImage resized = resizeTo512(img);
                String fileName = imgPath.getFileName().toString();
                int dotIndex = fileName.lastIndexOf('.');
                String base = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
                Path out = skullsDir.resolve(base + ".png");
                resized.writeTo(out);
                resized.close();
                img.close();
            } catch (Throwable t) {
                System.out.println("[SoTuff] UserMedia: failed to copy/resize image " + imgPath + " -> " + t);
            }
        }
    }

    private static NativeImage resizeTo512(NativeImage src) {
        int W = 512, H = 512;
        NativeImage dst = new NativeImage(W, H, false);
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        float sx = (float) srcW / W;
        float sy = (float) srcH / H;
        int maxSrcX = srcW - 1;
        int maxSrcY = srcH - 1;
        
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int srcX = Math.min((int) (x * sx), maxSrcX);
                int srcY = Math.min((int) (y * sy), maxSrcY);
                dst.setColorArgb(x, y, src.getColorArgb(srcX, srcY));
            }
        }
        return dst;
    }

    private static void deleteDirectory(Path dir) throws Exception {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> {
                          try { Files.delete(p); } catch (Exception ignored) {}
                      });
            }
        }
    }
}
