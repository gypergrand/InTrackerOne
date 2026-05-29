package com.intrackerone.ui.views.recruiters;

import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyService;
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

import java.util.List;
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

    public RecruiterListView(RecruiterService recruiterService, CompanyService companyService,
                             ProjectService projectService, SecurityService securityService) {
        this.recruiterService = recruiterService;
        this.companyService = companyService;
        this.projectService = projectService;
        this.securityService = securityService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        loadCompanies();
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
        var companies = companyService.findAll(current.getId(), current.getRole());

        companyFilter = new ComboBox<>("Company");
        companyFilter.setItems(companies);
        companyFilter.setItemLabelGenerator(CompanyDto::name);
        companyFilter.setPlaceholder("Select a company");
        companyFilter.addValueChangeListener(e -> refreshGrid());

        Button add = new Button("Add Recruiter", e -> openEditDialog(null));
        return new HorizontalLayout(companyFilter, add);
    }

    private void loadCompanies() {
        User current = securityService.getCurrentUser();
        var companies = companyService.findAll(current.getId(), current.getRole());
        if (!companies.isEmpty()) {
            companyFilter.setValue(companies.get(0));
        }
    }

    private void openEditDialog(RecruiterDto existing) {
        Dialog dialog = new Dialog();
        RecruiterForm form = new RecruiterForm();

        User current = securityService.getCurrentUser();
        var companies = companyService.findAll(current.getId(), current.getRole());

        TextField nameField = new TextField("Name");
        TextField emailField = new TextField("Email");
        TextField phoneField = new TextField("Phone");
        TextArea notesField = new TextArea("Notes");

        ComboBox<CompanyDto> companyField = new ComboBox<>("Company");
        companyField.setItems(companies);
        companyField.setItemLabelGenerator(CompanyDto::name);

        CheckboxGroup<Object> projectsField = new CheckboxGroup<>("Projects");

        companyField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                Long companyId = e.getValue().id();
                var projs = projectService.findByCompanyId(companyId, current.getId(), current.getRole());
                projectsField.setItems(projs);
//                projectsField.setItemLabelGenerator(p -> ((com.intrackerone.domain.project.ProjectDto) p).name());
                form.setCompanyId(companyId);
            } else {
                projectsField.setItems();
                form.setCompanyId(null);
            }
        });

        if (companyFilter.getValue() != null) {
            companyField.setValue(companyFilter.getValue());
        }

        if (existing != null) {
            nameField.setValue(existing.name());
            emailField.setValue(existing.email() != null ? existing.email() : "");
            phoneField.setValue(existing.phone() != null ? existing.phone() : "");
            notesField.setValue(existing.notes() != null ? existing.notes() : "");
            companyField.setValue(companies.stream()
                .filter(c -> c.id().equals(existing.companyId())).findFirst().orElse(null));
            if (existing.companyId() != null) {
                var projs = projectService.findByCompanyId(existing.companyId(), current.getId(), current.getRole());
                java.util.Set<Object> selected = projs.stream()
                    .filter(p -> existing.projectIds().contains(p.id()))
                    .collect(Collectors.toSet());
                projectsField.setValue(selected);
            }
            form.setCompanyId(existing.companyId());
            form.setProjectIds(existing.projectIds());
        }

        Button save = new Button("Save", e -> {
            form.setName(nameField.getValue());
            form.setEmail(emailField.getValue());
            form.setPhone(phoneField.getValue());
            form.setNotes(notesField.getValue());
            if (companyField.getValue() != null) {
                form.setCompanyId(companyField.getValue().id());
            }
            form.setProjectIds(projectsField.getSelectedItems().stream()
                .map(p -> ((com.intrackerone.domain.project.ProjectDto) ((List) p).get(0)).id())
                .collect(Collectors.toSet()));

            if (existing == null) {
                recruiterService.create(form, current.getId(), current.getRole());
            } else {
                recruiterService.update(existing.id(), form, current.getId(), current.getRole());
            }
            dialog.close();
            refreshGrid();
            Notification.show("Saved");
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(nameField, emailField, phoneField, notesField, companyField, projectsField),
            new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refreshGrid() {
        User current = securityService.getCurrentUser();
        if (companyFilter.getValue() != null) {
            grid.setItems(recruiterService.findByCompanyId(
                companyFilter.getValue().id(), current.getId(), current.getRole()));
        }
    }
}
