package com.intrackerone.ui.views.question;

import com.intrackerone.domain.question.QuestionDto;
import com.intrackerone.domain.question.QuestionForm;
import com.intrackerone.domain.question.QuestionService;
import com.intrackerone.domain.recruiter.RecruiterDto;
import com.intrackerone.domain.recruiter.RecruiterService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route(value = "questions", layout = MainLayout.class)
@PermitAll
public class QuestionsView extends VerticalLayout {

    private final QuestionService questionService;
    private final RecruiterService recruiterService;
    private final SecurityService securityService;
    private final Grid<QuestionDto> grid = new Grid<>(QuestionDto.class);

    public QuestionsView(QuestionService questionService, RecruiterService recruiterService,
                         SecurityService securityService) {
        this.questionService = questionService;
        this.recruiterService = recruiterService;
        this.securityService = securityService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(QuestionDto::question).setHeader("Question").setSortable(true).setWidth("300px");
        grid.addColumn(QuestionDto::answer).setHeader("Answer").setWidth("300px");
        grid.addColumn(QuestionDto::notes).setHeader("Notes");
        grid.addColumn(QuestionDto::recruiterName).setHeader("Recruiter").setSortable(true);
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) openEditDialog(e.getValue());
        });
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Add Question", VaadinIcon.PLUS.create(), e -> openEditDialog(null));
        Button addFromDefaults = new Button("Add from Defaults", VaadinIcon.BOOK.create(), e -> openAddFromDefaultsDialog());
        Button refresh = new Button(VaadinIcon.REFRESH.create(), e -> refresh());
        refresh.getElement().setAttribute("title", "Refresh");
        return new HorizontalLayout(add, addFromDefaults, refresh);
    }

    private void openEditDialog(QuestionDto existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing != null ? "Edit Question" : "Add Question");

        User current = securityService.getCurrentUser();
        QuestionForm form = new QuestionForm();

        TextField questionField = new TextField("Question");
        questionField.setRequiredIndicatorVisible(true);
        questionField.setWidthFull();
        TextArea answerField = new TextArea("Answer");
        answerField.setRequiredIndicatorVisible(true);
        answerField.setWidthFull();
        TextArea notesField = new TextArea("Notes");
        notesField.setWidthFull();

        Select<RecruiterDto> recruiterField = new Select<>();
        recruiterField.setLabel("Recruiter");
        recruiterField.setItems(recruiterService.findAllAccessible(current.getId(), current.getRole()));
        recruiterField.setItemLabelGenerator(r -> r.name() + (r.companyName() != null ? " (" + r.companyName() + ")" : ""));
        recruiterField.setEmptySelectionAllowed(true);
        recruiterField.setPlaceholder("None");

        if (existing != null) {
            questionField.setValue(existing.question());
            answerField.setValue(existing.answer());
            notesField.setValue(existing.notes() != null ? existing.notes() : "");
            if (existing.recruiterId() != null) {
                recruiterField.setValue(recruiterService.findAllAccessible(current.getId(), current.getRole()).stream()
                    .filter(r -> r.id().equals(existing.recruiterId())).findFirst().orElse(null));
            }
        }

        Button save = new Button("Save", e -> {
            try {
                if (questionField.getValue() == null || questionField.getValue().isBlank()) {
                    Notification.show("Question is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (answerField.getValue() == null || answerField.getValue().isBlank()) {
                    Notification.show("Answer is required", 3000, Notification.Position.MIDDLE);
                    return;
                }
                form.setQuestion(questionField.getValue());
                form.setAnswer(answerField.getValue());
                form.setNotes(notesField.getValue().isBlank() ? null : notesField.getValue());
                form.setRecruiterId(recruiterField.getValue() != null ? recruiterField.getValue().id() : null);

                if (existing == null) {
                    questionService.create(form, current.getId());
                } else {
                    questionService.update(existing.id(), form, current.getId(), current.getRole());
                }
                dialog.close();
                refresh();
                Notification.show("Saved");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);

        if (existing != null) {
            Button delete = new Button("Delete", e -> {
                new ConfirmDialog("Confirm Delete", "Delete this question?",
                    "Delete", ev -> {
                        try {
                            questionService.delete(existing.id(), current.getId(), current.getRole());
                            dialog.close();
                            refresh();
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

        dialog.add(new FormLayout(questionField, answerField, notesField, recruiterField), buttons);
        dialog.open();
    }

    private void openAddFromDefaultsDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add from Default Questions");
        dialog.setWidth("600px");

        User current = securityService.getCurrentUser();
        List<QuestionDto> defaults = questionService.findDefaultQuestions();

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        for (QuestionDto d : defaults) {
            HorizontalLayout row = new HorizontalLayout();
            Span text = new Span(d.question());
            text.setWidthFull();
            Button add = new Button("Add", e -> {
                try {
                    questionService.addFromDefault(d.id(), current.getId());
                    refresh();
                    Notification.show("Added: " + d.question());
                } catch (Exception ex) {
                    Notification.show("Failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            });
            row.add(text, add);
            row.setWidthFull();
            row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            list.add(row);
        }

        if (defaults.isEmpty()) {
            list.add(new Span("No default questions available."));
        }

        Button close = new Button("Close", e -> dialog.close());
        dialog.add(list, close);
        dialog.open();
    }

    private void refresh() {
        User current = securityService.getCurrentUser();
        try {
            List<QuestionDto> questions = questionService.findOwnQuestions(current.getId());
            grid.setItems(questions);
        } catch (Exception ex) {
            Notification.show("Failed to load questions: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}
