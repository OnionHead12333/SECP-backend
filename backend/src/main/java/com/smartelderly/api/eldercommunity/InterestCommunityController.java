package com.smartelderly.api.eldercommunity;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.eldercommunity.dto.ClearChatResponse;
import com.smartelderly.api.eldercommunity.dto.CommunityMessage;
import com.smartelderly.api.eldercommunity.dto.InterestCommunityBrief;
import com.smartelderly.api.eldercommunity.dto.JoinCommunityRequest;
import com.smartelderly.api.eldercommunity.dto.MessagePage;
import com.smartelderly.api.eldercommunity.dto.MembershipSummary;
import com.smartelderly.api.eldercommunity.dto.MembershipsResponse;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;

import com.smartelderly.service.eldercommunity.InterestCommunityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/elder/interest-communities")
@Validated
public class InterestCommunityController {

    private final InterestCommunityService interestCommunityService;
    private final ElderProfileRepository elderProfileRepository;

    public InterestCommunityController(InterestCommunityService interestCommunityService,
                                       ElderProfileRepository elderProfileRepository) {
        this.interestCommunityService = interestCommunityService;
        this.elderProfileRepository = elderProfileRepository;
    }

    @GetMapping
    public ApiResponse<List<InterestCommunityBrief>> listInterestCommunities() {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        var list = interestCommunityService.listCommunitiesForElder(elder.getId());
        return ApiResponse.ok(list);
    }

    @GetMapping("/memberships")
    public ApiResponse<MembershipsResponse> listMemberships() {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        String scopeKey = interestCommunityService.resolveScopeKey(elder.getId(), elder.getPhone());
        var resp = interestCommunityService.listMembershipsForElder(elder.getId(), scopeKey);
        return ApiResponse.ok(resp);
    }

    @GetMapping("/{communityId}/messages")
    public ApiResponse<MessagePage> listCommunityMessages(
            @PathVariable("communityId") String communityId,
            @RequestParam(value = "before", required = false) String before,
            @RequestParam(value = "limit", required = false) Integer limit) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        String scopeKey = interestCommunityService.resolveScopeKey(elder.getId(), elder.getPhone());
        MessagePage page = interestCommunityService.listMessagesForElder(elder.getId(), scopeKey, communityId, before, limit);
        return ApiResponse.ok(page);
    }

    @PostMapping("/memberships")
    public ApiResponse<MembershipSummary> joinCommunity(@Valid @RequestBody JoinCommunityRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        MembershipSummary summary = interestCommunityService.joinCommunity(elder.getId(), elder.getPhone(), request);
        return ApiResponse.ok("joined", summary);
    }

    @DeleteMapping("/memberships/{communityId}")
    public ApiResponse<MembershipSummary> leaveCommunity(@PathVariable("communityId") String communityId) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        MembershipSummary summary = interestCommunityService.leaveCommunity(elder.getId(), communityId);
        return ApiResponse.ok("left", summary);
    }

        @DeleteMapping("/{communityId}/messages")
        public ApiResponse<ClearChatResponse> clearCommunityMessagesForViewer(@PathVariable("communityId") String communityId) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
            .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        String scopeKey = interestCommunityService.resolveScopeKey(elder.getId(), elder.getPhone());
        ClearChatResponse response = interestCommunityService.clearChatForViewer(user.userId(), elder.getId(), scopeKey, communityId);
        return ApiResponse.ok(response);
        }

    @PostMapping(value = "/{communityId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CommunityMessage> sendCommunityTextMessage(
            @PathVariable("communityId") String communityId,
            @Valid @RequestBody com.smartelderly.api.eldercommunity.dto.SendTextMessageRequest request) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        String scopeKey = interestCommunityService.resolveScopeKey(elder.getId(), elder.getPhone());
        CommunityMessage message = interestCommunityService.sendTextMessage(elder.getId(), scopeKey, communityId, request.getTextContent());
        return ApiResponse.ok("sent", message);
    }

    @PostMapping(value = "/{communityId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CommunityMessage> sendCommunityMediaMessage(
            @PathVariable("communityId") String communityId,
            @RequestParam("kind") String kind,
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> params) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile elder = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(4030, "no elder profile for current user"));

        String scopeKey = interestCommunityService.resolveScopeKey(elder.getId(), elder.getPhone());
        CommunityMessage message;
        Integer durationMs = resolveVoiceDurationMs(params);
        if ("voice".equalsIgnoreCase(kind)) {
            message = interestCommunityService.sendVoiceMessage(elder.getId(), scopeKey, communityId, file, durationMs);
        } else if ("image".equalsIgnoreCase(kind)) {
            message = interestCommunityService.sendImageMessage(elder.getId(), scopeKey, communityId, file);
        } else {
            throw new ApiException(4000, "kind 仅支持 text/voice/image");
        }
        return ApiResponse.ok("sent", message);
    }

    private Integer resolveVoiceDurationMs(Map<String, String> params) {
        Integer durationMs = readInteger(params, "durationMs", "duration_ms", "durationMillis", "duration_millis");
        if (durationMs != null) {
            return durationMs;
        }

        Integer durationSeconds = readInteger(params, "durationSeconds", "duration_seconds", "duration");
        if (durationSeconds != null) {
            return durationSeconds * 1000;
        }

        return null;
    }

    private Integer readInteger(Map<String, String> params, String... keys) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            String value = params.get(key);
            if (value != null && !value.isBlank()) {
                try {
                    return Integer.valueOf(value.trim());
                } catch (NumberFormatException ignored) {
                    throw new ApiException(4000, key + " 必须是数字");
                }
            }
        }
        return null;
    }
}
