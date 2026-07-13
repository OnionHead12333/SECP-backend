package com.smartelderly.api.voice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.InspectionApiResponse;
import com.smartelderly.security.SecurityUtils;

@RestController
@RequestMapping("/voice")
public class VoiceCommandController {

    private final VoiceCommandService voiceCommandService;

    public VoiceCommandController(VoiceCommandService voiceCommandService) {
        this.voiceCommandService = voiceCommandService;
    }

    @PostMapping("/command")
    public InspectionApiResponse<VoiceCommandResponse> command(@RequestBody VoiceCommandRequest request) {
        var principal = SecurityUtils.requireMatchingUserId(request.userId());
        VoiceCommandRequest authenticatedRequest = new VoiceCommandRequest(
                request.command(),
                request.robotId(),
                principal.userId(),
                request.musicId(),
                request.musicName(),
                request.musicUrl(),
                request.danceMode());
        return InspectionApiResponse.ok(voiceCommandService.handle(authenticatedRequest));
    }
}
