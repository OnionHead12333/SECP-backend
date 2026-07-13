package com.smartelderly.api.voice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartelderly.api.inspection.InspectionApiResponse;

@RestController
@RequestMapping({"/voice", "/api/voice"})
public class VoiceCommandController {

    private final VoiceCommandService voiceCommandService;

    public VoiceCommandController(VoiceCommandService voiceCommandService) {
        this.voiceCommandService = voiceCommandService;
    }

    @PostMapping("/command")
    public InspectionApiResponse<VoiceCommandResponse> command(@RequestBody VoiceCommandRequest request) {
        return InspectionApiResponse.ok(voiceCommandService.handle(request));
    }
}
