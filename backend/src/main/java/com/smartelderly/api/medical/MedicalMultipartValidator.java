package com.smartelderly.api.medical;

import java.util.Locale;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.smartelderly.api.ApiException;

public final class MedicalMultipartValidator {

    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    MediaType.IMAGE_JPEG_VALUE,
                    MediaType.IMAGE_PNG_VALUE,
                    "image/bmp",
                    "image/jpg");

    private MedicalMultipartValidator() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(4001, "请上传图片文件");
        }
        String ct = file.getContentType();
        if (ct == null || ALLOWED_TYPES.stream().noneMatch(t -> t.equalsIgnoreCase(ct.trim()))) {
            throw new ApiException(
                    4001,
                    "仅支持 jpg / jpeg / png / bmp，当前 Content-Type：" + (ct == null ? "未知" : ct));
        }
        String original = file.getOriginalFilename();
        if (original != null && !hasAllowedExtension(original)) {
            throw new ApiException(4001, "文件名后缀应为 .jpg .jpeg .png .bmp");
        }
    }

    private static boolean hasAllowedExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return true;
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("bmp");
    }
}
