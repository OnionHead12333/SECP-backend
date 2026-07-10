package com.smartelderly.service.medical;

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
public class MedicalDocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(MedicalDocumentStorageService.class);

    private final AppProperties appProperties;
    private Path rootDir;

    public MedicalDocumentStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    void init() {
        String raw = appProperties.getMedical().getStorageDir();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                    "app.medical.storage-dir 未配置，请在 application.yml 或环境变量 APP_MEDICAL_STORAGE 中设置");
        }
        rootDir = Path.of(raw).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建医疗归档目录: " + rootDir, e);
        }
        log.info("Medical document storage root: {}", rootDir);
    }

    /** @return 相对根目录的路径（posix 风格），写入 {@link com.smartelderly.domain.MedicalDocument#setStoredPath} */
    public String save(long elderProfileId, String originalFilename, byte[] bytes) throws IOException {
        String ext = extensionOf(originalFilename);
        String relative =
                elderProfileId + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
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
            return ".jpg";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return ".jpg";
        }
        String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
        if (ext.equals(".jpeg") || ext.equals(".jpg") || ext.equals(".png") || ext.equals(".bmp")) {
            return ext;
        }
        return ".jpg";
    }
}
