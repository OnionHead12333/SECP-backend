package com.smartelderly.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Baidu baidu = new Baidu();
    private final Medical medical = new Medical();
    private final Profile profile = new Profile();
    private final Community community = new Community();
    private final Preprocess preprocess = new Preprocess();

    public Cors getCors() {
        return cors;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Baidu getBaidu() {
        return baidu;
    }

    public Medical getMedical() {
        return medical;
    }

    public Profile getProfile() {
        return profile;
    }

    public Community getCommunity() {
        return community;
    }

    public Preprocess getPreprocess() {
        return preprocess;
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Jwt {
        private String secret = "";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    /** 百度云 AI（与服务端代理 OCR 等能力对齐）。 */
    public static class Baidu {
        private final Ocr ocr = new Ocr();

        public Ocr getOcr() {
            return ocr;
        }
    }

    /** 百度云文字识别：密钥仅存在于服务端。 */
    public static class Ocr {
        private String apiKey = "";
        private String secretKey = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && secretKey != null && !secretKey.isBlank();
        }
    }

    /** 医疗单据归档：本地文件目录（可改为对象存储）。 */
    public static class Medical {
        /** 绝对路径或 {@code ${user.home}} 形式由 Spring 解析 */
        private String storageDir = "";

        public String getStorageDir() {
            return storageDir;
        }

        public void setStorageDir(String storageDir) {
            this.storageDir = storageDir;
        }
    }

    /** 用户头像本地存储目录。 */
    public static class Profile {
        private String storageDir = "";

        public String getStorageDir() {
            return storageDir;
        }

        public void setStorageDir(String storageDir) {
            this.storageDir = storageDir;
        }
    }

    /** 兴趣社群消息媒体：语音/图片本地存储目录。 */
    public static class Community {
        private String storageDir = "";

        public String getStorageDir() {
            return storageDir;
        }

        public void setStorageDir(String storageDir) {
            this.storageDir = storageDir;
        }
    }

    /** 图像预处理配置 */
    public static class Preprocess {
        private boolean enabled = true;
        private int maxWidth = 2000;
        private float jpegQuality = 0.85f;
        private String tempDir = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public void setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        public float getJpegQuality() {
            return jpegQuality;
        }

        public void setJpegQuality(float jpegQuality) {
            this.jpegQuality = jpegQuality;
        }

        public String getTempDir() {
            return tempDir;
        }

        public void setTempDir(String tempDir) {
            this.tempDir = tempDir;
        }
    }
}
