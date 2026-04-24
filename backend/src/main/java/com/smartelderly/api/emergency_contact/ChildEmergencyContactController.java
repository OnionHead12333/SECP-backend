package com.smartelderly.api.emergency_contact;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.emergency_contact.dto.AddEmergencyContactRequest;
import com.smartelderly.api.emergency_contact.dto.EmergencyContactResponse;
import com.smartelderly.api.emergency_contact.dto.UpdateEmergencyContactRequest;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.emergency_contact.EmergencyContactService;

@RestController
@RequestMapping("/v1/children/elders")
public class ChildEmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    public ChildEmergencyContactController(EmergencyContactService emergencyContactService) {
        this.emergencyContactService = emergencyContactService;
    }

    /**
     * 获取老人的紧急联系人列表
     * @param elderId 老人档案ID
     * @return 紧急联系人列表，按优先级升序排列（数字越小越优先）
     */
    @GetMapping("/{elderId}/emergency-contacts")
    public ApiResponse<List<EmergencyContactResponse>> getEmergencyContacts(@PathVariable("elderId") Long elderId) {
        // 验证子女身份
        var user = SecurityUtils.requireRole(UserRole.child);
        return emergencyContactService.getEmergencyContacts(elderId);
    }

    /**
     * 新建紧急联系人
     * @param elderId 老人档案ID
     * @param request 新增请求：name, phone, relation, priority
     * @return 新增后的紧急联系人信息
     */
    @PostMapping("/{elderId}/emergency-contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EmergencyContactResponse> addEmergencyContact(
            @PathVariable("elderId") Long elderId,
            @RequestBody AddEmergencyContactRequest request) {
        // 验证子女身份
        var user = SecurityUtils.requireRole(UserRole.child);
        return emergencyContactService.addEmergencyContact(elderId, request);
    }

    /**
     * 修改紧急联系人（部分更新）
     * @param elderId 老人档案ID
     * @param contactId 联系人ID
     * @param request 修改请求：name, phone, relation, priority（可选）
     * @return 修改后的紧急联系人信息
     */
    @PatchMapping("/{elderId}/emergency-contacts/{contactId}")
    public ApiResponse<EmergencyContactResponse> updateEmergencyContact(
            @PathVariable("elderId") Long elderId,
            @PathVariable("contactId") Long contactId,
            @RequestBody UpdateEmergencyContactRequest request) {
        // 验证子女身份
        var user = SecurityUtils.requireRole(UserRole.child);
        return emergencyContactService.updateEmergencyContact(elderId, contactId, request);
    }

    /**
     * 删除紧急联系人
     * @param elderId 老人档案ID
     * @param contactId 联系人ID
     * @return 成功删除后的响应（200 OK）
     */
    @DeleteMapping("/{elderId}/emergency-contacts/{contactId}")
    public ApiResponse<Void> deleteEmergencyContact(
            @PathVariable("elderId") Long elderId,
            @PathVariable("contactId") Long contactId) {
        // 验证子女身份
        var user = SecurityUtils.requireRole(UserRole.child);
        return emergencyContactService.deleteEmergencyContact(elderId, contactId);
    }
}
