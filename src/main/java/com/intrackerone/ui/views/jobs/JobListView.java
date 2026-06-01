package com.intrackerone.ui.views.jobs;

import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyService;
import com.intrackerone.domain.job.JobDto;
import com.intrackerone.domain.job.JobForm;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.job.JobType;
import com.intrackerone.domain.job.Source;
import com.intrackerone.domain.job.WorkType;
import com.intrackerone.domain.project.ProjectDto;
import com.intrackerone.domain.project.ProjectService;
import com.intrackerone.domain.recruiter.RecruiterDto;
import com.intrackerone.domain.recruiter.RecruiterService;
import com.intrackerone.domain.status.PipelineStatusDto;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "jobs", layout = MainLayout.class)
@PermitAll
public class JobListView extends VerticalLayout {

    private final JobService jobService;
    private final CompanyService companyService;
    private final ProjectService projectService;
    private final RecruiterService recruiterService;
    private final PipelineStatusService pipelineStatusService;
    private final SecurityService securityService;
    private final Grid<JobDto> grid = new Grid<>(JobDto.class);

    public JobListView(JobService jobService, CompanyService companyService,
                       ProjectService projectService, RecruiterService recruiterService,
                       PipelineStatusService pipelineStatusService, SecurityService securityService) {
        this.jobService = jobService;
        this.companyService = companyService;
        this.projectService = projectService;
        this.recruiterService = recruiterService;
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
        grid.addColumn(JobDto::position).setHeader("Position").setSortable(true);
        grid.addColumn(JobDto::companyName).setHeader("Company");
        grid.addColumn(JobDto::projectName).setHeader("Project");
        grid.addColumn(new ComponentRenderer<Span, JobDto>(job -> {
            Span badge = new Span(job.currentStatusName());
            badge.getStyle().set("background", job.currentStatusColor());
            badge.getStyle().set("color", "white");
            badge.getStyle().set("padding", "2px 8px");
            badge.getStyle().set("border-radius", "4px");
            return badge;
        })).setHeader("Status");
        grid.addColumn(JobDto::jobType).setHeader("Employment Type");
        grid.addColumn(JobDto::workType).setHeader("Work Type");
        grid.addColumn(JobDto::updatedAt).setHeader("Updated");
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) openEditDialog(e.getValue());
        });
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Add Job", e -> openEditDialog(null));
        return new HorizontalLayout(add);
    }

    private void openEditDialog(JobDto existing) {
        Dialog dialog = new Dialog();
        JobForm form = new JobForm();

        User current = securityService.getCurrentUser();

        TextField positionField = new TextField("Position");
        positionField.setRequiredIndicatorVisible(true);

        TextArea descField = new TextArea("Description");
        TextField urlField = new TextField("Job URL");
        TextField locationField = new TextField("Location");
        TextField salaryField = new TextField("Salary Range");
        TextArea notesField = new TextArea("Notes");
        TextField sourceMetaField = new TextField("Source Meta");

        Select<JobType> typeField = new Select<>();
        typeField.setLabel("Employment Type");
        typeField.setItems(JobType.values());
        typeField.setRequiredIndicatorVisible(true);

        Select<WorkType> workTypeField = new Select<>();
        workTypeField.setLabel("Work Type");
        workTypeField.setItems(WorkType.values());
        workTypeField.setRequiredIndicatorVisible(true);

        Select<Source> sourceField = new Select<>();
        sourceField.setLabel("Source");
        sourceField.setItems(Source.values());
        sourceField.setRequiredIndicatorVisible(true);

        var companies = companyService.findAll(current.getId(), current.getRole());
        ComboBox<CompanyDto> companyField = new ComboBox<>("Company");
        companyField.setItems(companies);
        companyField.setItemLabelGenerator(CompanyDto::name);
        companyField.setRequiredIndicatorVisible(true);

        ComboBox<ProjectDto> projectField = new ComboBox<>("Project");
        projectField.setRequiredIndicatorVisible(true);
        projectField.setEnabled(false);
        projectField.setPlaceholder("Select a company first");

        ComboBox<RecruiterDto> recruiterField = new ComboBox<>("Recruiter");
        recruiterField.setItemLabelGenerator(RecruiterDto::name);

        var statuses = pipelineStatusService.findByOwnerId(current.getId());
        ComboBox<PipelineStatusDto> statusField = new ComboBox<>("Status");
        statusField.setItems(statuses);
        statusField.setItemLabelGenerator(PipelineStatusDto::name);
        statusField.setRequiredIndicatorVisible(true);

        companyField.addValueChangeListener(e -> {
            projectField.clear();
            recruiterField.clear();
            if (e.getValue() != null) {
                Long companyId = e.getValue().id();
                var projects = projectService.findByCompanyId(companyId, current.getId(), current.getRole());
                projectField.setItems(projects);
                projectField.setItemLabelGenerator(ProjectDto::name);
                projectField.setEnabled(true);
                projectField.setPlaceholder(null);

                var recruiters = recruiterService.findByCompanyId(companyId, current.getId(), current.getRole());
                recruiterField.setItems(recruiters);
            } else {
                projectField.setEnabled(false);
                projectField.setPlaceholder("Select a company first");
            }
        });

        if (existing != null) {
            positionField.setValue(existing.position());
            descField.setValue(existing.jobDescription() != null ? existing.jobDescription() : "");
            urlField.setValue(existing.jobUrl() != null ? existing.jobUrl() : "");
            locationField.setValue(existing.location() != null ? existing.location() : "");
            salaryField.setValue(existing.salaryRange() != null ? existing.salaryRange() : "");
            notesField.setValue(existing.notes() != null ? existing.notes() : "");
            sourceMetaField.setValue(existing.sourceMeta() != null ? existing.sourceMeta() : "");
            typeField.setValue(existing.jobType());
            workTypeField.setValue(existing.workType());
            sourceField.setValue(existing.source());

            if (existing.companyId() != null) {
                CompanyDto selectedCompany = companies.stream()
                    .filter(c -> c.id().equals(existing.companyId())).findFirst().orElse(null);
                companyField.setValue(selectedCompany);

                // Populate project and recruiter dropdowns for the pre-selected company
                // (setValue on companyField does not trigger the ValueChangeListener programmatically)
                var projects = projectService.findByCompanyId(existing.companyId(), current.getId(), current.getRole());
                projectField.setItems(projects);
                projectField.setItemLabelGenerator(ProjectDto::name);
                projectField.setEnabled(true);
                projectField.setPlaceholder(null);
                projectField.setValue(projects.stream()
                    .filter(p -> p.id().equals(existing.projectId())).findFirst().orElse(null));

                var recruiters = recruiterService.findByCompanyId(existing.companyId(), current.getId(), current.getRole());
                recruiterField.setItems(recruiters);
                recruiterField.setValue(recruiters.stream()
                    .filter(r -> r.id().equals(existing.recruiterId())).findFirst().orElse(null));
            }
            statusField.setValue(statuses.stream()
                .filter(s -> s.id().equals(existing.currentStatusId())).findFirst().orElse(null));
        }

        Button save = new Button("Save", e -> {
            try {
                if (positionField.isEmpty()) {
                    Notification.show("Position is required", 3000, Notification.Position.MIDDLE);
                    positionField.focus();
                    return;
                }
                if (typeField.isEmpty()) {
                    Notification.show("Employment Type is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (workTypeField.isEmpty()) {
                    Notification.show("Work Type is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (sourceField.isEmpty()) {
                    Notification.show("Source is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (companyField.isEmpty()) {
                    Notification.show("Company is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (projectField.isEmpty()) {
                    Notification.show("Project is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (statusField.isEmpty()) {
                    Notification.show("Status is required", 3000, Notification.Position.MIDDLE);
                    return;
                }

                form.setPosition(positionField.getValue());
                form.setJobDescription(descField.getValue());
                form.setJobUrl(urlField.getValue());
                form.setLocation(locationField.getValue());
                form.setSalaryRange(salaryField.getValue());
                form.setNotes(notesField.getValue());
                form.setSourceMeta(sourceMetaField.getValue());
                form.setJobType(typeField.getValue());
                form.setWorkType(workTypeField.getValue());
                form.setSource(sourceField.getValue());

                form.setCurrentStatusId(statusField.getValue().id());
                form.setProjectId(projectField.getValue().id());

                if (recruiterField.getValue() != null) {
                    form.setRecruiterId(recruiterField.getValue().id());
                }

                if (existing == null) {
                    jobService.create(form, current.getId(), current.getRole());
                } else {
                    jobService.update(existing.id(), form, current.getId(), current.getRole());
                }
                dialog.close();
                refresh();
                Notification.show("Saved");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(positionField, descField, urlField, locationField, salaryField,
            typeField, workTypeField, sourceField, sourceMetaField, notesField,
            companyField, projectField, recruiterField, statusField),
            new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refresh() {
        User current = securityService.getCurrentUser();
        try {
            grid.setItems(jobService.findAll(current.getId(), current.getRole(), null, null, null));
        } catch (Exception ex) {
            Notification.show("Failed to load jobs: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}
