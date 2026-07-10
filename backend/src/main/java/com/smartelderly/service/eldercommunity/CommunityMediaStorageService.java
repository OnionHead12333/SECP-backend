package com.smartelderly.service.eldercommunity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.config.AppProperties;

import jakarta.annotation.PostConstruct;

@Service
public class CommunityMediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(CommunityMediaStorageService.class);

    private final AppProperties appProperties;
    private Path rootDir;

    public CommunityMediaStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    void init() {
        String raw = appProperties.getCommunity().getStorageDir();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                    "app.community.storage-dir 未配置，请在 application.yml 或环境变量 APP_COMMUNITY_STORAGE 中设置");
        }
        rootDir = Path.of(raw).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建兴趣社群媒体目录: " + rootDir, e);
        }
        log.info("Community media storage root: {}", rootDir);
    }

    public String saveVoice(String originalFilename, byte[] bytes) throws IOException {
        String ext = extensionOf(originalFilename, ".m4a", ".aac", ".mp4");
        return save("community_voice", ext, bytes);
    }

    public String saveImage(String originalFilename, byte[] bytes) throws IOException {
        String ext = extensionOf(originalFilename, ".jpg", ".jpeg", ".png", ".webp");
        return save("community_image", ext, bytes);
    }

    public Path resolveStoredFile(String storedRelativePath) {
        String cleaned = storedRelativePath == null ? "" : storedRelativePath.replaceFirst("^/", "");
        Path p = rootDir.resolve(Path.of(cleaned)).normalize();
        if (!p.startsWith(rootDir)) {
            throw new ApiException(4001, "非法文件路径");
        }
        return p;
    }

    /**
     * 尝试解析存储文件路径，优先按传入路径解析（例如 uploads/community_voice/...），
     * 若不存在则尝试兼容老位置（去掉 uploads/ 前缀，如 community_voice/...）。
     */
    public Path resolveStoredFileWithFallback(String storedRelativePath) {
        String cleaned = storedRelativePath == null ? "" : storedRelativePath.replaceFirst("^/", "");
        Path p1 = rootDir.resolve(Path.of(cleaned)).normalize();
        if (p1.startsWith(rootDir) && Files.exists(p1)) {
            return p1;
        }
        String alt = cleaned.replaceFirst("^uploads/", "");
        Path p2 = rootDir.resolve(Path.of(alt)).normalize();
        if (p2.startsWith(rootDir) && Files.exists(p2)) {
            return p2;
        }
        throw new ApiException(404, "媒体文件不存在");
    }

    /** 尽力删除磁盘文件（入库失败回滚时使用）；路径不存在则不抛错。 */
    public void deleteIfExists(String storedRelativePath) {
        if (storedRelativePath == null || storedRelativePath.isBlank()) {
            return;
        }
        String cleaned = storedRelativePath.replaceFirst("^/", "");
        Path p1 = rootDir.resolve(Path.of(cleaned)).normalize();
        try {
            if (p1.startsWith(rootDir) && Files.exists(p1)) {
                Files.deleteIfExists(p1);
                return;
            }
            String alt = cleaned.replaceFirst("^uploads/", "");
            Path p2 = rootDir.resolve(Path.of(alt)).normalize();
            if (p2.startsWith(rootDir) && Files.exists(p2)) {
                Files.deleteIfExists(p2);
            }
        } catch (IOException e) {
            log.warn("deleteIfExists ignored: {}", e.toString());
        }
    }

    private String save(String folder, String ext, byte[] bytes) throws IOException {
        String relative = "uploads/" + folder + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path dest = rootDir.resolve(Path.of(relative)).normalize();
        if (!dest.startsWith(rootDir)) {
            throw new ApiException(4001, "非法存储路径");
        }
        Files.createDirectories(dest.getParent());
        Files.write(dest, bytes, StandardOpenOption.CREATE_NEW);
        return "/" + relative.replace('\\', '/');
    }

    private static String extensionOf(String originalFilename, String... allowed) {
        String fallback = allowed.length == 0 ? ".bin" : allowed[0];
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallback;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return fallback;
        }
        String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
        for (String allow : allowed) {
            if (allow.equalsIgnoreCase(ext)) {
                return ext;
            }
        }
        return fallback;
    }
}
