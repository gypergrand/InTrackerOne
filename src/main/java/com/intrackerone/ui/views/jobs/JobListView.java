package com.intrackerone.ui.views.jobs;

import com.intrackerone.domain.company.CompanyService;
import com.intrackerone.domain.job.JobDto;
import com.intrackerone.domain.job.JobForm;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.job.JobType;
import com.intrackerone.domain.job.Source;
import com.intrackerone.domain.project.ProjectService;
import com.intrackerone.domain.recruiter.RecruiterService;
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
        grid.addColumn(JobDto::jobType).setHeader("Type");
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
        TextArea descField = new TextArea("Description");
        TextField urlField = new TextField("Job URL");
        TextField locationField = new TextField("Location");
        TextField salaryField = new TextField("Salary Range");
        TextArea notesField = new TextArea("Notes");
        TextField sourceMetaField = new TextField("Source Meta");

        Select<JobType> typeField = new Select<>();
        typeField.setLabel("Job Type");
        typeField.setItems(JobType.values());

        Select<Source> sourceField = new Select<>();
        sourceField.setLabel("Source");
        sourceField.setItems(Source.values());

        var companies = companyService.findAll(current.getId(), current.getRole());
        ComboBox<Object> companyField = new ComboBox<>("Company");
        companyField.setItems(companies);
        companyField.setItemLabelGenerator(c -> ((com.intrackerone.domain.company.CompanyDto) c).name());

        ComboBox<Object> projectField = new ComboBox<>("Project");
        ComboBox<Object> recruiterField = new ComboBox<>("Recruiter");
        recruiterField.setItemLabelGenerator(r -> ((com.intrackerone.domain.recruiter.RecruiterDto) r).name());

        var statuses = pipelineStatusService.findByOwnerId(current.getId());
        ComboBox<Object> statusField = new ComboBox<>("Status");
        statusField.setItems(statuses);
        statusField.setItemLabelGenerator(s -> ((com.intrackerone.domain.status.PipelineStatusDto) s).name());

        companyField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                Long companyId = ((com.intrackerone.domain.company.CompanyDto) e.getValue()).id();
                var projects = projectService.findByCompanyId(companyId, current.getId(), current.getRole());
                projectField.setItems(projects);
                projectField.setItemLabelGenerator(p -> ((com.intrackerone.domain.project.ProjectDto) p).name());

                var recruiters = recruiterService.findByCompanyId(companyId, current.getId(), current.getRole());
                recruiterField.setItems(recruiters);
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
            sourceField.setValue(existing.source());
            companyField.setValue(companies.stream()
                .filter(c -> c.id().equals(existing.companyId())).findFirst().orElse(null));
            if (existing.companyId() != null) {
                var projects = projectService.findByCompanyId(existing.companyId(), current.getId(), current.getRole());
                projectField.setValue(projects.stream()
                    .filter(p -> p.id().equals(existing.projectId())).findFirst().orElse(null));

                var recruiters = recruiterService.findByCompanyId(existing.companyId(), current.getId(), current.getRole());
                recruiterField.setValue(recruiters.stream()
                    .filter(r -> r.id().equals(existing.recruiterId())).findFirst().orElse(null));
            }
            statusField.setValue(statuses.stream()
                .filter(s -> s.id().equals(existing.currentStatusId())).findFirst().orElse(null));
        }

        Button save = new Button("Save", e -> {
            form.setPosition(positionField.getValue());
            form.setJobDescription(descField.getValue());
            form.setJobUrl(urlField.getValue());
            form.setLocation(locationField.getValue());
            form.setSalaryRange(salaryField.getValue());
            form.setNotes(notesField.getValue());
            form.setSourceMeta(sourceMetaField.getValue());
            form.setJobType(typeField.getValue());
            form.setSource(sourceField.getValue());

            if (statusField.getValue() != null) {
                form.setCurrentStatusId(((com.intrackerone.domain.status.PipelineStatusDto) statusField.getValue()).id());
            }
            if (projectField.getValue() != null) {
                form.setProjectId(((com.intrackerone.domain.project.ProjectDto) projectField.getValue()).id());
            }
            if (recruiterField.getValue() != null) {
                form.setRecruiterId(((com.intrackerone.domain.recruiter.RecruiterDto) recruiterField.getValue()).id());
            }

            if (existing == null) {
                jobService.create(form, current.getId(), current.getRole());
            } else {
                jobService.update(existing.id(), form, current.getId(), current.getRole());
            }
            dialog.close();
            refresh();
            Notification.show("Saved");
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(positionField, descField, urlField, locationField, salaryField,
            typeField, sourceField, sourceMetaField, notesField,
            companyField, projectField, recruiterField, statusField),
            new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refresh() {
        User current = securityService.getCurrentUser();
        grid.setItems(jobService.findAll(current.getId(), current.getRole(), null, null, null));
    }
}
