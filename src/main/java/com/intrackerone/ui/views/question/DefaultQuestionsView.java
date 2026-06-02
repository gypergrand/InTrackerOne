package com.intrackerone.ui.views.question;

import com.intrackerone.domain.question.QuestionDto;
import com.intrackerone.domain.question.QuestionForm;
import com.intrackerone.domain.question.QuestionService;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "default-questions", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class DefaultQuestionsView extends VerticalLayout {

    private final QuestionService questionService;
    private final Grid<QuestionDto> grid = new Grid<>(QuestionDto.class);

    public DefaultQuestionsView(QuestionService questionService) {
        this.questionService = questionService;
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
        grid.addComponentColumn(this::createActionRow).setHeader("Actions").setWidth("100px");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Add Default Question", VaadinIcon.PLUS.create(), e -> openDialog(null));
        Button refresh = new Button(VaadinIcon.REFRESH.create(), e -> refresh());
        refresh.getElement().setAttribute("title", "Refresh");
        return new HorizontalLayout(add, refresh);
    }

    private void openDialog(QuestionDto existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing != null ? "Edit Default Question" : "Add Default Question");
        dialog.setWidth("500px");

        TextField questionField = new TextField("Question");
        questionField.setRequiredIndicatorVisible(true);
        questionField.setWidthFull();
        TextArea answerField = new TextArea("Answer");
        answerField.setRequiredIndicatorVisible(true);
        answerField.setWidthFull();
        TextArea notesField = new TextArea("Notes");
        notesField.setWidthFull();

        if (existing != null) {
            questionField.setValue(existing.question());
            answerField.setValue(existing.answer());
            notesField.setValue(existing.notes() != null ? existing.notes() : "");
        }

        Button save = new Button("Save", e -> {
            if (questionField.getValue() == null || questionField.getValue().isBlank()) {
                Notification.show("Question is required", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (answerField.getValue() == null || answerField.getValue().isBlank()) {
                Notification.show("Answer is required", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                QuestionForm form = new QuestionForm();
                form.setQuestion(questionField.getValue());
                form.setAnswer(answerField.getValue());
                form.setNotes(notesField.getValue().isBlank() ? null : notesField.getValue());
                if (existing == null) {
                    questionService.createDefault(form);
                } else {
                    questionService.updateDefault(existing.id(), form);
                }
                dialog.close();
                refresh();
                Notification.show("Default question saved");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(questionField, answerField, notesField), new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refresh() {
        try {
            List<QuestionDto> questions = questionService.findDefaultQuestions();
            grid.setItems(questions);
        } catch (Exception ex) {
            Notification.show("Failed to load default questions: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private HorizontalLayout createActionRow(QuestionDto question) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(question));
        edit.getElement().setAttribute("title", "Edit");

        Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Delete Default Question");
            confirm.setText("Are you sure you want to delete \"" + question.question() + "\"?");
            confirm.setCancelable(true);
            confirm.setConfirmText("Delete");
            confirm.setConfirmButtonTheme("error primary");
            confirm.addConfirmListener(ev -> {
                try {
                    questionService.deleteDefault(question.id());
                    refresh();
                    Notification.show("Default question deleted");
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
