package com.smartelderly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.dto.ChildBoundElderResponse;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.FamilyBinding;
import com.smartelderly.domain.FamilyBindingRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("子女绑定老人服务单元测试")
class ChildBoundElderServiceTest {

    @Mock
    private FamilyBindingRepository familyBindingRepository;

    @Mock
    private ElderProfileRepository elderProfileRepository;

    @InjectMocks
    private ChildBoundElderService childBoundElderService;

    @Test
    @DisplayName("查询绑定老人：active 绑定应映射为响应列表")
    void listBoundElders_activeBindings_returnsMappedProfiles() {
        FamilyBinding binding = binding(7L, 20L, "父亲", true);
        ElderProfile profile = profile(7L, "张三", "13900000001");

        when(familyBindingRepository.findByChildUserIdAndStatus(20L, BindingStatus.active))
                .thenReturn(List.of(binding));
        when(elderProfileRepository.findById(7L)).thenReturn(Optional.of(profile));

        ApiResponse<List<ChildBoundElderResponse>> response = childBoundElderService.listBoundElders(20L);

        assertEquals(0, response.getCode());
        assertEquals(1, response.getData().size());
        ChildBoundElderResponse elder = response.getData().get(0);
        assertEquals(7L, elder.elderProfileId());
        assertEquals("张三", elder.name());
        assertEquals("13900000001", elder.phone());
        assertEquals("父亲", elder.relation());
        assertTrue(elder.isPrimary());
    }

    @Test
    @DisplayName("查询绑定老人：关系为空时应使用默认关系")
    void listBoundElders_blankRelation_usesDefaultRelation() {
        FamilyBinding binding = binding(8L, 20L, " ", false);
        ElderProfile profile = profile(8L, "李四", "13900000002");

        when(familyBindingRepository.findByChildUserIdAndStatus(20L, BindingStatus.active))
                .thenReturn(List.of(binding));
        when(elderProfileRepository.findById(8L)).thenReturn(Optional.of(profile));

        var response = childBoundElderService.listBoundElders(20L);

        assertEquals(1, response.getData().size());
        assertEquals("家人", response.getData().get(0).relation());
        assertFalse(response.getData().get(0).isPrimary());
    }

    @Test
    @DisplayName("查询绑定老人：老人档案缺失时跳过该绑定")
    void listBoundElders_missingProfile_skipsBinding() {
        FamilyBinding binding = binding(9L, 20L, "母亲", true);

        when(familyBindingRepository.findByChildUserIdAndStatus(20L, BindingStatus.active))
                .thenReturn(List.of(binding));
        when(elderProfileRepository.findById(9L)).thenReturn(Optional.empty());

        var response = childBoundElderService.listBoundElders(20L);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    @DisplayName("查询绑定老人：没有 active 绑定时返回空列表")
    void listBoundElders_noActiveBindings_returnsEmptyList() {
        when(familyBindingRepository.findByChildUserIdAndStatus(20L, BindingStatus.active))
                .thenReturn(List.of());

        var response = childBoundElderService.listBoundElders(20L);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isEmpty());
        verifyNoInteractions(elderProfileRepository);
    }

    private static FamilyBinding binding(Long elderProfileId, Long childUserId, String relation, boolean primary) {
        FamilyBinding binding = new FamilyBinding();
        binding.setElderProfileId(elderProfileId);
        binding.setChildUserId(childUserId);
        binding.setRelation(relation);
        binding.setIsPrimary(primary);
        binding.setStatus(BindingStatus.active);
        return binding;
    }

    private static ElderProfile profile(Long id, String name, String phone) {
        ElderProfile profile = new ElderProfile();
        profile.setId(id);
        profile.setName(name);
        profile.setPhone(phone);
        return profile;
    }
}
