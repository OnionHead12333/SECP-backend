package com.smartelderly.api.device;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.device.dto.DeviceHeartbeatRequest;
import com.smartelderly.api.device.dto.DeviceSosRequest;
import com.smartelderly.api.device.dto.DeviceSosResponse;
import com.smartelderly.api.device.dto.DeviceStatusResponse;
import com.smartelderly.service.DeviceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/device")
@Validated
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/heartbeat")
    public ApiResponse<DeviceStatusResponse> heartbeat(
            @Valid @RequestBody DeviceHeartbeatRequest request) {
        return ApiResponse.ok("heartbeat accepted", deviceService.heartbeat(request));
    }

    @PostMapping("/sos")
    public ApiResponse<DeviceSosResponse> sos(
            @Valid @RequestBody DeviceSosRequest request) {
        return ApiResponse.ok("created", deviceService.createHardwareSos(request));
    }
}
