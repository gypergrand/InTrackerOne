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
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Route(value = "projects", layout = MainLayout.class)
@PermitAll
public class ProjectListView extends VerticalLayout {

    private final ProjectService projectService;
    private final CompanyService companyService;
    private final SecurityService securityService;
    private final Grid<ProjectDto> grid = new Grid<>(ProjectDto.class);
    private ComboBox<CompanyDto> companyFilter;
    private final List<CompanyDto> companies = new ArrayList<>();

    private static final Set<String> INDUSTRY_SUGGESTIONS = new HashSet<>(Arrays.asList(
        "Software Development", "Healthcare", "Fintech", "Logistics",
        "SaaS", "Retail", "Engineering", "AI-native", "Cybersecurity",
        "Education", "Telecommunications", "Robotics", "Consulting",
        "Energy", "Gaming", "Sales", "Marketing", "HR Services",
        "R&D", "Hardware", "Platforms", "Infrastructure",
        "Systems Integrations", "Media", "Simulation", "Ecommerce",
        "Space", "Smart Cities", "Business Services", "Biotechnology", "Cloud"
    ));

    public ProjectListView(ProjectService projectService, CompanyService companyService,
                            SecurityService securityService) {
        this.projectService = projectService;
        this.companyService = companyService;
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
        grid.addColumn(ProjectDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(ProjectDto::companyName).setHeader("Company");
        grid.addColumn(p -> p.description() != null && p.description().length() > 100
            ? p.description().substring(0, 100) + "..." : p.description()).setHeader("Description");
        grid.addComponentColumn(project -> {
            HorizontalLayout badges = new HorizontalLayout();
            badges.setSpacing(true);
            if (project.industries() != null) {
                project.industries().forEach(ind -> {
                    Span badge = new Span(ind);
                    badge.getStyle().set("background", "#e0e0e0")
                                  .set("padding", "2px 6px")
                                  .set("border-radius", "4px")
                                  .set("font-size", "0.8em");
                    badges.add(badge);
                });
            }
            return badges;
        }).setHeader("Industries");
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

        Button add = new Button("Add Project", e -> openEditDialog(null));
        return new HorizontalLayout(companyFilter, add);
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

        // Industries: checkbox group for existing + text field for new ones
        CheckboxGroup<String> industriesPicker = new CheckboxGroup<>();
        industriesPicker.setLabel("Industries");
        industriesPicker.setItemLabelGenerator(item -> item);
        industriesPicker.setItems(INDUSTRY_SUGGESTIONS);

        TextField newIndustryField = new TextField("Add new industry");
        newIndustryField.setPlaceholder("Type and press Enter to add");
        newIndustryField.addKeyDownListener(com.vaadin.flow.component.Key.ENTER, e -> {
            String val = newIndustryField.getValue();
            if (val != null && !val.isBlank()) {
                form.getIndustries().add(val.trim());
                industriesPicker.getDataProvider().refreshAll();
                // Also add to selectable items if not already there
                if (!INDUSTRY_SUGGESTIONS.contains(val.trim())) {
                    INDUSTRY_SUGGESTIONS.add(val.trim());
                    industriesPicker.setItems(INDUSTRY_SUGGESTIONS);
                }
                industriesPicker.select(val.trim());
                newIndustryField.clear();
            }
        });

        binder.bind(nameField, "name");
        binder.bind(descField, "description");
        binder.bind(industriesPicker, "industries");

        if (existing != null) {
            nameField.setValue(existing.name());
            descField.setValue(existing.description() != null ? existing.description() : "");
            companyField.setValue(companies.stream()
                .filter(c -> c.id().equals(existing.companyId())).findFirst().orElse(null));
            form.setCompanyId(existing.companyId());
            if (existing.industries() != null) {
                form.setIndustries(new java.util.HashSet<>(existing.industries()));
                // Ensure existing industry names are selectable
                existing.industries().forEach(ind -> {
                    if (!INDUSTRY_SUGGESTIONS.contains(ind)) {
                        INDUSTRY_SUGGESTIONS.add(ind);
                    }
                });
                industriesPicker.setItems(INDUSTRY_SUGGESTIONS);
            }
        } else if (companyFilter.getValue() != null) {
            companyField.setValue(companyFilter.getValue());
            form.setCompanyId(companyFilter.getValue().id());
        }

        companyField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                form.setCompanyId(e.getValue().id());
            }
        });

        Button save = new Button("Save", e -> {
            try {
                if (!binder.validate().isOk()) {
                    return;
                }
                if (form.getCompanyId() == null) {
                    Notification.show("Company is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (existing == null) {
                    projectService.create(form, current.getId(), current.getRole());
                } else {
                    projectService.update(existing.id(), form, current.getId(), current.getRole());
                }
                dialog.close();
                refreshGrid();
                Notification.show("Saved");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(nameField, descField, companyField, industriesPicker),
                   new HorizontalLayout(newIndustryField),
                   new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refreshGrid() {
        User current = securityService.getCurrentUser();
        try {
            if (companyFilter.getValue() != null) {
                grid.setItems(projectService.findByCompanyId(
                    companyFilter.getValue().id(), current.getId(), current.getRole()));
            } else {
                companies.clear();
                companies.addAll(companyService.findAll(current.getId(), current.getRole()));
                var allProjects = projectService.findByCompanyId(null, current.getId(), current.getRole());
                grid.setItems(allProjects);
            }
        } catch (Exception ex) {
            Notification.show("Failed to load projects: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}