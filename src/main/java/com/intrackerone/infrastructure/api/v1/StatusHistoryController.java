package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.history.StatusChangeDto;
import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/history")
@RequiredArgsConstructor
public class StatusHistoryController {

    private final StatusChangeService statusChangeService;
    private final SecurityService securityService;

    @GetMapping
    public List<StatusChangeDto> getHistory(@PathVariable Long jobId) {
        User current = securityService.getCurrentUser();
        return statusChangeService.findByJobId(jobId, current.getId(), current.getRole());
    }
}
