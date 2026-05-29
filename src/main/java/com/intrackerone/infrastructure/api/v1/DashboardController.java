package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final JobService jobService;
    private final StatusChangeService statusChangeService;
    private final PipelineStatusService pipelineStatusService;
    private final SecurityService securityService;

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        User current = securityService.getCurrentUser();
        var statuses = pipelineStatusService.findByOwnerId(current.getId());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;
        for (var status : statuses) {
            long count = jobService.countByOwnerIdAndStatusId(current.getId(), status.id());
            byStatus.put(status.name(), count);
            total += count;
        }

        return Map.of(
            "totalJobs", total,
            "byStatus", byStatus
        );
    }

    @GetMapping("/recent")
    public Object recent() {
        User current = securityService.getCurrentUser();
        return statusChangeService.findRecentByOwnerId(current.getId(), 20);
    }
}
