package com.smartelderly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.api.dto.RegisterChildWithEldersRequest;
import com.smartelderly.domain.BindingStatus;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.FamilyBinding;
import com.smartelderly.domain.FamilyBindingRepository;
import com.smartelderly.domain.User;
import com.smartelderly.service.emergency_contact.EmergencyContactService;

@ExtendWith(MockitoExtension.class)
@DisplayName("子女注册并绑定老人服务单元测试")
class AuthChildRegistrationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ElderProfileRepository elderProfileRepository;

    @Mock
    private FamilyBindingRepository familyBindingRepository;

    @Mock
    private EmergencyContactService emergencyContactService;

    @InjectMocks
    private AuthChildRegistrationService service;

    @Test
    @DisplayName("注册子女并绑定老人：新老人档案应创建绑定和紧急联系人")
    void registerChildWithElders_newElder_createsProfileBindingAndContact() {
        var request = request("张小明", "13800000001", "13900000001");
        User savedChild = childUser(20L, "张小明", "13800000001");
        ElderProfile savedProfile = elderProfile(7L, "王老人", "13900000001", "unclaimed");

        when(userService.findByUsername("13800000001")).thenReturn(Optional.empty());
        when(userService.findByPhone("13800000001")).thenReturn(Optional.empty());
        when(userService.register(any(User.class), eq("Test123456"))).thenReturn(savedChild);
        when(elderProfileRepository.findByPhone("13900000001")).thenReturn(Optional.empty());
        when(elderProfileRepository.save(any(ElderProfile.class))).thenReturn(savedProfile);
        when(familyBindingRepository.existsByChildUserIdAndElderProfileId(20L, 7L)).thenReturn(false);
        when(familyBindingRepository.findByChildUserIdAndStatus(20L, BindingStatus.active))
                .thenReturn(List.of(new FamilyBinding()));

        var response = service.registerChildWithElders(request);

        assertEquals(0, response.getCode());
        assertEquals("child", response.getData().getRole());
        assertEquals(1, response.getData().getFamilyCount());

        ArgumentCaptor<FamilyBinding> bindingCaptor = ArgumentCaptor.forClass(FamilyBinding.class);
        verify(familyBindingRepository).save(bindingCaptor.capture());
        assertEquals(7L, bindingCaptor.getValue().getElderProfileId());
        assertEquals(20L, bindingCaptor.getValue().getChildUserId());
        assertEquals(BindingStatus.active, bindingCaptor.getValue().getStatus());
        assertTrue(bindingCaptor.getValue().getIsPrimary());

        verify(emergencyContactService).ensureChildAsPriorityOneEmergencyContact(
                7L, "张小明", "13800000001", "子女");
    }

    @Test
    @DisplayName("注册子女并绑定老人：子女手机号已注册时返回错误")
    void registerChildWithElders_childPhoneExists_returnsError() {
        var request = request("张小明", "13800000001", "13900000001");
        when(userService.findByUsername("13800000001")).thenReturn(Optional.of(childUser(20L, "张小明", "13800000001")));

        var response = service.registerChildWithElders(request);

        assertEquals(400, response.getCode());
        verify(userService, never()).register(any(), anyString());
    }

    @Test
    @DisplayName("注册子女并绑定老人：老人手机号不能与子女相同")
    void registerChildWithElders_elderPhoneSameAsChild_returnsError() {
        var request = request("张小明", "13800000001", "13800000001");
        when(userService.findByUsername("13800000001")).thenReturn(Optional.empty());
        when(userService.findByPhone("13800000001")).thenReturn(Optional.empty());

        var response = service.registerChildWithElders(request);

        assertEquals(400, response.getCode());
        verify(userService, never()).register(any(), anyString());
    }

    @Test
    @DisplayName("注册子女并绑定老人：老人列表手机号重复时返回错误")
    void registerChildWithElders_duplicateElderPhones_returnsError() {
        var request = request("张小明", "13800000001", "13900000001");
        request.getElders().add(elderItem("王老人2", "13900000001", "父亲"));
        when(userService.findByUsername("13800000001")).thenReturn(Optional.empty());
        when(userService.findByPhone("13800000001")).thenReturn(Optional.empty());

        var response = service.registerChildWithElders(request);

        assertEquals(400, response.getCode());
        verify(userService, never()).register(any(), anyString());
    }

    @Test
    @DisplayName("注册子女并绑定老人：不可绑定状态的老人档案应返回错误")
    void registerChildWithElders_unbindableProfile_returnsError() {
        var request = request("张小明", "13800000001", "13900000001");
        User savedChild = childUser(20L, "张小明", "13800000001");
        ElderProfile archived = elderProfile(7L, "王老人", "13900000001", "archived");

        when(userService.findByUsername("13800000001")).thenReturn(Optional.empty());
        when(userService.findByPhone("13800000001")).thenReturn(Optional.empty());
        when(userService.register(any(User.class), eq("Test123456"))).thenReturn(savedChild);
        when(elderProfileRepository.findByPhone("13900000001")).thenReturn(Optional.of(archived));

        var response = service.registerChildWithElders(request);

        assertEquals(400, response.getCode());
        verify(familyBindingRepository, never()).save(any());
    }

    private static RegisterChildWithEldersRequest request(String childName, String childPhone, String elderPhone) {
        RegisterChildWithEldersRequest request = new RegisterChildWithEldersRequest();
        RegisterChildWithEldersRequest.ChildBlock child = new RegisterChildWithEldersRequest.ChildBlock();
        child.setName(childName);
        child.setNickname("小明");
        child.setPhone(childPhone);
        child.setPassword("Test123456");
        request.setChild(child);
        request.setElders(new java.util.ArrayList<>(List.of(elderItem("王老人", elderPhone, "子女"))));
        return request;
    }

    private static RegisterChildWithEldersRequest.ElderItem elderItem(String name, String phone, String relation) {
        RegisterChildWithEldersRequest.ElderItem elder = new RegisterChildWithEldersRequest.ElderItem();
        elder.setName(name);
        elder.setPhone(phone);
        elder.setRelation(relation);
        return elder;
    }

    private static User childUser(Long id, String name, String phone) {
        User user = new User();
        user.setId(id);
        user.setUsername(phone);
        user.setName(name);
        user.setPhone(phone);
        user.setRole("child");
        return user;
    }

    private static ElderProfile elderProfile(Long id, String name, String phone, String status) {
        ElderProfile profile = new ElderProfile();
        profile.setId(id);
        profile.setName(name);
        profile.setPhone(phone);
        profile.setStatus(status);
        return profile;
    }
}
