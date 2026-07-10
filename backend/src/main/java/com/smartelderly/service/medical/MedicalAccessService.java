package com.smartelderly.service.medical;

import org.springframework.stereotype.Service;

import com.smartelderly.api.ApiException;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.AuthPrincipal;

@Service
public class MedicalAccessService {

    private final ElderProfileRepository elderProfileRepository;
    private final FamilyBindingRepository familyBindingRepository;

    public MedicalAccessService(
            ElderProfileRepository elderProfileRepository,
            FamilyBindingRepository familyBindingRepository) {
        this.elderProfileRepository = elderProfileRepository;
        this.familyBindingRepository = familyBindingRepository;
    }

    /**
     * 子女必须显式传 {@code elderProfileId}；老人默认操作本人已认领档案。
     */
    public long resolveTargetElderProfileId(AuthPrincipal principal, Long elderProfileId) {
        if (principal.role() == UserRole.elder) {
            long mine =
                    elderProfileRepository
                            .findByClaimedUserId(principal.userId())
                            .orElseThrow(
                                    () ->
                                            new ApiException(
                                                    4030, "请先完成老人档案认领后再使用医疗归档"))
                            .getId();
            if (elderProfileId != null && !elderProfileId.equals(mine)) {
                throw new ApiException(4030, "无权操作其他老人医疗档案");
            }
            return mine;
        }
        if (principal.role() == UserRole.child) {
            if (elderProfileId == null) {
                throw new ApiException(4001, "请指定 elderProfileId（为谁建档）");
            }
            if (!familyBindingRepository.existsByChildUserIdAndElderProfileIdAndStatus(
                    principal.userId(), elderProfileId, BindingStatus.active)) {
                throw new ApiException(4030, "无权管理该老人的医疗数据");
            }
            return elderProfileId;
        }
        throw new ApiException(4030, "当前角色不支持医疗归档功能");
    }
}
