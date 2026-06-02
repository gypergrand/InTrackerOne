package com.intrackerone.ui.views.status;

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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "default-statuses", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class DefaultStatusView extends VerticalLayout {

    private final PipelineStatusService pipelineStatusService;
    private final SecurityService securityService;
    private final Grid<PipelineStatusDto> grid = new Grid<>(PipelineStatusDto.class);

    public DefaultStatusView(PipelineStatusService pipelineStatusService, SecurityService securityService) {
        this.pipelineStatusService = pipelineStatusService;
        this.securityService = securityService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(PipelineStatusDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(this::colorPreview).setHeader("Color").setWidth("120px");
        grid.addColumn(PipelineStatusDto::type).setHeader("Type");
        grid.addColumn(PipelineStatusDto::sortOrder).setHeader("Order").setSortable(true);
        grid.addComponentColumn(this::createActionRow).setHeader("Actions").setWidth("160px");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private String colorPreview(PipelineStatusDto status) {
        return status.color() + " " + status.name();
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Add Default Status", VaadinIcon.PLUS.create(), e -> openDialog(null));
        Button refresh = new Button(VaadinIcon.REFRESH.create(), e -> refresh());
        refresh.getElement().setAttribute("title", "Refresh");
        HorizontalLayout toolbar = new HorizontalLayout(add, refresh);
        toolbar.setPadding(true);
        return toolbar;
    }

    private void openDialog(PipelineStatusDto existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing != null ? "Edit Default Status" : "Add Default Status");

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
                    pipelineStatusService.createDefault(form);
                } else {
                    pipelineStatusService.updateDefault(existing.id(), form);
                }
                dialog.close();
                refresh();
                Notification.show("Default status saved");
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

    private void refresh() {
        try {
            List<PipelineStatusDto> statuses = pipelineStatusService.findDefaultStatuses();
            grid.setItems(statuses);
        } catch (Exception ex) {
            Notification.show("Failed to load default statuses: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private HorizontalLayout createActionRow(PipelineStatusDto status) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(status));
        edit.getElement().setAttribute("title", "Edit");

        Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Delete Default Status");
            confirm.setText("Are you sure you want to delete \"" + status.name() + "\"?");
            confirm.setCancelable(true);
            confirm.setConfirmText("Delete");
            confirm.setConfirmButtonTheme("error primary");
            confirm.addConfirmListener(ev -> {
                try {
                    pipelineStatusService.deleteDefault(status.id());
                    refresh();
                    Notification.show("Default status deleted");
                } catch (Exception ex) {
                    Notification.show("Delete failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });
            confirm.open();
        });
        delete.getElement().setAttribute("title", "Delete");

        Button moveUp = new Button(VaadinIcon.ARROW_UP.create(), e -> {
            pipelineStatusService.moveDefault(status.id(), status.sortOrder() - 1);
            refresh();
        });
        moveUp.getElement().setAttribute("title", "Move up");

        Button moveDown = new Button(VaadinIcon.ARROW_DOWN.create(), e -> {
            pipelineStatusService.moveDefault(status.id(), status.sortOrder() + 1);
            refresh();
        });
        moveDown.getElement().setAttribute("title", "Move down");

        HorizontalLayout actions = new HorizontalLayout(edit, delete, moveUp, moveDown);
        actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        actions.setPadding(false);
        actions.setSpacing(true);
        return actions;
    }
}
