package com.smartelderly.service.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartelderly.api.ApiException;
import com.smartelderly.config.AppProperties;
import com.smartelderly.domain.User;

import jakarta.annotation.PostConstruct;

@Service
public class ProfileAvatarStorageService {

    private static final Logger log = LoggerFactory.getLogger(ProfileAvatarStorageService.class);

    private final AppProperties appProperties;
    private Path rootDir;

    public ProfileAvatarStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    void init() {
        String raw = appProperties.getProfile().getStorageDir();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("app.profile.storage-dir 未配置，请在 application.yml 或环境变量 APP_PROFILE_STORAGE 中设置");
        }
        rootDir = Path.of(raw).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建头像目录: " + rootDir, e);
        }
        log.info("Profile avatar storage root: {}", rootDir);
    }

    public String save(User user, MultipartFile file) throws IOException {
        String ext = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String baseName = buildBaseName(user);
        String relative = "avatars/" + baseName + ext;
        Path dest = rootDir.resolve(Path.of(relative)).normalize();
        if (!dest.startsWith(rootDir)) {
            throw new ApiException(4001, "非法存储路径");
        }
        Files.createDirectories(dest.getParent());
        Files.write(dest, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return "/uploads/" + relative.replace('\\', '/');
    }

    private String buildBaseName(User user) {
        String phone = user.getPhone();
        if (phone != null) {
            String digits = phone.replaceAll("\\D+", "");
            if (digits.length() == 11) {
                return "phone_" + digits;
            }
        }
        return "user_" + user.getId();
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String ext = ".jpg";
        if (originalFilename != null && !originalFilename.isBlank()) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String candidate = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
                if (candidate.equals(".jpg") || candidate.equals(".jpeg") || candidate.equals(".png") || candidate.equals(".webp") || candidate.equals(".bmp")) {
                    ext = candidate;
                }
            }
        }
        if (contentType != null) {
            String ct = contentType.trim().toLowerCase(Locale.ROOT);
            if (ct.equals("image/png")) {
                ext = ".png";
            } else if (ct.equals("image/webp")) {
                ext = ".webp";
            } else if (ct.equals("image/bmp")) {
                ext = ".bmp";
            } else if (ct.equals("image/jpeg") || ct.equals("image/jpg")) {
                ext = ".jpg";
            }
        }
        return ext;
    }
}
