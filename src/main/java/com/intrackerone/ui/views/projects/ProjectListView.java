package com.intrackerone.ui.views.projects;

import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyService;
import com.intrackerone.domain.project.ProjectDto;
import com.intrackerone.domain.project.ProjectForm;
import com.intrackerone.domain.project.ProjectService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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

import java.util.List;

@Route(value = "projects", layout = MainLayout.class)
@PermitAll
public class ProjectListView extends VerticalLayout {

    private final ProjectService projectService;
    private final CompanyService companyService;
    private final SecurityService securityService;
    private final Grid<ProjectDto> grid = new Grid<>(ProjectDto.class);
    private ComboBox<CompanyDto> companyFilter;
    private List<CompanyDto> companies;

    public ProjectListView(ProjectService projectService, CompanyService companyService,
                           SecurityService securityService) {
        this.projectService = projectService;
        this.companyService = companyService;
        this.securityService = securityService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        loadCompanies();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(ProjectDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(ProjectDto::companyName).setHeader("Company");
        grid.addColumn(p -> p.description() != null && p.description().length() > 100
            ? p.description().substring(0, 100) + "..." : p.description()).setHeader("Description");
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) openEditDialog(e.getValue());
        });
    }

    private HorizontalLayout createToolbar() {
        User current = securityService.getCurrentUser();
        companies = companyService.findAll(current.getId(), current.getRole());

        companyFilter = new ComboBox<>("Company");
        companyFilter.setItems(companies);
        companyFilter.setItemLabelGenerator(CompanyDto::name);
        companyFilter.setPlaceholder("Select a company");
        companyFilter.addValueChangeListener(e -> refreshGrid());

        Button add = new Button("Add Project", e -> openEditDialog(null));
        return new HorizontalLayout(companyFilter, add);
    }

    private void loadCompanies() {
        if (!companies.isEmpty()) {
            companyFilter.setValue(companies.get(0));
        }
    }

    private void openEditDialog(ProjectDto existing) {
        Dialog dialog = new Dialog();
        ProjectForm form = new ProjectForm();

        User current = securityService.getCurrentUser();

        Binder<ProjectForm> binder = new Binder<>(ProjectForm.class);
        binder.setBean(form);

        TextField nameField = new TextField("Name");
        TextArea descField = new TextArea("Description");

        ComboBox<CompanyDto> companyField = new ComboBox<>("Company");
        companyField.setItems(companies);
        companyField.setItemLabelGenerator(CompanyDto::name);

        if (companyFilter.getValue() != null) {
            companyField.setValue(companyFilter.getValue());
            form.setCompanyId(companyFilter.getValue().id());
        }

        binder.bind(nameField, "name");
        binder.bind(descField, "description");

        if (existing != null) {
            nameField.setValue(existing.name());
            descField.setValue(existing.description() != null ? existing.description() : "");
            companyField.setValue(companies.stream()
                .filter(c -> c.id().equals(existing.companyId())).findFirst().orElse(null));
            form.setCompanyId(existing.companyId());
        }

        companyField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                form.setCompanyId(e.getValue().id());
            }
        });

        Button save = new Button("Save", e -> {
            if (binder.validate().isOk() && form.getCompanyId() != null) {
                if (existing == null) {
                    projectService.create(form, current.getId(), current.getRole());
                } else {
                    projectService.update(existing.id(), form, current.getId(), current.getRole());
                }
                dialog.close();
                refreshGrid();
                Notification.show("Saved");
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(nameField, descField, companyField), new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refreshGrid() {
        User current = securityService.getCurrentUser();
        if (companyFilter.getValue() != null) {
            grid.setItems(projectService.findByCompanyId(
                companyFilter.getValue().id(), current.getId(), current.getRole()));
        }
    }
}
