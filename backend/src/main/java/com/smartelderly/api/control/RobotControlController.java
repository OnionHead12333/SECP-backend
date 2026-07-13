package com.smartelderly.api.control;

import com.smartelderly.api.inspection.InspectionApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/robot/control")
public class RobotControlController {

    private final RobotControlService robotControlService;

    public RobotControlController(RobotControlService robotControlService) {
        this.robotControlService = robotControlService;
    }

    @PostMapping("/command")
    public InspectionApiResponse<RobotControlCommandResult> sendCommand(
            @RequestBody RobotControlCommandRequest request) {
        return robotControlService.sendCommand(request);
    }

    @GetMapping("/state")
    public InspectionApiResponse<RobotControlStateResponse> getState() {
        return robotControlService.getState();
    }
}
