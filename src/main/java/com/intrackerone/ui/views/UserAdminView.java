package com.intrackerone.ui.views;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserDto;
import com.intrackerone.domain.user.UserForm;
import com.intrackerone.domain.user.UserService;
import com.intrackerone.infrastructure.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "users", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MODERATOR"})
public class UserAdminView extends VerticalLayout {

    private final UserService userService;
    private final SecurityService securityService;
    private final Grid<UserDto> grid = new Grid<>(UserDto.class);

    public UserAdminView(UserService userService, SecurityService securityService) {
        this.userService = userService;
        this.securityService = securityService;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(UserDto::username).setHeader("Username").setSortable(true);
        grid.addColumn(UserDto::email).setHeader("Email");
        grid.addColumn(UserDto::role).setHeader("Role");
        grid.addColumn(UserDto::enabled).setHeader("Enabled");
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) openEditDialog(e.getValue());
        });
    }

    private HorizontalLayout createToolbar() {
        Button add = new Button("Add User", e -> openEditDialog(null));
        return new HorizontalLayout(add);
    }

    private void openEditDialog(UserDto existing) {
        User current = securityService.getCurrentUser();
        if (current.getRole() != Role.ADMIN) {
            Notification.show("Only admins can manage users");
            return;
        }

        Dialog dialog = new Dialog();
        UserForm form = new UserForm();

        TextField usernameField = new TextField("Username");
        usernameField.setRequiredIndicatorVisible(true);
        PasswordField passwordField = new PasswordField("Password");
        TextField emailField = new TextField("Email");
        emailField.setRequiredIndicatorVisible(true);
        ComboBox<Role> roleField = new ComboBox<>("Role");
        roleField.setItems(Role.values());
        roleField.setRequiredIndicatorVisible(true);
        Checkbox enabledField = new Checkbox("Enabled");
        enabledField.setValue(true);

        if (existing != null) {
            usernameField.setValue(existing.username());
            emailField.setValue(existing.email() != null ? existing.email() : "");
            roleField.setValue(existing.role());
            enabledField.setValue(existing.enabled());
            passwordField.setPlaceholder("Leave blank to keep current password");
        } else {
            passwordField.setRequiredIndicatorVisible(true);
        }

        Button save = new Button("Save", e -> {
            try {
                if (usernameField.getValue() == null || usernameField.getValue().isBlank()) {
                    Notification.show("Username is required", 3000, Notification.Position.MIDDLE);
                    usernameField.focus();
                    return;
                }
                if (existing == null && (passwordField.getValue() == null || passwordField.getValue().isBlank())) {
                    Notification.show("Password is required", 3000, Notification.Position.MIDDLE);
                    passwordField.focus();
                    return;
                }
                if (emailField.getValue() == null || emailField.getValue().isBlank()) {
                    Notification.show("Email is required", 3000, Notification.Position.MIDDLE);
                    emailField.focus();
                    return;
                }
                if (!emailField.getValue().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                    Notification.show("Email must be valid", 3000, Notification.Position.MIDDLE);
                    emailField.focus();
                    return;
                }
                if (roleField.getValue() == null) {
                    Notification.show("Role is required", 3000, Notification.Position.MIDDLE);
                    return;
                }

                form.setUsername(usernameField.getValue());
                form.setPassword(passwordField.getValue().isBlank() ? null : passwordField.getValue());
                form.setEmail(emailField.getValue());
                form.setRole(roleField.getValue());
                form.setEnabled(enabledField.getValue());

                if (existing != null && existing.id().equals(current.getId()) && !enabledField.getValue()) {
                    Notification.show("You cannot disable your own account");
                    return;
                }

                if (existing == null) {
                    userService.create(form);
                } else {
                    userService.update(existing.id(), form);
                }
                dialog.close();
                refresh();
                Notification.show("Saved");
            } catch (Exception ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(usernameField, passwordField, emailField, roleField, enabledField),
            new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private void refresh() {
        grid.setItems(userService.findAll());
    }
}