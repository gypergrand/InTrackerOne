package com.intrackerone.ui.views.pipeline;

import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.job.JobDto;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.status.PipelineStatusDto;
import com.intrackerone.domain.status.PipelineStatusForm;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.status.StatusType;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "pipeline", layout = MainLayout.class)
@PermitAll
public class PipelineView extends VerticalLayout {

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

        add(createToolbar(statuses, current));

        HorizontalLayout columnsRow = new HorizontalLayout();
        columnsRow.setSpacing(false);

        for (int i = 0; i < statuses.size(); i++) {
            PipelineStatusDto status = statuses.get(i);
            VerticalLayout column = new VerticalLayout();
            column.setWidth("200px");
            column.setMinWidth("180px");
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
            headRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            headRow.setWidthFull();
            column.add(headRow);

            HorizontalLayout actionRow = new HorizontalLayout();
            actionRow.setWidthFull();
            actionRow.getStyle().set("margin-top", "2px");

            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openStatusDialog(status, current));
            editBtn.getElement().setAttribute("title", "Edit status");
            editBtn.getStyle().set("font-size", "9px");
            actionRow.add(editBtn);

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> openDeleteConfirm(status, current));
            deleteBtn.getElement().setAttribute("title", "Delete status");
            deleteBtn.getStyle().set("font-size", "9px");
            actionRow.add(deleteBtn);

            if (i > 0) {
                Button moveLeft = new Button(VaadinIcon.ARROW_LEFT.create(), e -> {
                    pipelineStatusService.move(status.id(), status.sortOrder() - 1, current.getId(), current.getRole());
                    renderPipeline();
                });
                moveLeft.getElement().setAttribute("title", "Move left");
                moveLeft.getStyle().set("font-size", "9px");
                actionRow.add(moveLeft);
            }
            if (i < statuses.size() - 1) {
                Button moveRight = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> {
                    pipelineStatusService.move(status.id(), status.sortOrder() + 1, current.getId(), current.getRole());
                    renderPipeline();
                });
                moveRight.getElement().setAttribute("title", "Move right");
                moveRight.getStyle().set("font-size", "9px");
                actionRow.add(moveRight);
            }
            column.add(actionRow);

            List<JobDto> columnJobs = jobsByStatus.getOrDefault(status.id(), List.of());
            for (JobDto job : columnJobs) {
                column.add(createJobCard(job, statuses, current));
            }

            columnsRow.add(column);
        }

        add(columnsRow);
    }

    private HorizontalLayout createToolbar(List<PipelineStatusDto> statuses, User current) {
        Button addBtn = new Button("Add Status", VaadinIcon.PLUS.create(), e -> openStatusDialog(null, current));
        Button refreshBtn = new Button(VaadinIcon.REFRESH.create(), e -> renderPipeline());
        refreshBtn.getElement().setAttribute("title", "Refresh");
        HorizontalLayout toolbar = new HorizontalLayout(addBtn, refreshBtn);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        return toolbar;
    }

    private void openStatusDialog(PipelineStatusDto existing, User current) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing != null ? "Edit Status" : "Add Status");

        TextField nameField = new TextField("Name");
        nameField.setRequiredIndicatorVisible(true);
        TextField colorField = new TextField("Color");
        colorField.setRequiredIndicatorVisible(true);
        colorField.setPlaceholder("#2196F3");
        ComboBox<StatusType> typeField = new ComboBox<>("Type");
        typeField.setItems(StatusType.values());
        typeField.setRequiredIndicatorVisible(true);

        Span colorPreview = new Span("█");
        colorPreview.getStyle().set("font-size", "20px");

        if (existing != null) {
            nameField.setValue(existing.name());
            colorField.setValue(existing.color());
            typeField.setValue(existing.type());
            colorPreview.getStyle().set("color", existing.color());
        }

        colorField.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().matches("^#[0-9A-Fa-f]{6}$")) {
                colorPreview.getStyle().set("color", e.getValue());
            }
        });

        Button save = new Button("Save", e -> {
            if (nameField.getValue() == null || nameField.getValue().isBlank()) {
                Notification.show("Name is required", 3000, Notification.Position.MIDDLE);
                nameField.focus();
                return;
            }
            if (colorField.getValue() == null || colorField.getValue().isBlank()) {
                Notification.show("Color is required", 3000, Notification.Position.MIDDLE);
                colorField.focus();
                return;
            }
            if (!colorField.getValue().matches("^#[0-9A-Fa-f]{6}$")) {
                Notification.show("Color must be a hex value like #2196F3", 3000, Notification.Position.MIDDLE);
                colorField.focus();
                return;
            }
            if (typeField.getValue() == null) {
                Notification.show("Type is required", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                PipelineStatusForm form = new PipelineStatusForm();
                form.setName(nameField.getValue());
                form.setColor(colorField.getValue());
                form.setType(typeField.getValue());
                if (existing == null) {
                    pipelineStatusService.create(form, current.getId());
                } else {
                    pipelineStatusService.update(existing.id(), form, current.getId(), current.getRole());
                }
                dialog.close();
                renderPipeline();
                Notification.show("Status saved");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        HorizontalLayout colorRow = new HorizontalLayout(colorField, colorPreview);
        colorRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        dialog.add(new FormLayout(nameField, colorRow, typeField), new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void openDeleteConfirm(PipelineStatusDto status, User current) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Status");
        dialog.setText("Are you sure you want to delete \"" + status.name() + "\"?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            try {
                pipelineStatusService.delete(status.id(), current.getId(), current.getRole());
                renderPipeline();
                Notification.show("Status deleted");
            } catch (Exception ex) {
                Notification.show("Delete failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        dialog.open();
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

        List<PipelineStatusDto> available = statuses.stream()
            .filter(s -> !s.id().equals(job.currentStatusId()))
            .toList();
        ComboBox<PipelineStatusDto> statusCombo = new ComboBox<>();
        statusCombo.setPlaceholder("Move to...");
        statusCombo.setItemLabelGenerator(PipelineStatusDto::name);
        statusCombo.setItems(available);
        statusCombo.getStyle().set("font-size", "12px");
        statusCombo.setWidthFull();
        statusCombo.addValueChangeListener(e -> {
            if (e.getValue() == null) return;
            statusChangeService.recordChange(job.id(), e.getValue().id(), null, null, current.getId(), current.getRole());
            renderPipeline();
            Notification.show("Moved to " + e.getValue().name());
        });
        card.add(statusCombo);

        return card;
    }
}
