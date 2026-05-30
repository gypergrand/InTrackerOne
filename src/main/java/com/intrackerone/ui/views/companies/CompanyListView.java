package com.intrackerone.ui.views.companies;

import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyForm;
import com.intrackerone.domain.company.CompanyService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "companies", layout = MainLayout.class)
@PermitAll
public class CompanyListView extends VerticalLayout {

    private final CompanyService companyService;
    private final SecurityService securityService;
    private final Grid<CompanyDto> grid = new Grid<>(CompanyDto.class);

    public CompanyListView(CompanyService companyService, SecurityService securityService) {
        this.companyService = companyService;
        this.securityService = securityService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(CompanyDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(CompanyDto::website).setHeader("Website");
        grid.addColumn(c -> c.notes() != null && c.notes().length() > 100
            ? c.notes().substring(0, 100) + "..." : c.notes()).setHeader("Notes");
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) openEditDialog(e.getValue());
        });
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Add Company", e -> openEditDialog(null));
        return new HorizontalLayout(add);
    }

    private void openEditDialog(CompanyDto existing) {
        Dialog dialog = new Dialog();
        CompanyForm form = new CompanyForm();
        if (existing != null) {
            form.setName(existing.name());
            form.setWebsite(existing.website());
            form.setNotes(existing.notes());
        }

        Binder<CompanyForm> binder = new Binder<>(CompanyForm.class);
        binder.setBean(form);

        TextField nameField = new TextField("Name");
        TextField websiteField = new TextField("Website");
        TextArea notesField = new TextArea("Notes");
        binder.bind(nameField, "name");
        binder.bind(websiteField, "website");
        binder.bind(notesField, "notes");

        Button save = new Button("Save", e -> {
            try {
                if (binder.validate().isOk()) {
                    User current = securityService.getCurrentUser();
                    if (existing == null) {
                        companyService.create(form, current.getId());
                    } else {
                        companyService.update(existing.id(), form, current.getId(), current.getRole());
                    }
                    dialog.close();
                    refresh();
                    Notification.show("Saved");
                }
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(nameField, websiteField, notesField), new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refresh() {
        User current = securityService.getCurrentUser();
        try {
            grid.setItems(companyService.findAll(current.getId(), current.getRole()));
        } catch (Exception ex) {
            Notification.show("Failed to load companies: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}
