package com.smartelderly.service.eldercommunity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.eldercommunity.dto.ClearChatResponse;
import com.smartelderly.api.eldercommunity.dto.CommunityMessage;
import com.smartelderly.api.eldercommunity.dto.InterestCommunityBrief;
import com.smartelderly.api.eldercommunity.dto.JoinCommunityRequest;
import com.smartelderly.api.eldercommunity.dto.MessagePage;
import com.smartelderly.api.eldercommunity.dto.MembershipSummary;
import com.smartelderly.api.eldercommunity.dto.MembershipsResponse;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.InterestCommunity;
import com.smartelderly.domain.InterestCommunityChatClear;
import com.smartelderly.domain.InterestCommunityChatClearRepository;
import com.smartelderly.domain.InterestCommunityMessage;
import com.smartelderly.domain.InterestCommunityMessageRepository;
import com.smartelderly.domain.InterestCommunityMembership;
import com.smartelderly.domain.InterestCommunityMembershipRepository;
import com.smartelderly.domain.InterestCommunityRepository;
import com.smartelderly.util.TimeUtils;

@Service
public class InterestCommunityService {

    private static final Logger log = LoggerFactory.getLogger(InterestCommunityService.class);
    private static final Pattern MESSAGE_ID_TIME_PATTERN = Pattern.compile("^msg_(.+)_(\\d{17})_([0-9a-f]{6})$");
    private static final DateTimeFormatter MESSAGE_ID_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final InterestCommunityRepository communityRepository;
    private final InterestCommunityMembershipRepository membershipRepository;
    private final InterestCommunityMessageRepository messageRepository;
    private final InterestCommunityChatClearRepository chatClearRepository;
    private final CommunityMediaStorageService mediaStorageService;
    private final ElderProfileRepository elderProfileRepository;

    public InterestCommunityService(InterestCommunityRepository communityRepository,
                                    InterestCommunityMembershipRepository membershipRepository,
                                    InterestCommunityMessageRepository messageRepository,
                                    InterestCommunityChatClearRepository chatClearRepository,
                                    CommunityMediaStorageService mediaStorageService,
                                    ElderProfileRepository elderProfileRepository) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.messageRepository = messageRepository;
        this.chatClearRepository = chatClearRepository;
        this.mediaStorageService = mediaStorageService;
        this.elderProfileRepository = elderProfileRepository;
    }

    public List<InterestCommunityBrief> listCommunitiesForElder(Long elderProfileId) {
        List<InterestCommunity> all = communityRepository.findByIsActiveTrueOrderBySortOrderAsc();
        List<InterestCommunityMembership> joined = membershipRepository.findByElderProfileIdAndStatus(elderProfileId, "active");
        var joinedSet = joined.stream().map(InterestCommunityMembership::getCommunityId).collect(Collectors.toSet());

        return all.stream().map(c -> {
            InterestCommunityBrief b = new InterestCommunityBrief();
            b.setId(c.getId());
            b.setName(c.getName());
            b.setShortDescription(c.getShortDescription());
            b.setPreviewIcon(c.getPreviewIcon());
            b.setMemberHint(c.getMemberHint());
            b.setJoined(joinedSet.contains(c.getId()));
            return b;
        }).collect(Collectors.toList());
    }

    public MembershipsResponse listMembershipsForElder(Long elderProfileId, String scopeKey) {
        List<InterestCommunityMembership> joined = membershipRepository.findByElderProfileIdAndStatus(elderProfileId, "active");
        MembershipsResponse r = new MembershipsResponse();
        r.setJoinedCommunityIds(joined.stream().map(InterestCommunityMembership::getCommunityId).collect(Collectors.toList()));
        r.setScopeKey(scopeKey);
        return r;
    }

    public String resolveScopeKey(Long elderProfileId, String phone) {
        return buildScopeKey(elderProfileId, phone);
    }

    @Transactional
    public ClearChatResponse clearChatForViewer(Long elderUserId, Long elderProfileId, String viewerScopeKey, String communityId) {
        ensureCommunityActive(communityId);
        ensureJoined(elderProfileId, communityId);

        LocalDateTime now = TimeUtils.now();
        Long clearBeforeMillis = toMillis(now);

        InterestCommunityChatClear clear = chatClearRepository
                .findByViewerScopeKeyAndCommunityId(viewerScopeKey, communityId)
                .orElseGet(InterestCommunityChatClear::new);
        clear.setViewerScopeKey(viewerScopeKey);
        clear.setViewerUserId(elderUserId);
        clear.setElderProfileId(elderProfileId);
        clear.setCommunityId(communityId);
        clear.setClearBeforeMillis(clearBeforeMillis);
        clear.setUpdatedAt(now);
        chatClearRepository.save(clear);

        ClearChatResponse response = new ClearChatResponse();
        response.setCommunityId(communityId);
        response.setViewerScopeKey(viewerScopeKey);
        response.setClearBeforeMillis(clearBeforeMillis);
        return response;
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadVoiceFile(Long elderProfileId, String messageId) throws IOException {
        return loadMediaFile(elderProfileId, messageId, "voice");
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadImageFile(Long elderProfileId, String messageId) throws IOException {
        return loadMediaFile(elderProfileId, messageId, "image");
    }

    @Transactional(readOnly = true)
    public MediaFileInfo resolveMediaFileInfo(Long elderProfileId, String messageId, String kind) throws IOException {
        InterestCommunityMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(404, "消息不存在"));

        String communityId = message.getCommunityId();
        ensureCommunityActive(communityId);
        ensureJoined(elderProfileId, communityId);

        String storedPath;
        String filename;
        MediaType mediaType;
        if ("voice".equals(kind)) {
            if (!"voice".equalsIgnoreCase(message.getMessageKind()) || message.getAudioUrl() == null || message.getAudioUrl().isBlank()) {
                throw new ApiException(404, "消息不存在");
            }
            storedPath = message.getAudioUrl();
            filename = message.getId() + ".m4a";
            mediaType = voiceMediaType(storedPath);
        } else {
            if (!"image".equalsIgnoreCase(message.getMessageKind()) || message.getImageUrl() == null || message.getImageUrl().isBlank()) {
                throw new ApiException(404, "消息不存在");
            }
            storedPath = message.getImageUrl();
            filename = message.getId() + imageExtension(storedPath);
            mediaType = imageMediaType(storedPath);
        }

        var path = mediaStorageService.resolveStoredFileWithFallback(storedPath);
        if (!Files.exists(path)) {
            throw new ApiException(404, "媒体文件不存在");
        }
        long length = Files.size(path);
        return new MediaFileInfo(path, mediaType, filename, length);
    }

    @Transactional(readOnly = true)
    public MessagePage listMessagesForElder(Long elderProfileId, String viewerScopeKey, String communityId, String before, Integer limit) {
        ensureCommunityActive(communityId);
        ensureJoined(elderProfileId, communityId);

        int pageSize = limit == null ? 50 : limit;
        if (pageSize > 200) {
            throw new ApiException(4000, "limit 不能超过 200");
        }
        if (pageSize <= 0) {
            pageSize = 50;
        }

        List<InterestCommunityMessage> all = messageRepository.findByCommunityIdOrderByCreatedAtAscIdAsc(communityId)
            .stream()
            .sorted(Comparator
                .comparingLong(this::resolveMessageTimeMillis)
                .thenComparing(InterestCommunityMessage::getId, Comparator.nullsLast(String::compareTo)))
            .collect(Collectors.toList());
        Optional<InterestCommunityChatClear> clearOpt = chatClearRepository.findByViewerScopeKeyAndCommunityId(viewerScopeKey, communityId);
        Long clearBeforeMillis = clearOpt.map(InterestCommunityChatClear::getClearBeforeMillis).orElse(null);
    InterestCommunityMessage cursor = resolveCursor(before, all);

        List<InterestCommunityMessage> visible = all.stream()
                .filter(m -> {
                    long createdAtMillis = resolveMessageTimeMillis(m);
                    return clearBeforeMillis == null || createdAtMillis > clearBeforeMillis;
                })
        .filter(m -> cursor == null || isBeforeCursor(m, cursor))
                .collect(Collectors.toList());

        boolean hasMore = visible.size() > pageSize;
        List<InterestCommunityMessage> pageItems = hasMore ? visible.subList(Math.max(visible.size() - pageSize, 0), visible.size()) : visible;

        MessagePage page = new MessagePage();
        page.setItems(pageItems.stream().map(m -> toDto(m, viewerScopeKey)).collect(Collectors.toList()));
        page.setHasMore(hasMore);
        page.setNextBefore(hasMore && !pageItems.isEmpty() ? pageItems.get(0).getId() : null);
        return page;
    }

    @Transactional
    public CommunityMessage sendTextMessage(Long elderProfileId, String viewerScopeKey, String communityId, String textContent) {
        ensureCommunityActive(communityId);
        ensureJoined(elderProfileId, communityId);

        if (textContent == null || textContent.isBlank()) {
            throw new ApiException(4000, "textContent不能为空");
        }
        if (textContent.length() > 2000) {
            throw new ApiException(4000, "textContent不能超过2000字");
        }

        InterestCommunityMessage message = new InterestCommunityMessage();
        message.setId(buildMessageId(communityId));
        message.setCommunityId(communityId);
        message.setSenderScopeKey(viewerScopeKey);
        message.setSenderElderProfileId(elderProfileId);
        message.setSenderDisplayName(resolveDisplayName(elderProfileId));
        message.setSenderRole("elder");
        message.setMessageKind("text");
        message.setTextContent(textContent);
        message.setDurationMs(0);
        messageRepository.save(message);
        return toDto(message, viewerScopeKey);
    }

    @Transactional
    public CommunityMessage sendVoiceMessage(Long elderProfileId, String viewerScopeKey, String communityId, MultipartFile file, Integer durationMs) {
        ensureCommunityActive(communityId);
        ensureJoined(elderProfileId, communityId);
        if (file == null || file.isEmpty()) {
            throw new ApiException(4001, "请上传语音文件");
        }
        int duration = durationMs == null ? 0 : durationMs;
        if (durationMs != null && durationMs < 400) {
            throw new ApiException(4000, "录音时间太短");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(5001, "读取上传文件失败：" + e.getMessage());
        }
        String storedPath;
        try {
            storedPath = mediaStorageService.saveVoice(file.getOriginalFilename(), bytes);
        } catch (IOException e) {
            throw new ApiException(5001, "保存语音文件失败：" + e.getMessage());
        }

        InterestCommunityMessage message = new InterestCommunityMessage();
        message.setId(buildMessageId(communityId));
        message.setCommunityId(communityId);
        message.setSenderScopeKey(viewerScopeKey);
        message.setSenderElderProfileId(elderProfileId);
        message.setSenderDisplayName(resolveDisplayName(elderProfileId));
        message.setSenderRole("elder");
        message.setMessageKind("voice");
        message.setAudioUrl(storedPath);
        message.setDurationMs(duration);
        messageRepository.save(message);
        return toDto(message, viewerScopeKey);
    }

    @Transactional
    public CommunityMessage sendImageMessage(Long elderProfileId, String viewerScopeKey, String communityId, MultipartFile file) {
        ensureCommunityActive(communityId);
        ensureJoined(elderProfileId, communityId);
        if (file == null || file.isEmpty()) {
            throw new ApiException(4001, "请上传图片文件");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(5001, "读取上传文件失败：" + e.getMessage());
        }
        String storedPath;
        try {
            storedPath = mediaStorageService.saveImage(file.getOriginalFilename(), bytes);
        } catch (IOException e) {
            throw new ApiException(5001, "保存图片文件失败：" + e.getMessage());
        }

        InterestCommunityMessage message = new InterestCommunityMessage();
        message.setId(buildMessageId(communityId));
        message.setCommunityId(communityId);
        message.setSenderScopeKey(viewerScopeKey);
        message.setSenderElderProfileId(elderProfileId);
        message.setSenderDisplayName(resolveDisplayName(elderProfileId));
        message.setSenderRole("elder");
        message.setMessageKind("image");
        message.setImageUrl(storedPath);
        message.setDurationMs(0);
        messageRepository.save(message);
        return toDto(message, viewerScopeKey);
    }

    @Transactional
    public MembershipSummary joinCommunity(Long elderProfileId, String phone, JoinCommunityRequest request) {
        InterestCommunity community = communityRepository.findById(request.getCommunityId())
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElseThrow(() -> new ApiException(404, "社群不存在"));

        String scopeKey = buildScopeKey(elderProfileId, phone);
        LocalDateTime now = TimeUtils.now();

        InterestCommunityMembership membership = membershipRepository
                .findByElderProfileIdAndCommunityId(elderProfileId, community.getId())
                .orElseGet(InterestCommunityMembership::new);

        if ("active".equalsIgnoreCase(membership.getStatus())) {
            throw new ApiException(409, "已是群成员");
        }

        membership.setElderProfileId(elderProfileId);
        membership.setCommunityId(community.getId());
        membership.setScopeKey(scopeKey);
        membership.setStatus("active");
        membership.setJoinedAt(now);
        membership.setLeftAt(null);
        membershipRepository.save(membership);

        return toSummary(membership);
    }

    @Transactional
    public MembershipSummary leaveCommunity(Long elderProfileId, String communityId) {
        InterestCommunityMembership membership = membershipRepository
                .findByElderProfileIdAndCommunityId(elderProfileId, communityId)
                .filter(m -> "active".equalsIgnoreCase(m.getStatus()))
                .orElseThrow(() -> new ApiException(404, "未加入该社群"));

        LocalDateTime now = TimeUtils.now();
        membership.setStatus("left");
        membership.setLeftAt(now);
        membershipRepository.save(membership);

        return toSummary(membership);
    }

    private String buildScopeKey(Long elderProfileId, String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D+", "");
        if (digits.length() == 11) {
            return "phone_" + digits;
        }
        return "elder_" + elderProfileId;
    }

    private MembershipSummary toSummary(InterestCommunityMembership membership) {
        MembershipSummary summary = new MembershipSummary();
        summary.setCommunityId(membership.getCommunityId());
        communityRepository.findById(membership.getCommunityId()).ifPresent(community -> {
            summary.setCommunityName(community.getName());
            summary.setWelcomeMessage(resolveWelcomeMessage(community));
        });
        summary.setScopeKey(membership.getScopeKey());
        summary.setStatus(membership.getStatus());
        var joinedAtInstant = TimeUtils.toInstant(membership.getJoinedAt());
        var leftAtInstant = TimeUtils.toInstant(membership.getLeftAt());
        summary.setJoinedAtMillis(joinedAtInstant == null ? null : joinedAtInstant.toEpochMilli());
        summary.setLeftAtMillis(leftAtInstant == null ? null : leftAtInstant.toEpochMilli());
        return summary;
    }

    private void ensureCommunityActive(String communityId) {
        communityRepository.findById(communityId)
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElseThrow(() -> new ApiException(404, "社群不存在"));
    }

    private void ensureJoined(Long elderProfileId, String communityId) {
        currentActiveMembership(elderProfileId, communityId);
    }

    private InterestCommunityMembership currentActiveMembership(Long elderProfileId, String communityId) {
        return membershipRepository.findByElderProfileIdAndCommunityId(elderProfileId, communityId)
                .filter(m -> "active".equalsIgnoreCase(m.getStatus()))
                .orElseThrow(() -> new ApiException(403, "请先加入该社群"));
    }

    private boolean isBeforeCursor(InterestCommunityMessage message, InterestCommunityMessage cursor) {
        long cursorAtMillis = resolveMessageTimeMillis(cursor);
        long currentAtMillis = resolveMessageTimeMillis(message);
        if (currentAtMillis < cursorAtMillis) {
            return true;
        }
        if (currentAtMillis > cursorAtMillis) {
            return false;
        }
        return message.getId() != null && cursor.getId() != null && message.getId().compareTo(cursor.getId()) < 0;
    }

    private InterestCommunityMessage resolveCursor(String before, List<InterestCommunityMessage> all) {
        if (before == null || before.isBlank()) {
            return null;
        }
        return all.stream()
                .filter(m -> before.equals(m.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "消息不存在"));
    }

    private CommunityMessage toDto(InterestCommunityMessage message, String viewerScopeKey) {
        CommunityMessage dto = new CommunityMessage();
        dto.setId(message.getId());
        dto.setCommunityId(message.getCommunityId());
        dto.setRole(message.getSenderRole());
        dto.setSenderDisplay(message.getSenderDisplayName());
        dto.setSenderScopeKey(message.getSenderScopeKey());
        dto.setKind(message.getMessageKind());
        dto.setTextContent(message.getTextContent());
        dto.setAudioUrl(message.getAudioUrl());
        dto.setImageUrl(message.getImageUrl());
        dto.setDurationMs(message.getDurationMs());
        LocalDateTime resolvedCreatedAt = resolveMessageCreatedAt(message);
        dto.setCreatedAtMillis(toMillis(resolvedCreatedAt));
        dto.setCreatedAt(resolvedCreatedAt == null ? null : resolvedCreatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setMine(viewerScopeKey != null && viewerScopeKey.equals(message.getSenderScopeKey()));
        return dto;
    }

    private Long toMillis(LocalDateTime dateTime) {
        var instant = TimeUtils.toInstant(dateTime);
        return instant == null ? null : instant.toEpochMilli();
    }

    private long resolveMessageTimeMillis(InterestCommunityMessage message) {
        Long millis = toMillis(resolveMessageCreatedAt(message));
        return millis == null ? 0L : millis;
    }

    private LocalDateTime resolveMessageCreatedAt(InterestCommunityMessage message) {
        if (message == null) {
            return null;
        }
        if (message.getCreatedAt() != null) {
            return message.getCreatedAt();
        }
        String id = message.getId();
        if (id == null || id.isBlank()) {
            return null;
        }
        Matcher matcher = MESSAGE_ID_TIME_PATTERN.matcher(id);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return LocalDateTime.parse(matcher.group(2), MESSAGE_ID_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildMessageId(String communityId) {
        return "msg_" + communityId + "_" + TimeUtils.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + "_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private String resolveDisplayName(Long elderProfileId) {
        return elderProfileRepository.findById(elderProfileId)
                .map(ElderProfile::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("老人" + elderProfileId);
    }

    private String resolveWelcomeMessage(InterestCommunity community) {
        if (community == null) {
            return null;
        }
        if (community.getMemberHint() != null && !community.getMemberHint().isBlank()) {
            return community.getMemberHint();
        }
        return "欢迎加入" + community.getName();
    }

        private DownloadPayload loadMediaFile(Long elderProfileId, String messageId, String kind)
            throws IOException {
        InterestCommunityMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(404, "消息不存在"));

        String communityId = message.getCommunityId();
        ensureCommunityActive(communityId);
        ensureJoined(elderProfileId, communityId);

        String storedPath;
        String filename;
        MediaType mediaType;
        if ("voice".equals(kind)) {
            if (!"voice".equalsIgnoreCase(message.getMessageKind()) || message.getAudioUrl() == null || message.getAudioUrl().isBlank()) {
                throw new ApiException(404, "消息不存在");
            }
            storedPath = message.getAudioUrl();
            filename = message.getId() + ".m4a";
            mediaType = voiceMediaType(storedPath);
        } else {
            if (!"image".equalsIgnoreCase(message.getMessageKind()) || message.getImageUrl() == null || message.getImageUrl().isBlank()) {
                throw new ApiException(404, "消息不存在");
            }
            storedPath = message.getImageUrl();
            filename = message.getId() + imageExtension(storedPath);
            mediaType = imageMediaType(storedPath);
        }

        var path = mediaStorageService.resolveStoredFileWithFallback(storedPath);
        log.debug("Resolved media path for message={} storedPath={} resolved={}", messageId, storedPath, path);
        try {
            byte[] bytes = Files.readAllBytes(path);
            Resource resource = new ByteArrayResource(bytes);
            return new DownloadPayload(resource, mediaType, filename);
        } catch (IOException e) {
            if (e instanceof NoSuchFileException) {
                log.warn("Media file not found on disk: {} (message={})", path, messageId);
                throw new ApiException(404, "媒体文件不存在");
            }
            log.error("Failed reading media file {} for message {}: {}", path, messageId, e.toString());
            throw new ApiException(5001, "读取媒体文件失败：" + e.getMessage());
        }
    }

    public record MediaFileInfo(java.nio.file.Path path, MediaType mediaType, String filename, long length) {}

    private MediaType voiceMediaType(String storedPath) {
        String lower = storedPath == null ? "" : storedPath.toLowerCase();
        if (lower.endsWith(".aac")) {
            return MediaType.parseMediaType("audio/aac");
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".m4a")) {
            return MediaType.parseMediaType("audio/mp4");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private MediaType imageMediaType(String storedPath) {
        String lower = storedPath == null ? "" : storedPath.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.parseMediaType("image/png");
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".bmp")) {
            return MediaType.parseMediaType("image/bmp");
        }
        if (lower.endsWith(".jpeg") || lower.endsWith(".jpg")) {
            return MediaType.parseMediaType("image/jpeg");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String imageExtension(String storedPath) {
        String lower = storedPath == null ? "" : storedPath.toLowerCase();
        if (lower.endsWith(".png")) {
            return ".png";
        }
        if (lower.endsWith(".webp")) {
            return ".webp";
        }
        if (lower.endsWith(".bmp")) {
            return ".bmp";
        }
        if (lower.endsWith(".jpeg")) {
            return ".jpeg";
        }
        return ".jpg";
    }

    public record DownloadPayload(Resource resource, MediaType mediaType, String filename) {}
}
