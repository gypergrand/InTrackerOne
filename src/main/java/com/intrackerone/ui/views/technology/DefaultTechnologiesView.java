package com.intrackerone.ui.views.technology;

import com.intrackerone.domain.technology.TechnologyDto;
import com.intrackerone.domain.technology.TechnologyService;
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

@Route(value = "default-technologies", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class DefaultTechnologiesView extends VerticalLayout {

    private final TechnologyService technologyService;
    private final Grid<TechnologyDto> grid = new Grid<>(TechnologyDto.class);

    public DefaultTechnologiesView(TechnologyService technologyService) {
        this.technologyService = technologyService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(TechnologyDto::name).setHeader("Technology Name").setSortable(true);
        grid.addComponentColumn(this::createActionRow).setHeader("Actions").setWidth("100px");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Add Technology", VaadinIcon.PLUS.create(), e -> openDialog(null));
        Button refresh = new Button(VaadinIcon.REFRESH.create(), e -> refresh());
        refresh.getElement().setAttribute("title", "Refresh");
        return new HorizontalLayout(add, refresh);
    }

    private void openDialog(TechnologyDto existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing != null ? "Edit Technology" : "Add Technology");

        TextField nameField = new TextField("Name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();
        if (existing != null) {
            nameField.setValue(existing.name());
        }

        Button save = new Button("Save", e -> {
            if (nameField.getValue() == null || nameField.getValue().isBlank()) {
                Notification.show("Technology name is required", 3000, Notification.Position.MIDDLE);
                nameField.focus();
                return;
            }
            try {
                if (existing == null) {
                    technologyService.create(nameField.getValue());
                } else {
                    technologyService.update(existing.id(), nameField.getValue());
                }
                dialog.close();
                refresh();
                Notification.show(existing != null ? "Technology updated" : "Technology added");
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
            List<TechnologyDto> technologies = technologyService.findAll();
            grid.setItems(technologies);
        } catch (Exception ex) {
            Notification.show("Failed to load technologies: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private HorizontalLayout createActionRow(TechnologyDto technology) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(technology));
        edit.getElement().setAttribute("title", "Edit");

        Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Delete Technology");
            confirm.setText("Are you sure you want to delete \"" + technology.name() + "\"?");
            confirm.setCancelable(true);
            confirm.setConfirmText("Delete");
            confirm.setConfirmButtonTheme("error primary");
            confirm.addConfirmListener(ev -> {
                try {
                    technologyService.delete(technology.id());
                    refresh();
                    Notification.show("Technology deleted");
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
