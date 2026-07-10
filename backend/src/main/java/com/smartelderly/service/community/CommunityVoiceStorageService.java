package com.smartelderly.service.community;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.config.AppProperties;

import jakarta.annotation.PostConstruct;

@Service
public class CommunityVoiceStorageService {

    private static final Logger log = LoggerFactory.getLogger(CommunityVoiceStorageService.class);

    private final AppProperties appProperties;
    private Path rootDir;

    public CommunityVoiceStorageService(AppProperties appProperties) {
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
            throw new IllegalStateException("无法创建兴趣社群语音目录: " + rootDir, e);
        }
        log.info("Community voice storage root: {}", rootDir);
    }

    public String save(long threadId, String messageId, String originalFilename, byte[] bytes) throws IOException {
        String ext = extensionOf(originalFilename);
        String relative = "community_voice/" + threadId + "/" + messageId + ext;
        Path dest = rootDir.resolve(Path.of(relative)).normalize();
        if (!dest.startsWith(rootDir)) {
            throw new ApiException(4001, "非法存储路径");
        }
        Files.createDirectories(dest.getParent());
        Files.write(dest, bytes, StandardOpenOption.CREATE_NEW);
        return rootDir.relativize(dest).toString().replace('\\', '/');
    }

    public Path resolveStoredFile(String storedRelativePath) {
        Path p = rootDir.resolve(Path.of(storedRelativePath)).normalize();
        if (!p.startsWith(rootDir)) {
            throw new ApiException(4001, "非法文件路径");
        }
        return p;
    }

    public void deleteIfExists(String storedRelativePath) throws IOException {
        if (storedRelativePath == null || storedRelativePath.isBlank()) {
            return;
        }
        Path p = resolveStoredFile(storedRelativePath);
        Files.deleteIfExists(p);
    }

    private static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return ".m4a";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return ".m4a";
        }
        String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
        if (ext.equals(".m4a") || ext.equals(".aac") || ext.equals(".mp4")) {
            return ext;
        }
        return ".m4a";
    }
}