package com.intrackerone.ui.views;

import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@PermitAll
public class DashboardView extends VerticalLayout {

    private final JobService jobService;
    private final PipelineStatusService pipelineStatusService;
    private final StatusChangeService statusChangeService;
    private final SecurityService securityService;

    public DashboardView(JobService jobService, PipelineStatusService pipelineStatusService,
                         StatusChangeService statusChangeService, SecurityService securityService) {
        this.jobService = jobService;
        this.pipelineStatusService = pipelineStatusService;
        this.statusChangeService = statusChangeService;
        this.securityService = securityService;
        setSizeFull();
        add(new H2("Dashboard"));
        add(createSummaryCards());
        add(createRecentActivity());
    }

    private HorizontalLayout createSummaryCards() {
        User current = securityService.getCurrentUser();
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();

        long totalJobs = jobService.countByOwnerId(current.getId());
        cards.add(createCard("Total Jobs", String.valueOf(totalJobs)));

        var statuses = pipelineStatusService.findByOwnerId(current.getId());
        for (var status : statuses) {
            long count = jobService.countByOwnerIdAndStatusId(current.getId(), status.id());
            Span card = createCard(status.name(), String.valueOf(count));
            card.getStyle().set("border-left", "4px solid " + status.color());
            cards.add(card);
        }

        return cards;
    }

    private Span createCard(String title, String value) {
        Span card = new Span();
        card.getStyle().set("padding", "16px");
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("min-width", "120px");

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-size", "12px");
        titleSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "24px");
        valueSpan.getStyle().set("font-weight", "bold");

        card.add(titleSpan, new Span(" "), valueSpan);
        return card;
    }

    private VerticalLayout createRecentActivity() {
        User current = securityService.getCurrentUser();
        VerticalLayout activity = new VerticalLayout();
        activity.add(new H2("Recent Activity"));

        var recent = statusChangeService.findRecentByOwnerId(current.getId(), 10);
        for (var change : recent) {
            String name = change.fromStatusName() != null ? change.fromStatusName() + " → " + change.toStatusName()
                : "→ " + change.toStatusName();
            Span entry = new Span(name + " — " + change.changedAt().toLocalDate());
            activity.add(entry);
        }
        return activity;
    }
}
