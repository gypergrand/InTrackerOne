package com.intrackerone.ui.views.projects;

import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyService;
import com.intrackerone.domain.project.IndustryService;
import com.intrackerone.domain.project.ProjectDto;
import com.intrackerone.domain.project.ProjectForm;
import com.intrackerone.domain.project.ProjectService;
import com.intrackerone.domain.technology.TechnologyService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserDto;
import com.intrackerone.domain.user.UserService;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Route(value = "projects", layout = MainLayout.class)
@PermitAll
public class ProjectListView extends VerticalLayout {

    private final ProjectService projectService;
    private final CompanyService companyService;
    private final IndustryService industryService;
    private final TechnologyService technologyService;
    private final UserService userService;
    private final SecurityService securityService;
    private final Grid<ProjectDto> grid = new Grid<>(ProjectDto.class);
    private ComboBox<CompanyDto> companyFilter;
    private final List<CompanyDto> companies = new ArrayList<>();

    public ProjectListView(ProjectService projectService, CompanyService companyService,
                           IndustryService industryService, TechnologyService technologyService,
                           UserService userService, SecurityService securityService) {
        this.projectService = projectService;
        this.companyService = companyService;
        this.industryService = industryService;
        this.technologyService = technologyService;
        this.userService = userService;
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
        grid.addComponentColumn(project -> {
            HorizontalLayout badges = new HorizontalLayout();
            badges.setSpacing(true);
            if (project.technologies() != null) {
                project.technologies().forEach(tech -> {
                    Span badge = new Span(tech);
                    badge.getStyle().set("background", "#bbdefb")
                                  .set("padding", "2px 6px")
                                  .set("border-radius", "4px")
                                  .set("font-size", "0.8em");
                    badges.add(badge);
                });
            }
            return badges;
        }).setHeader("Technologies");
        grid.addColumn(ProjectDto::ownerName).setHeader("Owner").setSortable(true);
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
        nameField.setRequiredIndicatorVisible(true);
        TextArea descField = new TextArea("Description");

        boolean canChangeOwner = current.getRole() == Role.ADMIN;
        final ComboBox<UserDto> ownerField;
        if (canChangeOwner) {
            ownerField = new ComboBox<>("Owner");
            ownerField.setItems(userService.findAll());
            ownerField.setItemLabelGenerator(u -> u.username() + " (" + u.email() + ")");
            if (existing != null && existing.ownerId() != null) {
                ownerField.setValue(userService.findAll().stream()
                    .filter(u -> u.id().equals(existing.ownerId())).findFirst().orElse(null));
            }
        } else {
            ownerField = null;
        }

        ComboBox<CompanyDto> companyField = new ComboBox<>("Company");
        companyField.setItems(companies);
        companyField.setItemLabelGenerator(CompanyDto::name);
        companyField.setRequiredIndicatorVisible(true);

        List<String> allIndustryNames = industryService.findAllNames();

        ComboBox<String> industryCombo = new ComboBox<>("Industries");
        industryCombo.setItems(allIndustryNames);
        industryCombo.setAllowCustomValue(true);
        industryCombo.setPlaceholder("Type or select to add...");
        industryCombo.setWidthFull();

        HorizontalLayout industryTags = new HorizontalLayout();
        industryTags.setSpacing(true);
        industryTags.setPadding(false);
        industryTags.setWidthFull();
        industryTags.getStyle().set("flex-wrap", "wrap");

        industryCombo.addCustomValueSetListener(e -> {
            String val = e.getDetail();
            if (val != null && !val.isBlank()) {
                form.getIndustries().add(val.trim());
                industryCombo.setItems(industryService.findAllNames());
                industryCombo.clear();
                renderIndustryTags(industryTags, form, industryService);
            }
        });

        industryCombo.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                form.getIndustries().add(e.getValue());
                industryCombo.setItems(industryService.findAllNames());
                industryCombo.clear();
                renderIndustryTags(industryTags, form, industryService);
            }
        });

        // Technologies combo + tags
        List<String> allTechNames = technologyService.findAllNames();

        ComboBox<String> techCombo = new ComboBox<>("Technologies");
        techCombo.setItems(allTechNames);
        techCombo.setAllowCustomValue(true);
        techCombo.setPlaceholder("Type or select to add...");
        techCombo.setWidthFull();

        HorizontalLayout techTags = new HorizontalLayout();
        techTags.setSpacing(true);
        techTags.setPadding(false);
        techTags.setWidthFull();
        techTags.getStyle().set("flex-wrap", "wrap");

        techCombo.addCustomValueSetListener(e -> {
            String val = e.getDetail();
            if (val != null && !val.isBlank()) {
                form.getTechnologies().add(val.trim());
                techCombo.setItems(technologyService.findAllNames());
                techCombo.clear();
                renderTechTags(techTags, form, technologyService);
            }
        });

        techCombo.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                form.getTechnologies().add(e.getValue());
                techCombo.setItems(technologyService.findAllNames());
                techCombo.clear();
                renderTechTags(techTags, form, technologyService);
            }
        });

        binder.bind(nameField, "name");
        binder.bind(descField, "description");

        if (existing != null) {
            nameField.setValue(existing.name());
            descField.setValue(existing.description() != null ? existing.description() : "");
            companyField.setValue(companies.stream()
                .filter(c -> c.id().equals(existing.companyId())).findFirst().orElse(null));
            form.setCompanyId(existing.companyId());
            if (existing.industries() != null) {
                form.setIndustries(new HashSet<>(existing.industries()));
                renderIndustryTags(industryTags, form, industryService);
            }
            if (existing.technologies() != null) {
                form.setTechnologies(new HashSet<>(existing.technologies()));
                renderTechTags(techTags, form, technologyService);
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
                if (nameField.getValue() == null || nameField.getValue().isBlank()) {
                    Notification.show("Project name is required", 3000, Notification.Position.MIDDLE);
                    nameField.focus();
                    return;
                }
                if (form.getCompanyId() == null) {
                    Notification.show("Company is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (existing == null) {
                    projectService.create(form, current.getId(), current.getRole());
                } else {
                    Long newOwnerId = canChangeOwner && ownerField.getValue() != null
                        ? ownerField.getValue().id() : null;
                    projectService.update(existing.id(), form, newOwnerId, current.getId(), current.getRole());
                }
                dialog.close();
                refreshGrid();
                Notification.show("Saved");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        if (existing != null) {
            Button delete = new Button("Delete", e -> {
                new ConfirmDialog("Confirm Delete", "Delete this project?",
                    "Delete", ev -> {
                        try {
                            projectService.delete(existing.id(), current.getId(), current.getRole());
                            dialog.close();
                            refreshGrid();
                            Notification.show("Deleted");
                        } catch (Exception ex) {
                            Notification.show("Delete failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        }
                    },
                    "Cancel", ev2 -> {}
                ).open();
            });
            delete.getStyle().set("color", "red");
            buttons.add(delete);
        }

        if (canChangeOwner && ownerField != null) {
            dialog.add(new FormLayout(nameField, descField, companyField, ownerField, industryCombo, techCombo),
                       industryTags, techTags,
                       buttons);
        } else {
            dialog.add(new FormLayout(nameField, descField, companyField, industryCombo, techCombo),
                       industryTags, techTags,
                       buttons);
        }
        dialog.open();
    }

    private void renderIndustryTags(HorizontalLayout container, ProjectForm form, IndustryService svc) {
        container.removeAll();
        for (String ind : form.getIndustries()) {
            Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create(), ev -> {
                form.getIndustries().remove(ind);
                renderIndustryTags(container, form, svc);
            });
            removeBtn.getStyle().set("padding", "0").set("margin", "0 0 0 4px");
            removeBtn.setHeight("16px");
            removeBtn.setWidth("16px");
            HorizontalLayout tagLayout = new HorizontalLayout(new Span(ind), removeBtn);
            tagLayout.setPadding(false);
            tagLayout.setSpacing(false);
            tagLayout.getStyle().set("background", "#e0e0e0")
                .set("padding", "2px 6px")
                .set("border-radius", "4px")
                .set("font-size", "0.8em")
                .set("align-items", "center");
            container.add(tagLayout);
        }
    }

    private void renderTechTags(HorizontalLayout container, ProjectForm form, TechnologyService svc) {
        container.removeAll();
        for (String tech : form.getTechnologies()) {
            Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create(), ev -> {
                form.getTechnologies().remove(tech);
                renderTechTags(container, form, svc);
            });
            removeBtn.getStyle().set("padding", "0").set("margin", "0 0 0 4px");
            removeBtn.setHeight("16px");
            removeBtn.setWidth("16px");
            HorizontalLayout tagLayout = new HorizontalLayout(new Span(tech), removeBtn);
            tagLayout.setPadding(false);
            tagLayout.setSpacing(false);
            tagLayout.getStyle().set("background", "#bbdefb")
                .set("padding", "2px 6px")
                .set("border-radius", "4px")
                .set("font-size", "0.8em")
                .set("align-items", "center");
            container.add(tagLayout);
        }
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
