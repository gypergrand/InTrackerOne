package com.intrackerone.ui.views.pipeline;

import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.job.JobDto;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.status.PipelineStatusDto;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.status.StatusType;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "pipeline", layout = MainLayout.class)
@PermitAll
public class PipelineView extends HorizontalLayout {

    private final JobService jobService;
    private final PipelineStatusService pipelineStatusService;
    private final StatusChangeService statusChangeService;
    private final SecurityService securityService;

    public PipelineView(JobService jobService, PipelineStatusService pipelineStatusService,
                        StatusChangeService statusChangeService, SecurityService securityService) {
        this.jobService = jobService;
        this.pipelineStatusService = pipelineStatusService;
        this.statusChangeService = statusChangeService;
        this.securityService = securityService;
        setSizeFull();
        setSpacing(false);
        renderPipeline();
    }

    private void renderPipeline() {
        removeAll();
        User current = securityService.getCurrentUser();
        List<PipelineStatusDto> statuses = pipelineStatusService.findByOwnerId(current.getId());
        List<JobDto> jobs = jobService.findAll(current.getId(), current.getRole(), null, null, null);

        Map<Long, List<JobDto>> jobsByStatus = jobs.stream()
            .collect(Collectors.groupingBy(JobDto::currentStatusId));

        for (PipelineStatusDto status : statuses) {
            VerticalLayout column = new VerticalLayout();
            column.setWidth("280px");
            column.setMinWidth("280px");
            column.getStyle().set("background", "var(--lumo-contrast-5pct)");
            column.getStyle().set("border-radius", "8px");
            column.getStyle().set("padding", "8px");
            column.getStyle().set("margin", "4px");

            H4 header = new H4(status.name());
            header.getStyle().set("color", status.color());
            header.getStyle().set("margin", "0");

            Span count = new Span("(" + jobsByStatus.getOrDefault(status.id(), List.of()).size() + ")");
            count.getStyle().set("color", "var(--lumo-secondary-text-color)");

            HorizontalLayout headRow = new HorizontalLayout(header, count);
            headRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            column.add(headRow);

            List<JobDto> columnJobs = jobsByStatus.getOrDefault(status.id(), List.of());
            for (JobDto job : columnJobs) {
                column.add(createJobCard(job, statuses, current));
            }

            add(column);
        }
    }

    private VerticalLayout createJobCard(JobDto job, List<PipelineStatusDto> statuses, User current) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "4px");
        card.getStyle().set("padding", "8px");
        card.getStyle().set("margin", "4px 0");
        card.setSpacing(false);

        Span title = new Span(job.position());
        title.getStyle().set("font-weight", "bold");
        card.add(title);

        Span company = new Span(job.companyName());
        company.getStyle().set("font-size", "12px");
        company.getStyle().set("color", "var(--lumo-secondary-text-color)");
        card.add(company);

        for (PipelineStatusDto s : statuses) {
            if (s.type() == StatusType.TERMINAL) continue;
            if (s.id().equals(job.currentStatusId())) continue;
            Button moveBtn = new Button("→ " + s.name(), e -> {
                statusChangeService.recordChange(job.id(), s.id(), null, null, current.getId(), current.getRole());
                renderPipeline();
                Notification.show("Moved to " + s.name());
            });
            moveBtn.getStyle().set("font-size", "11px");
            moveBtn.getStyle().set("padding", "0 8px");
            moveBtn.getStyle().set("margin", "2px 0");
            card.add(moveBtn);
        }

        return card;
    }
}
