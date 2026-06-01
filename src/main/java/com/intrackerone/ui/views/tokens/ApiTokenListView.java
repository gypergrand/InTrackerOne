package com.intrackerone.ui.views.tokens;

import com.intrackerone.domain.user.ApiTokenCreateForm;
import com.intrackerone.domain.user.ApiTokenDto;
import com.intrackerone.domain.user.ApiTokenService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "api-tokens", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MODERATOR"})
public class ApiTokenListView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ApiTokenService apiTokenService;
    private final SecurityService securityService;
    private final Grid<ApiTokenDto> grid = new Grid<>(ApiTokenDto.class);

    public ApiTokenListView(ApiTokenService apiTokenService, SecurityService securityService) {
        this.apiTokenService = apiTokenService;
        this.securityService = securityService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(ApiTokenDto::name)
            .setHeader("Name")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addColumn(ApiTokenDto::enabled)
            .setHeader("Status")
            .setAutoWidth(true);

        grid.addColumn(token -> token.lastUsedAt() != null
                ? token.lastUsedAt().format(DATE_FMT)
                : "Never")
            .setHeader("Last Used")
            .setAutoWidth(true);

        grid.addColumn(token -> token.createdAt() != null
                ? token.createdAt().format(DATE_FMT)
                : "")
            .setHeader("Created")
            .setAutoWidth(true);

        grid.addComponentColumn(token -> {
            Button revokeBtn = new Button("Revoke", VaadinIcon.TRASH.create(), e -> revokeToken(token));
            if (!token.enabled()) {
                revokeBtn.setEnabled(false);
                revokeBtn.setText("Revoked");
            }
            return revokeBtn;
        }).setHeader("Actions").setAutoWidth(true);

        grid.getColumns().forEach(col -> col.setResizable(true));
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Create Token", VaadinIcon.PLUS.create(), e -> openCreateDialog());
        return new HorizontalLayout(add);
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create API Token");

        ApiTokenCreateForm form = new ApiTokenCreateForm();
        Binder<ApiTokenCreateForm> binder = new Binder<>(ApiTokenCreateForm.class);
        binder.setBean(form);

        TextField nameField = new TextField("Token Name");
        nameField.setPlaceholder("e.g. CI/CD pipeline, Slack integration");
        binder.bind(nameField, "name");

        Button createBtn = new Button("Create", e -> {
            try {
                if (binder.validate().isOk()) {
                    User current = securityService.getCurrentUser();
                    ApiTokenService.CreatedToken created = apiTokenService.create(current.getId(), form.getName());
                    dialog.close();
                    refresh();
                    showTokenDialog(created);
                }
            } catch (Exception ex) {
                Notification.show("Failed to create token: " + ex.getMessage(),
                    5000, Notification.Position.MIDDLE);
            }
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(nameField), new HorizontalLayout(createBtn, cancelBtn));
        dialog.open();
    }

    private void showTokenDialog(ApiTokenService.CreatedToken created) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Token Created");

        Span warning = new Span("Copy this token now. It will not be shown again.");
        warning.getStyle().set("color", "red").set("font-weight", "bold");

        TextField tokenField = new TextField("API Token");
        tokenField.setValue(created.token());
        tokenField.setReadOnly(true);
        tokenField.setWidthFull();

        Span usageHint = new Span("Use as: Authorization: Bearer " + created.token());
        usageHint.getStyle().set("font-family", "monospace").set("font-size", "0.85em");

        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.getStyle().set("margin-top", "1em");

        dialog.add(warning, tokenField, usageHint, closeBtn);
        dialog.open();
    }

    private void revokeToken(ApiTokenDto token) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Revoke Token");
        confirmDialog.add(new Span("Revoke token \"" + token.name() + "\"? This action cannot be undone."));

        Button confirmBtn = new Button("Revoke", e -> {
            try {
                User current = securityService.getCurrentUser();
                apiTokenService.revoke(token.id(), current.getId());
                confirmDialog.close();
                refresh();
                Notification.show("Token revoked");
            } catch (Exception ex) {
                Notification.show("Failed to revoke token: " + ex.getMessage(),
                    5000, Notification.Position.MIDDLE);
            }
        });

        Button cancelBtn = new Button("Cancel", e -> confirmDialog.close());
        confirmDialog.add(new HorizontalLayout(confirmBtn, cancelBtn));
        confirmDialog.open();
    }

    private void refresh() {
        User current = securityService.getCurrentUser();
        try {
            List<ApiTokenDto> tokens = apiTokenService.findByUserId(current.getId());
            grid.setItems(tokens);
        } catch (Exception ex) {
            Notification.show("Failed to load API tokens: " + ex.getMessage(),
                5000, Notification.Position.MIDDLE);
        }
    }
}
