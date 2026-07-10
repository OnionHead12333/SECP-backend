package com.smartelderly.service.emergency_contact;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartelderly.api.ApiException;
import com.smartelderly.api.emergency_contact.dto.AddEmergencyContactRequest;
import com.smartelderly.api.emergency_contact.dto.ElderAddEmergencyContactRequest;
import com.smartelderly.api.emergency_contact.dto.UpdateEmergencyContactRequest;
import com.smartelderly.domain.ElderProfile;
import com.smartelderly.domain.ElderProfileRepository;
import com.smartelderly.domain.User;
import com.smartelderly.domain.UserRepository;
import com.smartelderly.domain.emergency_contact.EmergencyContact;
import com.smartelderly.domain.emergency_contact.EmergencyContactRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Emergency contact service unit tests")
class EmergencyContactServiceTest {

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private ElderProfileRepository elderProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmergencyContactService service;

    @Test
    @DisplayName("List child-managed contacts maps ordered entities")
    void getEmergencyContacts_existingElder_returnsMappedResponses() {
        when(elderProfileRepository.findById(7L)).thenReturn(Optional.of(elderProfile(7L, "13900000000")));
        when(emergencyContactRepository.findByElderProfileIdOrderByPriorityAsc(7L))
                .thenReturn(List.of(contact(1L, 7L, "Alice", "13800000001", "daughter", 1)));

        var response = service.getEmergencyContacts(7L);

        assertEquals(0, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals(1L, response.getData().get(0).getId());
        assertEquals(7L, response.getData().get(0).getElderId());
        assertEquals("Alice", response.getData().get(0).getName());
        assertEquals("13800000001", response.getData().get(0).getPhone());
    }

    @Test
    @DisplayName("Add primary contact shifts existing priorities")
    void addEmergencyContact_priorityOne_shiftsExistingContactsAndSavesNewContact() {
        EmergencyContact existingPrimary = contact(1L, 7L, "Old primary", "13800000001", "son", 1);
        EmergencyContact existingSecond = contact(2L, 7L, "Second", "13800000002", "friend", 2);
        AddEmergencyContactRequest request = new AddEmergencyContactRequest("New primary", "13800000003", "daughter", 1);

        when(elderProfileRepository.findById(7L)).thenReturn(Optional.of(elderProfile(7L, "13900000000")));
        when(emergencyContactRepository.existsByElderProfileIdAndPhone(7L, "13800000003")).thenReturn(false);
        when(emergencyContactRepository.findByElderProfileIdOrderByPriorityAsc(7L))
                .thenReturn(List.of(existingPrimary, existingSecond));
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenAnswer(invocation -> {
            EmergencyContact saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
            saved.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
            return saved;
        });

        var response = service.addEmergencyContact(7L, request);

        assertEquals("success", response.getMessage());
        assertEquals(10L, response.getData().getId());
        assertEquals(1, response.getData().getPriority());
        assertEquals(2, existingPrimary.getPriority());
        assertEquals(3, existingSecond.getPriority());
        verify(emergencyContactRepository).saveAll(List.of(existingPrimary, existingSecond));
        verify(emergencyContactRepository).save(argThat(contact ->
                contact.getElderProfileId().equals(7L)
                        && "New primary".equals(contact.getName())
                        && "13800000003".equals(contact.getPhone())
                        && contact.getPriority().equals(1)));
    }

    @Test
    @DisplayName("Add contact rejects duplicate phone for same elder")
    void addEmergencyContact_duplicatePhone_throwsApiException() {
        AddEmergencyContactRequest request = new AddEmergencyContactRequest("Alice", "13800000001", "daughter", 1);
        when(elderProfileRepository.findById(7L)).thenReturn(Optional.of(elderProfile(7L, "13900000000")));
        when(emergencyContactRepository.existsByElderProfileIdAndPhone(7L, "13800000001")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.addEmergencyContact(7L, request));

        assertEquals(409, exception.getCode());
        verify(emergencyContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ensure child contact is idempotent")
    void ensureChildAsPriorityOneEmergencyContact_duplicatePhoneSkipsInsert() {
        when(emergencyContactRepository.existsByElderProfileIdAndPhone(7L, "13800000001")).thenReturn(true);

        service.ensureChildAsPriorityOneEmergencyContact(7L, "Alice", "13800000001", "daughter");

        verify(elderProfileRepository, never()).findById(any());
        verify(emergencyContactRepository, never()).save(any());
        verify(emergencyContactRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Ensure child contact defaults blank relation and inserts as primary")
    void ensureChildAsPriorityOneEmergencyContact_blankRelationDefaultsAndAddsPrimary() {
        EmergencyContact existing = contact(1L, 7L, "Bob", "13800000002", "son", 1);
        when(emergencyContactRepository.existsByElderProfileIdAndPhone(7L, "13800000001")).thenReturn(false);
        when(elderProfileRepository.findById(7L)).thenReturn(Optional.of(elderProfile(7L, "13900000000")));
        when(emergencyContactRepository.findByElderProfileIdOrderByPriorityAsc(7L)).thenReturn(List.of(existing));
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.ensureChildAsPriorityOneEmergencyContact(7L, "Alice", "13800000001", " ");

        assertEquals(2, existing.getPriority());
        ArgumentCaptor<EmergencyContact> captor = ArgumentCaptor.forClass(EmergencyContact.class);
        verify(emergencyContactRepository).save(captor.capture());
        assertEquals("Alice", captor.getValue().getName());
        assertEquals("13800000001", captor.getValue().getPhone());
        assertFalse(captor.getValue().getRelation().isBlank());
        assertEquals(1, captor.getValue().getPriority());
    }

    @Test
    @DisplayName("Update contact to lower priority shifts contacts in between")
    void updateEmergencyContact_priorityRaised_shiftsIntermediateContacts() {
        EmergencyContact current = contact(3L, 7L, "Third", "13800000003", "friend", 3);
        EmergencyContact first = contact(1L, 7L, "First", "13800000001", "son", 1);
        EmergencyContact second = contact(2L, 7L, "Second", "13800000002", "daughter", 2);
        UpdateEmergencyContactRequest request = new UpdateEmergencyContactRequest("Updated", null, "neighbor", 1);

        when(elderProfileRepository.findById(7L)).thenReturn(Optional.of(elderProfile(7L, "13900000000")));
        when(emergencyContactRepository.findById(3L)).thenReturn(Optional.of(current));
        when(emergencyContactRepository.findByElderProfileIdOrderByPriorityAsc(7L))
                .thenReturn(List.of(first, second, current));
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateEmergencyContact(7L, 3L, request);

        assertEquals(1, response.getData().getPriority());
        assertEquals("Updated", response.getData().getName());
        assertEquals("neighbor", response.getData().getRelation());
        assertEquals(2, first.getPriority());
        assertEquals(3, second.getPriority());
        verify(emergencyContactRepository).saveAll(List.of(first, second, current));
    }

    @Test
    @DisplayName("Update contact rejects moving contact across elders")
    void updateEmergencyContact_contactBelongsToAnotherElder_throwsApiException() {
        EmergencyContact contact = contact(3L, 99L, "Other", "13800000003", "friend", 1);
        when(elderProfileRepository.findById(7L)).thenReturn(Optional.of(elderProfile(7L, "13900000000")));
        when(emergencyContactRepository.findById(3L)).thenReturn(Optional.of(contact));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.updateEmergencyContact(7L, 3L, new UpdateEmergencyContactRequest()));

        assertEquals(4042, exception.getCode());
        verify(emergencyContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete contact reorders remaining contacts by newest created time")
    void deleteEmergencyContact_existingContact_reordersRemainingContacts() {
        EmergencyContact deleted = contact(2L, 7L, "Delete me", "13800000002", "friend", 2);
        EmergencyContact older = contact(1L, 7L, "Older", "13800000001", "son", 1);
        older.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        EmergencyContact newer = contact(3L, 7L, "Newer", "13800000003", "daughter", 3);
        newer.setCreatedAt(LocalDateTime.of(2026, 1, 2, 8, 0));

        when(elderProfileRepository.findById(7L)).thenReturn(Optional.of(elderProfile(7L, "13900000000")));
        when(emergencyContactRepository.findById(2L)).thenReturn(Optional.of(deleted));
        when(emergencyContactRepository.findByElderProfileIdOrderByPriorityAsc(7L))
                .thenReturn(List.of(older, newer));

        var response = service.deleteEmergencyContact(7L, 2L);

        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals(2, older.getPriority());
        assertEquals(1, newer.getPriority());
        verify(emergencyContactRepository).deleteById(2L);
        verify(emergencyContactRepository).saveAll(List.of(newer, older));
    }

    @Test
    @DisplayName("Elder self lookup uses JWT user phone")
    void getElderEmergencyContactsForElderUser_validElder_returnsContacts() {
        User elderUser = user(20L, "elder", " 13900000000 ");
        EmergencyContact contact = contact(1L, 7L, "Alice", "13800000001", "daughter", 1);

        when(userRepository.findById(20L)).thenReturn(Optional.of(elderUser));
        when(elderProfileRepository.findByPhone("13900000000")).thenReturn(Optional.of(elderProfile(7L, "13900000000")));
        when(emergencyContactRepository.findByElderProfileIdOrderByPriorityAsc(7L)).thenReturn(List.of(contact));

        var response = service.getElderEmergencyContactsForElderUser(20L);

        assertEquals(1, response.getData().size());
        assertEquals(1L, response.getData().get(0).getContactId());
        assertTrue(response.getData().get(0).getIsPrimary());
    }

    @Test
    @DisplayName("Elder self lookup rejects child role")
    void getElderEmergencyContactsForElderUser_childRoleThrowsForbidden() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(user(20L, "child", "13800000001")));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.getElderEmergencyContactsForElderUser(20L));

        assertEquals(4030, exception.getCode());
        verifyNoInteractions(elderProfileRepository);
    }

    @Test
    @DisplayName("Elder add primary contact returns updated list")
    void addElderEmergencyContact_primaryContactShiftsAndReturnsUpdatedList() {
        ElderProfile elderProfile = elderProfile(7L, "13900000000");
        EmergencyContact existing = contact(1L, 7L, "Bob", "13800000002", "son", 1);
        EmergencyContact saved = contact(2L, 7L, "Alice", "13800000001", "daughter", 1);
        ElderAddEmergencyContactRequest request = new ElderAddEmergencyContactRequest(
                "13900000000", "Alice", "13800000001", "daughter", true);

        when(elderProfileRepository.findByPhone("13900000000")).thenReturn(Optional.of(elderProfile));
        when(emergencyContactRepository.existsByElderProfileIdAndPhone(7L, "13800000001")).thenReturn(false);
        when(emergencyContactRepository.findByElderProfileIdOrderByPriorityAsc(7L))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(saved, existing));

        var response = service.addElderEmergencyContact(request);

        assertEquals("created", response.getMessage());
        assertEquals(2, existing.getPriority());
        assertEquals(2, response.getData().size());
        assertEquals(2L, response.getData().get(0).getContactId());
        assertTrue(response.getData().get(0).getIsPrimary());

        ArgumentCaptor<EmergencyContact> captor = ArgumentCaptor.forClass(EmergencyContact.class);
        verify(emergencyContactRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getElderProfileId());
        assertEquals("Alice", captor.getValue().getName());
        assertEquals(1, captor.getValue().getPriority());
    }

    private static EmergencyContact contact(
            Long id, Long elderProfileId, String name, String phone, String relation, Integer priority) {
        EmergencyContact contact = new EmergencyContact();
        contact.setId(id);
        contact.setElderProfileId(elderProfileId);
        contact.setName(name);
        contact.setPhone(phone);
        contact.setRelation(relation);
        contact.setPriority(priority);
        contact.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0).plusDays(id == null ? 0 : id));
        contact.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 8, 0).plusDays(id == null ? 0 : id));
        return contact;
    }

    private static ElderProfile elderProfile(Long id, String phone) {
        ElderProfile profile = new ElderProfile();
        profile.setId(id);
        profile.setName("Elder");
        profile.setPhone(phone);
        return profile;
    }

    private static User user(Long id, String role, String phone) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setPhone(phone);
        return user;
    }
}
