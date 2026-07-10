package com.smartelderly.api.device;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.ApiResponse;
import com.smartelderly.api.device.dto.DeviceStatusResponse;
import com.smartelderly.domain.UserRole;
import com.smartelderly.security.SecurityUtils;
import com.smartelderly.service.DeviceService;

@RestController
@RequestMapping("/v1/child/elders")
public class ChildDeviceController {

    private final DeviceService deviceService;

    public ChildDeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/{elderId}/devices")
    public ApiResponse<List<DeviceStatusResponse>> listDevices(@PathVariable("elderId") long elderId) {
        var user = SecurityUtils.requireRole(UserRole.child);
        return ApiResponse.ok(deviceService.listForChildElder(user.userId(), elderId));
    }
}
