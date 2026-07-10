package com.smartelderly.api.eldercommunity;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpRange;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiException;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.eldercommunity.InterestCommunityService;
import com.smartelderly.service.community.ElderSocialService;

@RestController
public class CommunityMediaController {

    private final InterestCommunityService interestCommunityService;
    private final ElderProfileRepository elderProfileRepository;
    private final ElderSocialService elderSocialService;

    public CommunityMediaController(InterestCommunityService interestCommunityService,
                                    ElderProfileRepository elderProfileRepository,
                                    ElderSocialService elderSocialService) {
        this.interestCommunityService = interestCommunityService;
        this.elderProfileRepository = elderProfileRepository;
        this.elderSocialService = elderSocialService;
    }

    @GetMapping("/v1/community-image/{messageId}/file")
    public ResponseEntity<byte[]> downloadCommunityImage(
            @org.springframework.web.bind.annotation.PathVariable("messageId") String messageId,
            HttpServletRequest request) throws IOException {
        var user = SecurityUtils.requireRole(UserRole.elder);
        // 兼容：历史上私聊误走社群图片 URL（/v1/community-image/direct_xxx/file）
        if (messageId != null && messageId.startsWith("direct_")) {
            var payload = elderSocialService.loadImageMedia(user, messageId);
            return buildRangeResponse(payload.bytes(), payload.mediaType(), payload.filename(), request);
        }

        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        var info = interestCommunityService.resolveMediaFileInfo(elder.getId(), messageId, "image");
        return buildRangeResponse(info.path(), info.mediaType(), info.filename(), request);
    }

    private ResponseEntity<byte[]> buildRangeResponse(byte[] all, MediaType mediaType, String filename,
                                                      HttpServletRequest request) throws IOException {
        long total = all.length;
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(all);
        }
        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        if (ranges == null || ranges.isEmpty()) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
        HttpRange r = ranges.get(0);
        long start = r.getRangeStart(total);
        long end = r.getRangeEnd(total);
        if (start >= total) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
        int len = (int) (end - start + 1);
        byte[] part = java.util.Arrays.copyOfRange(all, (int) start, (int) (end + 1));
        String contentRange = String.format("bytes %d-%d/%d", start, end, total);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, contentRange)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(len))
                .body(part);
    }

    private ResponseEntity<byte[]> buildRangeResponse(java.nio.file.Path path, MediaType mediaType, String filename,
                                                      HttpServletRequest request) throws IOException {
        byte[] all = Files.readAllBytes(path);
        return buildRangeResponse(all, mediaType, filename, request);
    }
}
