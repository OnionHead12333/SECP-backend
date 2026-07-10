package com.smartelderly.service.community;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.community.AddElderFriendRequest;
import com.smartelderly.api.dto.community.DirectMessagePageResponse;
import com.smartelderly.api.dto.community.DirectMessageResponse;
import com.smartelderly.api.dto.community.DirectMessageSendRequest;
import com.smartelderly.api.dto.community.ElderFriendCandidateResponse;
import com.smartelderly.api.dto.community.ElderFriendResponse;
import com.smartelderly.api.medical.MedicalMultipartValidator;
import com.smartelderly.service.eldercommunity.CommunityMediaStorageService;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.User;
import com.smartelderly.domain.UserRepository;
import com.smartelderly.domain.UserRole;
import com.smartelderly.domain.community.CommunityDemoPeerProfile;
import com.smartelderly.domain.community.CommunityDemoPeerProfileRepository;
import com.smartelderly.domain.community.DirectMessage;
import com.smartelderly.domain.community.DirectMessageClear;
import com.smartelderly.domain.community.DirectMessageClearRepository;
import com.smartelderly.domain.community.DirectMessageRepository;
import com.smartelderly.domain.community.DirectMessageThread;
import com.smartelderly.domain.community.DirectMessageThreadRepository;
import com.smartelderly.domain.community.ElderFriend;
import com.smartelderly.domain.community.ElderFriendRepository;
import com.smartelderly.security.AuthPrincipal;

@Service
public class ElderSocialService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int MAX_PAGE_SIZE = 200;
    private static final String PHONE_PREFIX = "phone_";
    private static final String ELDER_PREFIX = "elder_";
    private static final String TEXT_KIND = "text";
    private static final String VOICE_KIND = "voice";
    private static final String IMAGE_KIND = "image";
    private static final String REGISTERED_ELDER_HINT = "已注册老人";
    private static final String PHONE_ADD_HINT = "通过手机号添加";
    private static final String UNKNOWN_EMOJI = "👤";

    private final UserRepository userRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final CommunityDemoPeerProfileRepository demoPeerProfileRepository;
    private final ElderFriendRepository elderFriendRepository;
    private final DirectMessageThreadRepository directMessageThreadRepository;
    private final DirectMessageRepository directMessageRepository;
    private final DirectMessageClearRepository directMessageClearRepository;
    private final CommunityMediaStorageService mediaStorageService;

    public ElderSocialService(
            UserRepository userRepository,
            ElderProfileRepository elderProfileRepository,
            CommunityDemoPeerProfileRepository demoPeerProfileRepository,
            ElderFriendRepository elderFriendRepository,
            DirectMessageThreadRepository directMessageThreadRepository,
            DirectMessageRepository directMessageRepository,
            DirectMessageClearRepository directMessageClearRepository,
            CommunityMediaStorageService mediaStorageService) {
        this.userRepository = userRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.demoPeerProfileRepository = demoPeerProfileRepository;
        this.elderFriendRepository = elderFriendRepository;
        this.directMessageThreadRepository = directMessageThreadRepository;
        this.directMessageRepository = directMessageRepository;
        this.directMessageClearRepository = directMessageClearRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional(readOnly = true)
    public List<ElderFriendResponse> listFriends(AuthPrincipal principal) {
        ElderContext context = resolveCurrentElderContext(principal);
        return elderFriendRepository.findByOwnerElderProfileIdOrderByAddedAtDesc(context.elderProfile().getId())
                .stream()
                .map(this::toFriendResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ElderFriendCandidateResponse> discoverFriends(AuthPrincipal principal, String phone) {
        ElderContext context = resolveCurrentElderContext(principal);
        Map<String, ElderFriendCandidateResponse> candidates = new LinkedHashMap<>();
        String normalizedPhone = normalizePhone(phone);

        if (normalizedPhone != null) {
            PeerCandidate resolved = resolvePeerByPhone(normalizedPhone);
            addCandidateIfVisible(candidates, context, resolved);
        } else {
            for (CommunityDemoPeerProfile demo : demoPeerProfileRepository.findAllByOrderByCreatedAtAsc()) {
                addCandidateIfVisible(candidates, context, toPeerCandidate(demo));
            }
            List<User> elderUsers = userRepository.findAll().stream()
                    .filter(user -> UserRole.elder.name().equalsIgnoreCase(user.getRole()))
                    .filter(user -> user.getPhone() != null && !user.getPhone().isBlank())
                    .sorted(Comparator.comparing(User::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();
            for (User elderUser : elderUsers) {
                addCandidateIfVisible(candidates, context, toRegisteredElderCandidate(elderUser));
            }
        }

        return new ArrayList<>(candidates.values());
    }

    @Transactional
    public ElderFriendResponse addFriend(AuthPrincipal principal, AddElderFriendRequest request) {
        ElderContext context = resolveCurrentElderContext(principal);
        PeerCandidate peer = resolvePeerCandidate(request.phone(), request.scopeKey());
        if (peer.scopeKey().equals(context.scopeKey())) {
            throw new ApiException(409, "不能添加自己为好友");
        }
        if (elderFriendRepository.existsByOwnerElderProfileIdAndFriendScopeKey(context.elderProfile().getId(), peer.scopeKey())
                || elderFriendRepository.existsByOwnerElderProfileIdAndPhone(context.elderProfile().getId(), peer.phone())) {
            throw new ApiException(409, "该好友已存在");
        }

        ElderFriend friend = new ElderFriend();
        friend.setOwnerElderProfileId(context.elderProfile().getId());
        friend.setOwnerScopeKey(context.scopeKey());
        friend.setFriendScopeKey(peer.scopeKey());
        friend.setFriendElderProfileId(peer.elderProfileId());
        friend.setDisplayName(peer.displayName());
        friend.setPhone(peer.phone());
        friend.setHint(peer.hint());
        friend.setEmoji(peer.emoji());
        friend.setAddedAt(LocalDateTime.now());
        friend = elderFriendRepository.save(friend);
        ensureReciprocalFriend(context, peer);
        return toFriendResponse(friend);
    }

    @Transactional
    public void deleteFriend(AuthPrincipal principal, String friendScopeKey) {
        ElderContext context = resolveCurrentElderContext(principal);
        ElderFriend friend = elderFriendRepository
                .findByOwnerElderProfileIdAndFriendScopeKey(context.elderProfile().getId(), friendScopeKey)
                .orElseThrow(() -> new ApiException(404, "好友不存在"));
        Long peerElderProfileId = friend.getFriendElderProfileId();
        elderFriendRepository.delete(friend);
        removeReciprocalFriend(peerElderProfileId, context.scopeKey());
    }

    @Transactional
    public DirectMessagePageResponse listDirectMessages(AuthPrincipal principal, String peerScopeKey, String before,
            Integer limit) {
        ElderContext context = resolveCurrentElderContext(principal);
        PeerCandidate peer = resolvePeerForChat(context, peerScopeKey);
        DirectMessageThread thread = getOrCreateThread(context, peer);
        List<DirectMessage> allMessages = directMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
        int pageSize = normalizePageSize(limit);

        // 过滤清空时间戳之后的消息
        var clearOpt = directMessageClearRepository.findByThreadIdAndScopeKey(thread.getId(), context.scopeKey());
        Long clearBeforeMillis = clearOpt.map(DirectMessageClear::getClearBeforeMillis).orElse(null);
        
        List<DirectMessage> visibleMessages = allMessages.stream()
                .filter(m -> {
                    long createdAtMillis = m.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
                    return clearBeforeMillis == null || createdAtMillis > clearBeforeMillis;
                })
                .toList();

        MessageSlice slice = sliceMessages(visibleMessages, before, pageSize);
        return new DirectMessagePageResponse(
                slice.items().stream().map(message -> toMessageResponse(message, context.scopeKey(), thread.getId()))
                        .toList(),
                slice.hasMore(),
                thread.getId(),
                peer.scopeKey());
    }

    @Transactional
    public DirectMessageResponse sendTextMessage(AuthPrincipal principal, String peerScopeKey,
            DirectMessageSendRequest request) {
        ElderContext context = resolveCurrentElderContext(principal);
        PeerCandidate peer = resolvePeerForChat(context, peerScopeKey);
        DirectMessageThread thread = getOrCreateThread(context, peer);
        String kind = normalizeKind(request.kind());
        if (!TEXT_KIND.equals(kind)) {
            throw new ApiException(4001, "kind 必须为 text");
        }
        String text = request.textContent() == null ? "" : request.textContent().trim();
        if (text.isEmpty()) {
            throw new ApiException(4001, "textContent 不能为空");
        }

        DirectMessage message = new DirectMessage();
        message.setId(buildMessageId("direct"));
        message.setThreadId(thread.getId());
        message.setSenderScopeKey(context.scopeKey());
        message.setSenderElderProfileId(context.elderProfile().getId());
        message.setSenderDisplayName(resolveSenderDisplayName(context.user(), context.elderProfile()));
        message.setSenderRole(UserRole.elder.name());
        message.setMessageKind(TEXT_KIND);
        message.setTextContent(text);
        message.setDurationMs(0);
        message.setCreatedAt(LocalDateTime.now());
        message = directMessageRepository.save(message);
        touchThread(thread);
        return toMessageResponse(message, context.scopeKey(), thread.getId());
    }

    @Transactional
    public DirectMessageResponse sendVoiceMessage(AuthPrincipal principal, String peerScopeKey, String kind,
            Long durationMs, MultipartFile file) {
        ElderContext context = resolveCurrentElderContext(principal);
        PeerCandidate peer = resolvePeerForChat(context, peerScopeKey);
        DirectMessageThread thread = getOrCreateThread(context, peer);
        if (!VOICE_KIND.equals(normalizeKind(kind))) {
            throw new ApiException(4001, "kind 必须为 voice");
        }
        validateVoice(file, durationMs);

        String messageId = buildMessageId("direct");
        String storedPath;
        try {
            // 与兴趣社群保持一致，统一通过 CommunityMediaStorageService 存储语音
            storedPath = mediaStorageService.saveVoice(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new ApiException(5001, "保存语音文件失败：" + e.getMessage());
        }

        DirectMessage message = new DirectMessage();
        message.setId(messageId);
        message.setThreadId(thread.getId());
        message.setSenderScopeKey(context.scopeKey());
        message.setSenderElderProfileId(context.elderProfile().getId());
        message.setSenderDisplayName(resolveSenderDisplayName(context.user(), context.elderProfile()));
        message.setSenderRole(UserRole.elder.name());
        message.setMessageKind(VOICE_KIND);
        message.setAudioUrl(storedPath);
        message.setDurationMs(durationMs == null ? 0 : durationMs.intValue());
        message.setCreatedAt(LocalDateTime.now());

        try {
            message = directMessageRepository.save(message);
            touchThread(thread);
        } catch (RuntimeException ex) {
            mediaStorageService.deleteIfExists(storedPath);
            throw ex;
        }
        return toMessageResponse(message, context.scopeKey(), thread.getId());
    }

    @Transactional
    public DirectMessageResponse sendImageMessage(AuthPrincipal principal, String peerScopeKey,
            MultipartFile file) {
        ElderContext context = resolveCurrentElderContext(principal);
        PeerCandidate peer = resolvePeerForChat(context, peerScopeKey);
        DirectMessageThread thread = getOrCreateThread(context, peer);
        validateImage(file);

        String messageId = buildMessageId("direct");
        String storedPath;
        try {
            storedPath = mediaStorageService.saveImage(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new ApiException(5001, "保存图片文件失败：" + e.getMessage());
        }

        DirectMessage message = new DirectMessage();
        message.setId(messageId);
        message.setThreadId(thread.getId());
        message.setSenderScopeKey(context.scopeKey());
        message.setSenderElderProfileId(context.elderProfile().getId());
        message.setSenderDisplayName(resolveSenderDisplayName(context.user(), context.elderProfile()));
        message.setSenderRole(UserRole.elder.name());
        message.setMessageKind(IMAGE_KIND);
        message.setImageUrl(storedPath);
        message.setDurationMs(0);
        message.setCreatedAt(LocalDateTime.now());
        message = directMessageRepository.save(message);
        touchThread(thread);
        return toMessageResponse(message, context.scopeKey(), thread.getId());
    }

    @Transactional
    public void clearDirectMessagesForViewer(AuthPrincipal principal, String peerScopeKey) {
        ElderContext context = resolveCurrentElderContext(principal);
        PeerCandidate peer = resolvePeerForChat(context, peerScopeKey);
        DirectMessageThread thread = getOrCreateThread(context, peer);

        LocalDateTime now = LocalDateTime.now();
        long clearBeforeMillis = now.atZone(SHANGHAI).toInstant().toEpochMilli();

        DirectMessageClear clear = directMessageClearRepository
                .findByThreadIdAndScopeKey(thread.getId(), context.scopeKey())
                .orElseGet(DirectMessageClear::new);
        clear.setThreadId(thread.getId());
        clear.setScopeKey(context.scopeKey());
        clear.setElderProfileId(context.elderProfile().getId());
        clear.setClearBeforeMillis(clearBeforeMillis);
        clear.setUpdatedAt(now);
        directMessageClearRepository.save(clear);
    }

    @Transactional(readOnly = true)
    public DirectMessageMediaPayload loadVoiceMedia(AuthPrincipal principal, String messageId) throws IOException {
        ElderContext context = resolveCurrentElderContext(principal);
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(404, "语音消息不存在"));
        DirectMessageThread thread = directMessageThreadRepository.findById(message.getThreadId())
                .orElseThrow(() -> new ApiException(404, "语音消息不存在"));
        if (!belongsToThread(context.scopeKey(), thread)) {
            throw new ApiException(4030, "无权访问该语音文件");
        }
        if (!VOICE_KIND.equalsIgnoreCase(message.getMessageKind())) {
            throw new ApiException(4001, "该消息不是语音消息");
        }
        if (message.getAudioUrl() == null || message.getAudioUrl().isBlank()) {
            throw new ApiException(404, "语音文件不存在");
        }
        byte[] bytes = Files.readAllBytes(mediaStorageService.resolveStoredFileWithFallback(message.getAudioUrl()));
        String filename = messageId + voiceFilenameSuffix(message.getAudioUrl());
        return new DirectMessageMediaPayload(bytes, voiceMediaTypeOf(message.getAudioUrl()), filename);
    }

    @Transactional(readOnly = true)
    public DirectMessageMediaPayload loadImageMedia(AuthPrincipal principal, String messageId) throws IOException {
        ElderContext context = resolveCurrentElderContext(principal);
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(404, "图片消息不存在"));
        DirectMessageThread thread = directMessageThreadRepository.findById(message.getThreadId())
                .orElseThrow(() -> new ApiException(404, "图片消息不存在"));
        if (!belongsToThread(context.scopeKey(), thread)) {
            throw new ApiException(4030, "无权访问该图片文件");
        }
        if (!IMAGE_KIND.equalsIgnoreCase(message.getMessageKind())) {
            throw new ApiException(4001, "该消息不是图片消息");
        }
        if (message.getImageUrl() == null || message.getImageUrl().isBlank()) {
            throw new ApiException(404, "图片文件不存在");
        }
        byte[] bytes = Files.readAllBytes(mediaStorageService.resolveStoredFileWithFallback(message.getImageUrl()));
        String filename = messageId + imageExtensionOf(message.getImageUrl());
        return new DirectMessageMediaPayload(bytes, imageMediaTypeOf(message.getImageUrl()), filename);
    }

    public record DirectMessageMediaPayload(byte[] bytes, MediaType mediaType, String filename) {
    }

    private void touchThread(DirectMessageThread thread) {
        thread.setUpdatedAt(LocalDateTime.now());
        directMessageThreadRepository.save(thread);
    }

    private boolean belongsToThread(String scopeKey, DirectMessageThread thread) {
        return scopeKey.equals(thread.getParticipantAScopeKey()) || scopeKey.equals(thread.getParticipantBScopeKey());
    }

    private DirectMessageThread getOrCreateThread(ElderContext context, PeerCandidate peer) {
        String ownerScope = context.scopeKey();
        String peerScope = peer.scopeKey();
        String first = ownerScope.compareTo(peerScope) <= 0 ? ownerScope : peerScope;
        String second = ownerScope.compareTo(peerScope) <= 0 ? peerScope : ownerScope;
        DirectMessageThread thread = directMessageThreadRepository
                .findByParticipantAScopeKeyAndParticipantBScopeKey(first, second)
                .orElseGet(() -> {
                    DirectMessageThread created = new DirectMessageThread();
                    created.setParticipantAScopeKey(first);
                    created.setParticipantBScopeKey(second);
                    if (first.equals(ownerScope)) {
                        created.setParticipantAElderProfileId(context.elderProfile().getId());
                        created.setParticipantBElderProfileId(peer.elderProfileId());
                    } else {
                        created.setParticipantAElderProfileId(peer.elderProfileId());
                        created.setParticipantBElderProfileId(context.elderProfile().getId());
                    }
                    created.setCreatedAt(LocalDateTime.now());
                    created.setUpdatedAt(LocalDateTime.now());
                    return directMessageThreadRepository.save(created);
                });
        if (thread.getId() == null) {
            thread = directMessageThreadRepository.save(thread);
        }
        return thread;
    }

    private void validateVoice(MultipartFile file, Long durationMs) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(4001, "请上传语音文件");
        }
        if (durationMs != null && durationMs < 400L) {
            throw new ApiException(4001, "语音时长不能少于 400 毫秒");
        }
        String ct = file.getContentType();
        if (ct != null) {
            String normalized = ct.trim().toLowerCase(Locale.ROOT);
            boolean allowed = normalized.equals("audio/mp4")
                    || normalized.equals("audio/m4a")
                    || normalized.equals("audio/aac")
                    || normalized.equals("audio/x-m4a")
                    || normalized.equals("video/mp4")
                    || normalized.equals("application/octet-stream");
            if (!allowed) {
                throw new ApiException(4001, "仅支持 m4a / aac 语音文件");
            }
        }
    }

    private void validateImage(MultipartFile file) {
        MedicalMultipartValidator.validate(file);
        String ct = file.getContentType();
        if (ct != null) {
            String normalized = ct.trim().toLowerCase(Locale.ROOT);
            boolean allowed = normalized.equals("image/jpeg")
                    || normalized.equals("image/jpg")
                    || normalized.equals("image/png")
                    || normalized.equals("image/webp")
                    || normalized.equals("application/octet-stream");
            if (!allowed) {
                throw new ApiException(4001, "仅支持 jpg / png / webp 图片文件");
            }
        }
    }

    private static int normalizePageSize(Integer limit) {
        if (limit == null || limit < 1) {
            return 50;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private static MessageSlice sliceMessages(List<DirectMessage> messages, String before, int limit) {
        if (messages.isEmpty()) {
            return new MessageSlice(List.of(), false);
        }
        if (before == null || before.isBlank()) {
            int from = Math.max(messages.size() - limit, 0);
            List<DirectMessage> items = new ArrayList<>(messages.subList(from, messages.size()));
            return new MessageSlice(items, messages.size() > items.size());
        }
        int index = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (before.equals(messages.get(i).getId())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ApiException(404, "消息不存在");
        }
        List<DirectMessage> older = messages.subList(0, index);
        int from = Math.max(older.size() - limit, 0);
        List<DirectMessage> items = new ArrayList<>(older.subList(from, older.size()));
        return new MessageSlice(items, older.size() > items.size());
    }

    private ElderFriendResponse toFriendResponse(ElderFriend friend) {
        return new ElderFriendResponse(
                friend.getFriendScopeKey(),
                friend.getDisplayName(),
                friend.getPhone(),
                friend.getHint() == null ? "" : friend.getHint(),
                friend.getEmoji() == null ? UNKNOWN_EMOJI : friend.getEmoji(),
                toMillis(friend.getAddedAt()));
    }

    private DirectMessageResponse toMessageResponse(DirectMessage message, String ownerScopeKey, Long threadId) {
        String audioUrl = VOICE_KIND.equalsIgnoreCase(message.getMessageKind()) ? voiceDownloadUrl(message.getId()) : null;
        String imageUrl = IMAGE_KIND.equalsIgnoreCase(message.getMessageKind()) ? imageDownloadUrl(message.getId()) : null;
        return new DirectMessageResponse(
                message.getId(),
                threadId,
                message.getSenderScopeKey(),
                message.getSenderDisplayName(),
                message.getSenderRole(),
                message.getMessageKind(),
                message.getTextContent(),
                audioUrl,
                imageUrl,
                message.getDurationMs(),
                toMillis(message.getCreatedAt()),
                ownerScopeKey.equals(message.getSenderScopeKey()));
    }

    private static long toMillis(LocalDateTime dateTime) {
        return dateTime == null ? 0L : dateTime.atZone(SHANGHAI).toInstant().toEpochMilli();
    }

    private static String voiceDownloadUrl(String messageId) {
        return "/v1/elder/direct-messages/messages/" + messageId + "/voice";
    }

    private static String imageDownloadUrl(String messageId) {
        return "/v1/elder/direct-messages/messages/" + messageId + "/image";
    }

    private static MediaType voiceMediaTypeOf(String storedPath) {
        String lower = storedPath == null ? "" : storedPath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".aac")) {
            return MediaType.parseMediaType("audio/aac");
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".m4a")) {
            return MediaType.parseMediaType("audio/mp4");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private static String voiceFilenameSuffix(String storedPath) {
        String lower = storedPath == null ? "" : storedPath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".aac")) {
            return ".aac";
        }
        if (lower.endsWith(".mp4")) {
            return ".mp4";
        }
        return ".m4a";
    }

    private static MediaType imageMediaTypeOf(String storedPath) {
        String lower = storedPath == null ? "" : storedPath.toLowerCase(Locale.ROOT);
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

    private static String imageExtensionOf(String storedPath) {
        String lower = storedPath == null ? "" : storedPath.toLowerCase(Locale.ROOT);
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

    private static String resolveSenderDisplayName(User user, ElderProfile profile) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName().trim();
        }
        if (user.getPhone() != null && user.getPhone().length() >= 4) {
            return "手机尾号" + user.getPhone().substring(user.getPhone().length() - 4);
        }
        return "我";
    }

    private static String normalizeKind(String kind) {
        return kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (!normalized.matches("^1[3-9]\\d{9}$")) {
            return null;
        }
        return normalized;
    }

    private String buildMessageId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private void addCandidateIfVisible(Map<String, ElderFriendCandidateResponse> candidates, ElderContext context,
            PeerCandidate candidate) {
        if (candidate == null) {
            return;
        }
        if (candidate.scopeKey().equals(context.scopeKey())) {
            return;
        }
        if (isAlreadyFriend(context.elderProfile().getId(), candidate.scopeKey(), candidate.phone())) {
            return;
        }
        candidates.putIfAbsent(candidate.scopeKey(), new ElderFriendCandidateResponse(
                candidate.scopeKey(),
                candidate.displayName(),
                candidate.phone(),
                candidate.hint(),
                candidate.emoji()));
    }

    private boolean isAlreadyFriend(Long ownerProfileId, String friendScopeKey, String phone) {
        if (elderFriendRepository.existsByOwnerElderProfileIdAndFriendScopeKey(ownerProfileId, friendScopeKey)) {
            return true;
        }
        return phone != null && !phone.isBlank() && elderFriendRepository.existsByOwnerElderProfileIdAndPhone(ownerProfileId, phone);
    }

    /**
     * 已注册老人互为好友：对方列表中也写入己方，便于双方都能在「我的好友」中看到彼此。
     */
    private void ensureReciprocalFriend(ElderContext owner, PeerCandidate peer) {
        Long peerProfileId = peer.elderProfileId();
        if (peerProfileId == null) {
            return;
        }
        if (peerProfileId.equals(owner.elderProfile().getId())) {
            return;
        }
        String ownerPhone = resolveOwnerPhone(owner);
        if (isAlreadyFriend(peerProfileId, owner.scopeKey(), ownerPhone)) {
            return;
        }

        ElderFriend reciprocal = new ElderFriend();
        reciprocal.setOwnerElderProfileId(peerProfileId);
        reciprocal.setOwnerScopeKey(peer.scopeKey());
        reciprocal.setFriendScopeKey(owner.scopeKey());
        reciprocal.setFriendElderProfileId(owner.elderProfile().getId());
        reciprocal.setDisplayName(resolveSenderDisplayName(owner.user(), owner.elderProfile()));
        reciprocal.setPhone(ownerPhone);
        reciprocal.setHint(REGISTERED_ELDER_HINT);
        reciprocal.setEmoji(resolveEmoji(owner.user().getGender()));
        reciprocal.setAddedAt(LocalDateTime.now());
        elderFriendRepository.save(reciprocal);
    }

    private void removeReciprocalFriend(Long peerElderProfileId, String ownerScopeKey) {
        if (peerElderProfileId == null || ownerScopeKey == null || ownerScopeKey.isBlank()) {
            return;
        }
        elderFriendRepository
                .findByOwnerElderProfileIdAndFriendScopeKey(peerElderProfileId, ownerScopeKey)
                .ifPresent(elderFriendRepository::delete);
    }

    private static String resolveOwnerPhone(ElderContext owner) {
        String phone = owner.user().getPhone();
        return phone == null ? "" : phone.trim();
    }

    private PeerCandidate resolvePeerCandidate(String phone, String scopeKey) {
        String normalizedPhone = normalizePhone(phone);
        if (scopeKey != null && !scopeKey.isBlank()) {
            return resolvePeerByScopeKey(scopeKey.trim(), normalizedPhone);
        }
        if (normalizedPhone != null) {
            return resolvePeerByPhone(normalizedPhone);
        }
        throw new ApiException(4001, "phone 或 scopeKey 至少提供一个");
    }

    private PeerCandidate resolvePeerForChat(ElderContext context, String peerScopeKey) {
        if (peerScopeKey == null || peerScopeKey.isBlank()) {
            throw new ApiException(4001, "peerScopeKey 不能为空");
        }
        PeerCandidate peer = resolvePeerByScopeKey(peerScopeKey.trim(), null);
        if (peer.scopeKey().equals(context.scopeKey())) {
            throw new ApiException(4001, "不能和自己发私聊");
        }
        return peer;
    }

    private PeerCandidate resolvePeerByScopeKey(String scopeKey, String fallbackPhone) {
        CommunityDemoPeerProfile demo = demoPeerProfileRepository.findById(scopeKey).orElse(null);
        if (demo != null) {
            return toPeerCandidate(demo);
        }
        if (scopeKey.startsWith(PHONE_PREFIX)) {
            String phone = scopeKey.substring(PHONE_PREFIX.length()).trim();
            if (phone.matches("^1[3-9]\\d{9}$")) {
                return resolvePeerByPhone(phone);
            }
        }
        if (scopeKey.startsWith(ELDER_PREFIX)) {
            String suffix = scopeKey.substring(ELDER_PREFIX.length()).trim();
            try {
                Long elderProfileId = Long.valueOf(suffix);
                ElderProfile profile = elderProfileRepository.findById(elderProfileId)
                        .orElseThrow(() -> new ApiException(404, "好友候选不存在"));
                return new PeerCandidate(
                        profile.getPhone() != null && !profile.getPhone().isBlank() ? PHONE_PREFIX + profile.getPhone().trim()
                                : scopeKey,
                        resolveDisplayName(null, profile.getName(), profile.getPhone()),
                        profile.getPhone() == null ? "" : profile.getPhone().trim(),
                        REGISTERED_ELDER_HINT,
                        resolveEmoji(profile.getGender()),
                        profile.getId());
            } catch (NumberFormatException ex) {
                throw new ApiException(4001, "scopeKey 格式无效");
            }
        }
        if (fallbackPhone != null) {
            return resolvePeerByPhone(fallbackPhone);
        }
        return new PeerCandidate(scopeKey, scopeKey, "", "", UNKNOWN_EMOJI, null);
    }

    private PeerCandidate resolvePeerByPhone(String phone) {
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user != null && UserRole.elder.name().equalsIgnoreCase(user.getRole())) {
            ElderProfile profile = elderProfileRepository.findByPhone(phone).orElse(null);
            Long elderProfileId = profile != null ? profile.getId() : null;
            String displayName = resolveDisplayName(user.getName(), profile != null ? profile.getName() : null, phone);
            String hint = profile != null && profile.getStatus() != null && profile.getStatus().equalsIgnoreCase("claimed")
                    ? REGISTERED_ELDER_HINT
                    : "老人手机号";
            String emoji = resolveEmoji(user.getGender());
            return new PeerCandidate(PHONE_PREFIX + phone, displayName, phone, hint, emoji, elderProfileId);
        }

        CommunityDemoPeerProfile demo = demoPeerProfileRepository.findByPhone(phone).orElse(null);
        if (demo != null) {
            return toPeerCandidate(demo);
        }

        return new PeerCandidate(
                PHONE_PREFIX + phone,
                "手机尾号" + phone.substring(phone.length() - 4),
                phone,
                PHONE_ADD_HINT,
                UNKNOWN_EMOJI,
                null);
    }

    private PeerCandidate toPeerCandidate(CommunityDemoPeerProfile demo) {
        return new PeerCandidate(
                demo.getScopeKey(),
                demo.getDisplayName(),
                demo.getPhone(),
                demo.getHint() == null ? "" : demo.getHint(),
                demo.getEmoji() == null ? UNKNOWN_EMOJI : demo.getEmoji(),
                demo.getLinkedElderProfileId());
    }

    private PeerCandidate toRegisteredElderCandidate(User elderUser) {
        ElderProfile profile = elderProfileRepository.findByPhone(elderUser.getPhone()).orElse(null);
        String phone = elderUser.getPhone() == null ? "" : elderUser.getPhone().trim();
        String displayName = resolveDisplayName(elderUser.getName(), profile != null ? profile.getName() : null, phone);
        String emoji = resolveEmoji(elderUser.getGender());
        return new PeerCandidate(PHONE_PREFIX + phone, displayName, phone, REGISTERED_ELDER_HINT, emoji,
                profile == null ? null : profile.getId());
    }

    private static String resolveDisplayName(String userName, String profileName, String phone) {
        if (userName != null && !userName.isBlank()) {
            return userName.trim();
        }
        if (profileName != null && !profileName.isBlank()) {
            return profileName.trim();
        }
        if (phone != null && phone.length() >= 4) {
            return "手机尾号" + phone.substring(phone.length() - 4);
        }
        return "好友";
    }

    private static String resolveEmoji(String gender) {
        if (gender == null) {
            return UNKNOWN_EMOJI;
        }
        if ("female".equalsIgnoreCase(gender)) {
            return "👵";
        }
        if ("male".equalsIgnoreCase(gender)) {
            return "👴";
        }
        return UNKNOWN_EMOJI;
    }

    private ElderContext resolveCurrentElderContext(AuthPrincipal principal) {
        if (principal.role() != UserRole.elder) {
            throw new ApiException(4030, "forbidden");
        }
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(4010, "unauthorized"));
        ElderProfile profile = null;
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            profile = elderProfileRepository.findByPhone(user.getPhone().trim()).orElse(null);
        }
        if (profile == null) {
            profile = elderProfileRepository.findByClaimedUserId(user.getId()).orElse(null);
        }
        if (profile == null) {
            throw new ApiException(4030, "请先完成老人档案认领后再使用好友与私聊功能");
        }
        String scopeKey = buildCurrentScopeKey(user, profile);
        return new ElderContext(user, profile, scopeKey);
    }

    private static String buildCurrentScopeKey(User user, ElderProfile profile) {
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return PHONE_PREFIX + user.getPhone().trim();
        }
        return ELDER_PREFIX + profile.getId();
    }

    private record ElderContext(User user, ElderProfile elderProfile, String scopeKey) {
    }

    private record PeerCandidate(String scopeKey, String displayName, String phone, String hint, String emoji,
            Long elderProfileId) {
    }

    private record MessageSlice(List<DirectMessage> items, boolean hasMore) {
    }
}