package com.intrackerone.ui.views.industry;

import com.intrackerone.domain.project.IndustryDto;
import com.intrackerone.domain.project.IndustryService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "default-industries", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class DefaultIndustryView extends VerticalLayout {

    private final IndustryService industryService;
    private final Grid<IndustryDto> grid = new Grid<>(IndustryDto.class);

    public DefaultIndustryView(IndustryService industryService) {
        this.industryService = industryService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(IndustryDto::name).setHeader("Industry Name").setSortable(true);
        grid.addComponentColumn(this::createActionRow).setHeader("Actions").setWidth("100px");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Add Industry", VaadinIcon.PLUS.create(), e -> openDialog(null));
        Button refresh = new Button(VaadinIcon.REFRESH.create(), e -> refresh());
        refresh.getElement().setAttribute("title", "Refresh");
        HorizontalLayout toolbar = new HorizontalLayout(add, refresh);
        toolbar.setPadding(true);
        return toolbar;
    }

    private void openDialog(IndustryDto existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing != null ? "Edit Industry" : "Add Industry");

        TextField nameField = new TextField("Name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();
        if (existing != null) {
            nameField.setValue(existing.name());
        }

        Button save = new Button("Save", e -> {
            if (nameField.getValue() == null || nameField.getValue().isBlank()) {
                Notification.show("Industry name is required", 3000, Notification.Position.MIDDLE);
                nameField.focus();
                return;
            }
            try {
                if (existing == null) {
                    industryService.create(nameField.getValue());
                } else {
                    industryService.update(existing.id(), nameField.getValue());
                }
                dialog.close();
                refresh();
                Notification.show(existing != null ? "Industry updated" : "Industry added");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(nameField), new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refresh() {
        try {
            List<IndustryDto> industries = industryService.findAll();
            grid.setItems(industries);
        } catch (Exception ex) {
            Notification.show("Failed to load industries: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private HorizontalLayout createActionRow(IndustryDto industry) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(industry));
        edit.getElement().setAttribute("title", "Edit");

        Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Delete Industry");
            confirm.setText("Are you sure you want to delete \"" + industry.name() + "\"?");
            confirm.setCancelable(true);
            confirm.setConfirmText("Delete");
            confirm.setConfirmButtonTheme("error primary");
            confirm.addConfirmListener(ev -> {
                try {
                    industryService.delete(industry.id());
                    refresh();
                    Notification.show("Industry deleted");
                } catch (Exception ex) {
                    Notification.show("Delete failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });
            confirm.open();
        });
        delete.getElement().setAttribute("title", "Delete");

        HorizontalLayout actions = new HorizontalLayout(edit, delete);
        actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        actions.setPadding(false);
        actions.setSpacing(true);
        return actions;
    }
}
