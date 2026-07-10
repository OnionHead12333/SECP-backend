package com.smartelderly.api;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartelderly.api.dto.ApiResponse;
import com.smartelderly.api.dto.community.AddElderFriendRequest;
import com.smartelderly.api.dto.community.DirectMessagePageResponse;
import com.smartelderly.api.dto.community.DirectMessageResponse;
import com.smartelderly.api.dto.community.DirectMessageSendRequest;
import com.smartelderly.api.dto.community.ElderFriendCandidateResponse;
import com.smartelderly.api.dto.community.ElderFriendResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.community.ElderSocialService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/elder")
public class ElderSocialController {

    private final ElderSocialService elderSocialService;

    public ElderSocialController(ElderSocialService elderSocialService) {
        this.elderSocialService = elderSocialService;
    }

    @GetMapping("/friends")
    public ApiResponse<java.util.List<ElderFriendResponse>> listFriends() {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.success(elderSocialService.listFriends(principal));
    }

    @GetMapping("/friends/discover")
    public ApiResponse<java.util.List<ElderFriendCandidateResponse>> discoverFriends(
            @RequestParam(value = "phone", required = false) String phone) {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.success(elderSocialService.discoverFriends(principal, phone));
    }

    @PostMapping("/friends")
    public ApiResponse<ElderFriendResponse> addFriend(@Valid @RequestBody AddElderFriendRequest request) {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.success(elderSocialService.addFriend(principal, request));
    }

    @DeleteMapping("/friends/{friendScopeKey}")
    public ApiResponse<Void> deleteFriend(@PathVariable String friendScopeKey) {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        elderSocialService.deleteFriend(principal, friendScopeKey);
        return ApiResponse.success(null);
    }

    @GetMapping("/direct-messages/threads/{peerScopeKey}/messages")
    public ApiResponse<DirectMessagePageResponse> listDirectMessages(
            @PathVariable String peerScopeKey,
            @RequestParam(value = "before", required = false) String before,
            @RequestParam(value = "limit", required = false) Integer limit) {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.success(elderSocialService.listDirectMessages(principal, peerScopeKey, before, limit));
    }

    @PostMapping(value = "/direct-messages/threads/{peerScopeKey}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<DirectMessageResponse> sendDirectMessageText(
            @PathVariable String peerScopeKey,
            @Valid @RequestBody DirectMessageSendRequest request) {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        return ApiResponse.success(elderSocialService.sendTextMessage(principal, peerScopeKey, request));
    }

    @PostMapping(value = "/direct-messages/threads/{peerScopeKey}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DirectMessageResponse> sendDirectMessageMedia(
            @PathVariable String peerScopeKey,
            @RequestParam(value = "kind", required = false, defaultValue = "voice") String kind,
            @RequestParam(value = "durationMs", required = false) Long durationMs,
            @RequestParam("file") MultipartFile file) {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        String normalizedKind = kind == null ? "voice" : kind.trim().toLowerCase();
        
        if ("image".equals(normalizedKind)) {
            return ApiResponse.success(elderSocialService.sendImageMessage(principal, peerScopeKey, file));
        } else if ("voice".equals(normalizedKind)) {
            return ApiResponse.success(elderSocialService.sendVoiceMessage(principal, peerScopeKey, kind, durationMs, file));
        } else {
            throw new com.smartelderly.api.ApiException(4001, "kind 仅支持 voice/image");
        }
    }

    @DeleteMapping("/direct-messages/threads/{peerScopeKey}/messages")
    public ApiResponse<Void> clearDirectMessages(@PathVariable String peerScopeKey) {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        elderSocialService.clearDirectMessagesForViewer(principal, peerScopeKey);
        return ApiResponse.success(null);
    }

    /** 私聊语音下载（与列表返回的 audioUrl 对应；请勿再使用社群 /v1/community-voice）。 */
    @GetMapping("/direct-messages/messages/{messageId}/voice")
    public ResponseEntity<byte[]> downloadDirectVoice(@PathVariable String messageId, HttpServletRequest request)
            throws IOException {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        var payload = elderSocialService.loadVoiceMedia(principal, messageId);
        return buildRangeResponse(payload.bytes(), payload.mediaType(), payload.filename(), request);
    }

    /** 私聊图片下载（与列表返回的 imageUrl 对应；请勿再使用社群 /v1/community-image）。 */
    @GetMapping("/direct-messages/messages/{messageId}/image")
    public ResponseEntity<byte[]> downloadDirectImage(@PathVariable String messageId, HttpServletRequest request)
            throws IOException {
        var principal = SecurityUtils.requireRole(UserRole.elder);
        var payload = elderSocialService.loadImageMedia(principal, messageId);
        return buildRangeResponse(payload.bytes(), payload.mediaType(), payload.filename(), request);
    }

    private static ResponseEntity<byte[]> buildRangeResponse(byte[] all, MediaType mediaType, String filename,
                                                           HttpServletRequest request) {
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
        byte[] part = Arrays.copyOfRange(all, (int) start, (int) (end + 1));
        String contentRange = String.format("bytes %d-%d/%d", start, end, total);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, contentRange)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(len))
                .body(part);
    }
}