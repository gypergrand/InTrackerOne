package com.intrackerone.ui.views.recruiters;

import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyService;
import com.intrackerone.domain.project.ProjectDto;
import com.intrackerone.domain.project.ProjectService;
import com.intrackerone.domain.recruiter.RecruiterDto;
import com.intrackerone.domain.recruiter.RecruiterForm;
import com.intrackerone.domain.recruiter.RecruiterService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "recruiters", layout = MainLayout.class)
@PermitAll
public class RecruiterListView extends VerticalLayout {

    private final RecruiterService recruiterService;
    private final CompanyService companyService;
    private final ProjectService projectService;
    private final SecurityService securityService;
    private final Grid<RecruiterDto> grid = new Grid<>(RecruiterDto.class);
    private ComboBox<CompanyDto> companyFilter;
    private final List<CompanyDto> companies = new ArrayList<>();

    public RecruiterListView(RecruiterService recruiterService, CompanyService companyService,
                              ProjectService projectService, SecurityService securityService) {
        this.recruiterService = recruiterService;
        this.companyService = companyService;
        this.projectService = projectService;
        this.securityService = securityService;
        setSizeFull();
        configureGrid();
        add(createToolbar());
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(RecruiterDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(RecruiterDto::email).setHeader("Email");
        grid.addColumn(RecruiterDto::phone).setHeader("Phone");
        grid.addColumn(RecruiterDto::companyName).setHeader("Company");
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) openEditDialog(e.getValue());
        });
    }

    private HorizontalLayout createToolbar() {
        User current = securityService.getCurrentUser();
        companies.clear();
        companies.addAll(companyService.findAll(current.getId(), current.getRole()));

        companyFilter = new ComboBox<>("Company");
        companyFilter.setItems(companies);
        companyFilter.setItemLabelGenerator(CompanyDto::name);
        companyFilter.setPlaceholder("All companies");
        companyFilter.addValueChangeListener(e -> refreshGrid());

        Button add = new Button("Add Recruiter", e -> openEditDialog(null));
        return new HorizontalLayout(companyFilter, add);
    }

    private void openEditDialog(RecruiterDto existing) {
        Dialog dialog = new Dialog();
        RecruiterForm form = new RecruiterForm();

        User current = securityService.getCurrentUser();
        List<ProjectDto> availableProjects = new ArrayList<>();

        TextField nameField = new TextField("Name");
        nameField.setRequiredIndicatorVisible(true);
        TextField emailField = new TextField("Email");
        TextField phoneField = new TextField("Phone");
        TextArea notesField = new TextArea("Notes");

        ComboBox<CompanyDto> companyField = new ComboBox<>("Company");
        companyField.setItems(companies);
        companyField.setItemLabelGenerator(CompanyDto::name);
        companyField.setRequiredIndicatorVisible(true);

        CheckboxGroup<ProjectDto> projectsField = new CheckboxGroup<>("Projects");

        if (existing != null) {
            nameField.setValue(existing.name());
            emailField.setValue(existing.email() != null ? existing.email() : "");
            phoneField.setValue(existing.phone() != null ? existing.phone() : "");
            notesField.setValue(existing.notes() != null ? existing.notes() : "");
            companyField.setValue(companies.stream()
                .filter(c -> c.id().equals(existing.companyId())).findFirst().orElse(null));
            form.setCompanyId(existing.companyId());
            form.setProjectIds(existing.projectIds());

            availableProjects.addAll(projectService.findByCompanyId(existing.companyId(), current.getId(), current.getRole()));
            Set<ProjectDto> selected = availableProjects.stream()
                .filter(p -> existing.projectIds().contains(p.id()))
                .collect(Collectors.toSet());
            projectsField.setItems(availableProjects);
            projectsField.setValue(selected);
        } else if (companyFilter.getValue() != null) {
            companyField.setValue(companyFilter.getValue());
        }

        companyField.addValueChangeListener(e -> {
            projectsField.clear();
            projectsField.setItems();
            if (e.getValue() != null) {
                Long companyId = e.getValue().id();
                form.setCompanyId(companyId);
                availableProjects.clear();
                availableProjects.addAll(projectService.findByCompanyId(companyId, current.getId(), current.getRole()));
                projectsField.setItems(availableProjects);
            } else {
                form.setCompanyId(null);
            }
        });

        Button save = new Button("Save", e -> {
            try {
                if (nameField.isEmpty()) {
                    Notification.show("Name is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (companyField.isEmpty()) {
                    Notification.show("Company is required", 3000, Notification.Position.MIDDLE);
                    return;
                }

                form.setName(nameField.getValue());
                form.setEmail(emailField.getValue().isBlank() ? null : emailField.getValue());
                form.setPhone(phoneField.getValue().isBlank() ? null : phoneField.getValue());
                form.setNotes(notesField.getValue().isBlank() ? null : notesField.getValue());
                form.setCompanyId(companyField.getValue().id());
                form.setProjectIds(projectsField.getSelectedItems().stream()
                    .map(ProjectDto::id)
                    .collect(Collectors.toSet()));

                if (existing == null) {
                    recruiterService.create(form, current.getId(), current.getRole());
                } else {
                    recruiterService.update(existing.id(), form, current.getId(), current.getRole());
                }
                dialog.close();
                refreshGrid();
                Notification.show("Saved");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(nameField, emailField, phoneField, notesField, companyField, projectsField),
            new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refreshGrid() {
        User current = securityService.getCurrentUser();
        try {
            if (companyFilter.getValue() != null) {
                grid.setItems(recruiterService.findByCompanyId(
                    companyFilter.getValue().id(), current.getId(), current.getRole()));
            } else {
                companies.clear();
                companies.addAll(companyService.findAll(current.getId(), current.getRole()));
                grid.setItems(recruiterService.findAllAccessible(current.getId(), current.getRole()));
            }
        } catch (Exception ex) {
            Notification.show("Failed to load recruiters: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}