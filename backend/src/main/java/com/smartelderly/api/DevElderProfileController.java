package com.smartelderly.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.dto.DevClaimElderRequest;
import com.smartelderly.domain.ElderProfileRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/dev/elder-profiles")
@Profile("dev")
@Validated
public class DevElderProfileController {

    private final ElderProfileRepository elderProfileRepository;

    public DevElderProfileController(ElderProfileRepository elderProfileRepository) {
        this.elderProfileRepository = elderProfileRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> list() {
        List<Map<String, Object>> rows = elderProfileRepository.findAll().stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("claimedUserId", p.getClaimedUserId());
                    return m;
                })
                .toList();
        return ApiResponse.ok(Map.of("list", rows, "total", rows.size()));
    }

    @PostMapping("/claim")
    @Transactional
    public ApiResponse<Map<String, Object>> claim(@Valid @RequestBody DevClaimElderRequest req) {
        var p = elderProfileRepository.findById(req.elderProfileId())
                .orElseThrow(() -> new ApiException(4004, "elder profile not found"));
        p.setClaimedUserId(req.claimedUserId());
        elderProfileRepository.save(p);
        return ApiResponse.ok(Map.of(
                "elderProfileId", p.getId(),
                "name", p.getName(),
                "claimedUserId", p.getClaimedUserId()));
    }
}

