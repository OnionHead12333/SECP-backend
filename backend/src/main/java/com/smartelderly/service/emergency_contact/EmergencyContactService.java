package com.smartelderly.service.emergency_contact;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.emergency_contact.dto.AddEmergencyContactRequest;
import com.smartelderly.api.emergency_contact.dto.ElderAddEmergencyContactRequest;
import com.smartelderly.api.emergency_contact.dto.ElderEmergencyContactResponse;
import com.smartelderly.api.emergency_contact.dto.EmergencyContactResponse;
import com.smartelderly.api.emergency_contact.dto.UpdateEmergencyContactRequest;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.User;
import com.smartelderly.domain.UserRepository;
import com.smartelderly.domain.emergency_contact.EmergencyContact;
import com.smartelderly.domain.emergency_contact.EmergencyContactRepository;

@Service
public class EmergencyContactService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final EmergencyContactRepository emergencyContactRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final UserRepository userRepository;

    public EmergencyContactService(EmergencyContactRepository emergencyContactRepository,
            ElderProfileRepository elderProfileRepository,
            UserRepository userRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取老人的所有紧急联系人，按优先级升序排列
     * 
     * @param elderId 老人档案ID
     * @return ApiResponse 包含紧急联系人列表
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<EmergencyContactResponse>> getEmergencyContacts(Long elderId) {
        // 验证老人档案是否存在
        elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 查询该老人的紧急联系人列表，按priority升序排列
        List<EmergencyContact> contacts = emergencyContactRepository
                .findByElderProfileIdOrderByPriorityAsc(elderId);

        // 转换为Response对象
        List<EmergencyContactResponse> responses = contacts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ApiResponse.ok(responses);
    }

    /**
     * 新增紧急联系人
     * 同一老人下的手机号必须唯一（去重）
     * 如果新增的联系人priority为1，则其他联系人的priority下降一档（加1）
     * 
     * @param elderId 老人档案ID
     * @param request 新增请求
     * @return ApiResponse 包含新增后的联系人信息
     */
    @Transactional
    public ApiResponse<EmergencyContactResponse> addEmergencyContact(Long elderId, AddEmergencyContactRequest request) {
        // 验证老人档案是否存在
        elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 检查同一老人下是否已存在该手机号（去重）
        if (emergencyContactRepository.existsByElderProfileIdAndPhone(elderId, request.getPhone())) {
            throw new ApiException(409, "该老人下该手机号的紧急联系人已存在");
        }

        // 如果新增的联系人priority为1，则调整其他联系人的priority
        if (request.getPriority() != null && request.getPriority() == 1) {
            // 查询该老人的所有现有联系人
            List<EmergencyContact> existingContacts = emergencyContactRepository
                    .findByElderProfileIdOrderByPriorityAsc(elderId);
            
            // 将所有现有联系人的priority加1（下降一档）
            for (EmergencyContact contact : existingContacts) {
                contact.setPriority(contact.getPriority() + 1);
            }
            
            // 批量保存调整后的联系人
            emergencyContactRepository.saveAll(existingContacts);
        }

        // 创建新的紧急联系人
        EmergencyContact contact = new EmergencyContact();
        contact.setElderProfileId(elderId);
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setRelation(request.getRelation());
        contact.setPriority(request.getPriority());

        // 保存到数据库
        EmergencyContact savedContact = emergencyContactRepository.save(contact);

        // 转换为Response对象
        EmergencyContactResponse response = toResponse(savedContact);

        return ApiResponse.ok("success", response);
    }

    /**
     * 子女与老人刚建立家庭绑定时：若该老人下尚无此手机号，则把子女加为 priority=1 的紧急联系人，其余联系人顺延 +1（与
     * {@link #addEmergencyContact} 规则一致）。若已存在同手机号则跳过，避免重复注册报错。
     */
    @Transactional
    public void ensureChildAsPriorityOneEmergencyContact(
            Long elderProfileId, String childName, String childPhone, String relation) {
        if (emergencyContactRepository.existsByElderProfileIdAndPhone(elderProfileId, childPhone)) {
            return;
        }
        var req = new AddEmergencyContactRequest();
        req.setName(childName);
        req.setPhone(childPhone);
        req.setRelation(relation != null && !relation.isBlank() ? relation : "子女");
        req.setPriority(1);
        addEmergencyContact(elderProfileId, req);
    }

    /**
     * 更新紧急联系人（支持部分更新）
     * 如果更新priority，则自动调整其他联系人的priority
     * 
     * @param elderId 老人档案ID
     * @param contactId 联系人ID
     * @param request 更新请求
     * @return ApiResponse 包含更新后的联系人信息
     */
    @Transactional
    public ApiResponse<EmergencyContactResponse> updateEmergencyContact(Long elderId, Long contactId, 
            UpdateEmergencyContactRequest request) {
        // 验证老人档案是否存在
        elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 查找要更新的联系人
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ApiException(4041, "contact not found"));

        // 验证联系人是否属于该老人
        if (!contact.getElderProfileId().equals(elderId)) {
            throw new ApiException(4042, "contact does not belong to this elder");
        }

        Integer oldPriority = contact.getPriority();
        Integer newPriority = request.getPriority();

        // 如果更新了phone，检查是否重复
        if (request.getPhone() != null && !request.getPhone().equals(contact.getPhone())) {
            if (emergencyContactRepository.existsByElderProfileIdAndPhone(elderId, request.getPhone())) {
                throw new ApiException(409, "该老人下该手机号的紧急联系人已存在");
            }
            contact.setPhone(request.getPhone());
        }

        // 如果更新了priority，调整其他联系人的priority
        if (newPriority != null && !newPriority.equals(oldPriority)) {
            List<EmergencyContact> allContacts = emergencyContactRepository
                    .findByElderProfileIdOrderByPriorityAsc(elderId);

            if (newPriority == 1) {
                // 新priority为1：所有其他priority >= 1的联系人priority加1
                for (EmergencyContact c : allContacts) {
                    if (!c.getId().equals(contactId) && c.getPriority() < oldPriority) {
                        c.setPriority(c.getPriority() + 1);
                    }
                }
            } else if (newPriority < oldPriority) {
                // 新priority < 旧priority（优先级提高）：
                // oldPriority-1 到 newPriority 范围内的联系人priority加1
                for (EmergencyContact c : allContacts) {
                    if (!c.getId().equals(contactId)) {
                        int p = c.getPriority();
                        if (p >= newPriority && p < oldPriority) {
                            c.setPriority(p + 1);
                        }
                    }
                }
            } else {
                // 新priority > 旧priority（优先级降低）：
                // oldPriority+1 到 newPriority 范围内的联系人priority减1
                for (EmergencyContact c : allContacts) {
                    if (!c.getId().equals(contactId)) {
                        int p = c.getPriority();
                        if (p > oldPriority && p <= newPriority) {
                            c.setPriority(p - 1);
                        }
                    }
                }
            }

            // 批量保存调整后的联系人
            emergencyContactRepository.saveAll(allContacts);

            // 更新当前联系人的priority
            contact.setPriority(newPriority);
        }

        // 更新其他字段
        if (request.getName() != null) {
            contact.setName(request.getName());
        }
        if (request.getRelation() != null) {
            contact.setRelation(request.getRelation());
        }

        // 保存更新
        EmergencyContact updatedContact = emergencyContactRepository.save(contact);

        // 转换为Response对象
        EmergencyContactResponse response = toResponse(updatedContact);

        return ApiResponse.ok("success", response);
    }

    /**
     * 删除紧急联系人
     * 删除后，重新整理priority：按创建时间从新到旧排序，依次分配priority从1开始
     * 这样可以避免出现优先级重复或冲突的情况
     * 
     * @param elderId 老人档案ID
     * @param contactId 联系人ID
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<Void> deleteEmergencyContact(Long elderId, Long contactId) {
        // 验证老人档案是否存在
        elderProfileRepository.findById(elderId)
                .orElseThrow(() -> new ApiException(4040, "elder not found"));

        // 查找要删除的联系人
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ApiException(4041, "contact not found"));

        // 验证联系人是否属于该老人
        if (!contact.getElderProfileId().equals(elderId)) {
            throw new ApiException(4042, "contact does not belong to this elder");
        }

        // 删除联系人
        emergencyContactRepository.deleteById(contactId);

        // 重新整理剩余联系人的priority：按创建时间从新到旧排序
        List<EmergencyContact> remainingContacts = emergencyContactRepository
                .findByElderProfileIdOrderByPriorityAsc(elderId)
                .stream()
                .sorted((a, b) -> {
                    // 创建时间晚的优先级高（排在前面）
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();

        // 重新分配priority，从1开始
        int newPriority = 1;
        for (EmergencyContact c : remainingContacts) {
            c.setPriority(newPriority);
            newPriority++;
        }

        // 批量保存调整后的联系人
        emergencyContactRepository.saveAll(remainingContacts);

        return ApiResponse.ok("success", null);
    }

    /**
     * 老人端：通过手机号查询紧急联系人列表
     * 
     * @param elderPhone 老人手机号
     * @return ApiResponse 包含紧急联系人列表（不包含note字段）
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<ElderEmergencyContactResponse>> getElderEmergencyContacts(String elderPhone) {
        // 通过手机号找到老人档案
        var elderProfile = elderProfileRepository.findByPhone(elderPhone)
                .orElseThrow(() -> new ApiException(4001, "elder profile not found"));

        // 查询该老人的紧急联系人列表，按priority升序排列
        List<EmergencyContact> contacts = emergencyContactRepository
                .findByElderProfileIdOrderByPriorityAsc(elderProfile.getId());

        // 转换为老人端Response对象
        List<ElderEmergencyContactResponse> responses = contacts.stream()
                .map(contact -> new ElderEmergencyContactResponse(
                        contact.getId(),
                        contact.getName(),
                        contact.getPhone(),
                        contact.getRelation(),
                        contact.getPriority(),
                        contact.getPriority() == 1  // isPrimary: priority == 1
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(responses);
    }

    /**
     * 与 {@link #getElderEmergencyContacts(String)} 相同，但由登录老人 JWT 解析手机号，避免仅凭 query 误查他人数据。
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<ElderEmergencyContactResponse>> getElderEmergencyContactsForElderUser(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(4010, "unauthorized"));
        if (!"elder".equalsIgnoreCase(user.getRole())) {
            throw new ApiException(4030, "forbidden");
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new ApiException(400, "elder account phone missing");
        }
        return getElderEmergencyContacts(user.getPhone().trim());
    }

    /**
     * 老人端：新增紧急联系人
     * 同一老人下的手机号必须唯一（去重）
     * 如果isPrimary=true，则该联系人为priority=1，其他联系人顺延
     * 
     * @param request 新增请求
     * @return ApiResponse 包含新增后的完整联系人列表
     */
    @Transactional
    public ApiResponse<List<ElderEmergencyContactResponse>> addElderEmergencyContact(
            ElderAddEmergencyContactRequest request) {
        // 通过手机号找到老人档案
        var elderProfile = elderProfileRepository.findByPhone(request.getElderPhone())
                .orElseThrow(() -> new ApiException(4001, "elder profile not found"));

        Long elderId = elderProfile.getId();

        // 检查同一老人下是否已存在该手机号（去重）
        if (emergencyContactRepository.existsByElderProfileIdAndPhone(elderId, request.getPhone())) {
            throw new ApiException(409, "该老人下该手机号的紧急联系人已存在");
        }

        // 确定priority
        Integer priority;
        if (request.getIsPrimary() != null && request.getIsPrimary()) {
            // 如果isPrimary=true，则设为priority=1，其他联系人顺延
            List<EmergencyContact> existingContacts = emergencyContactRepository
                    .findByElderProfileIdOrderByPriorityAsc(elderId);

            // 将所有现有联系人的priority加1（下降一档）
            for (EmergencyContact contact : existingContacts) {
                contact.setPriority(contact.getPriority() + 1);
            }

            // 批量保存调整后的联系人
            emergencyContactRepository.saveAll(existingContacts);

            priority = 1;
        } else {
            priority = 2;
        }

        // 创建新的紧急联系人
        EmergencyContact contact = new EmergencyContact();
        contact.setElderProfileId(elderId);
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setRelation(request.getRelation());
        contact.setPriority(priority);

        // 保存到数据库
        emergencyContactRepository.save(contact);

        // 返回新增后的完整联系人列表
        List<EmergencyContact> updatedContacts = emergencyContactRepository
                .findByElderProfileIdOrderByPriorityAsc(elderId);

        List<ElderEmergencyContactResponse> responses = updatedContacts.stream()
                .map(c -> new ElderEmergencyContactResponse(
                        c.getId(),
                        c.getName(),
                        c.getPhone(),
                        c.getRelation(),
                        c.getPriority(),
                        c.getPriority() == 1  // isPrimary: priority == 1
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok("created", responses);
    }

    /**
     * 将EmergencyContact实体转换为Response对象
     */
    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getElderProfileId(),
                contact.getName(),
                contact.getPhone(),
                contact.getRelation(),
                contact.getPriority(),
                contact.getCreatedAt() != null ? contact.getCreatedAt().format(ISO_FORMATTER) : null,
                contact.getUpdatedAt() != null ? contact.getUpdatedAt().format(ISO_FORMATTER) : null
        );
    }
}


