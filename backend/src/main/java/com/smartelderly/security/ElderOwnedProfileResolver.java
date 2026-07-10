package com.smartelderly.security;

import com.smartelderly.api.ApiException;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.UserRole;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 老人端接口：先按 {@link com.smartelderly.domain.ElderProfile#getClaimedUserId()} 解析当前用户对应档案，
 * 再校验请求中的 elderId 是否与该档案主键一致；亦接受误传 {@code users.id}（移动端曾用登录 userId 当 elderId）。
 */
@Component
public class ElderOwnedProfileResolver {

    private final ElderProfileRepository elderProfileRepository;

    public ElderOwnedProfileResolver(ElderProfileRepository elderProfileRepository) {
        this.elderProfileRepository = elderProfileRepository;
    }

    /**
     * @param requestedElderId 客户端传入的 elderId（可能为档案主键或用户主键）
     * @return 统一后的老人档案主键 {@link ElderProfile#getId()}
     */
    public long resolveRequestElderId(long requestedElderId) {
        var user = SecurityUtils.requireRole(UserRole.elder);
        ElderProfile profile = elderProfileRepository.findByClaimedUserId(user.userId())
                .orElseThrow(() -> new ApiException(404, "老人档案不存在"));
        long profileId = Objects.requireNonNull(profile.getId());
        if (!Objects.equals(requestedElderId, profileId) && !Objects.equals(requestedElderId, user.userId())) {
            throw new ApiException(403, "无权操作该老人档案");
        }
        return profileId;
    }
}
