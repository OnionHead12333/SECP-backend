package com.smartelderly.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.dto.ChildBoundElderResponse;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.FamilyBinding;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.ElderProfileRepository;

@Service
public class ChildBoundElderService {

    private final FamilyBindingRepository familyBindingRepository;
    private final ElderProfileRepository elderProfileRepository;

    public ChildBoundElderService(
            FamilyBindingRepository familyBindingRepository,
            ElderProfileRepository elderProfileRepository) {
        this.familyBindingRepository = familyBindingRepository;
        this.elderProfileRepository = elderProfileRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ChildBoundElderResponse>> listBoundElders(long childUserId) {
        List<FamilyBinding> bindings = familyBindingRepository.findByChildUserIdAndStatus(
                childUserId, BindingStatus.active);
        List<ChildBoundElderResponse> out = new ArrayList<>();
        for (FamilyBinding b : bindings) {
            elderProfileRepository.findById(b.getElderProfileId()).ifPresent(profile -> {
                String rel = b.getRelation() != null && !b.getRelation().isBlank() ? b.getRelation() : "家人";
                out.add(new ChildBoundElderResponse(
                        profile.getId(),
                        profile.getName() != null ? profile.getName() : "",
                        profile.getPhone() != null ? profile.getPhone() : "",
                        rel,
                        Boolean.TRUE.equals(b.getIsPrimary())));
            });
        }
        return ApiResponse.ok(out);
    }
}
