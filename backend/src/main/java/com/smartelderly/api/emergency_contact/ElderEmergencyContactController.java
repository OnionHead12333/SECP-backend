package com.smartelderly.api.emergency_contact;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.emergency_contact.dto.ElderAddEmergencyContactRequest;
import com.smartelderly.api.emergency_contact.dto.ElderEmergencyContactResponse;
import com.smartelderly.service.emergency_contact.EmergencyContactService;

@RestController
@RequestMapping("/v1/elder/emergency-contacts")
public class ElderEmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    public ElderEmergencyContactController(EmergencyContactService emergencyContactService) {
        this.emergencyContactService = emergencyContactService;
    }

    /**
     * 获取老人的紧急联系人列表
     * @param elderPhone 老人手机号（查询参数）
     * @return 紧急联系人列表，按优先级升序排列
     */
    @GetMapping
    public ApiResponse<List<ElderEmergencyContactResponse>> getEmergencyContacts(
            @RequestParam("elderPhone") String elderPhone) {
        return emergencyContactService.getElderEmergencyContacts(elderPhone);
    }

    /**
     * 新增紧急联系人
     * @param request 新增请求：elderPhone, name, phone, relation, isPrimary
     * @return 新增后的完整联系人列表
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<ElderEmergencyContactResponse>> addEmergencyContact(
            @RequestBody ElderAddEmergencyContactRequest request) {
        return emergencyContactService.addElderEmergencyContact(request);
    }
}

