package com.intrackerone.ui.views;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.companies.CompanyListView;
import com.intrackerone.ui.views.jobs.JobListView;
import com.intrackerone.ui.views.pipeline.PipelineView;
import com.intrackerone.ui.views.projects.ProjectListView;
import com.intrackerone.ui.views.question.DefaultQuestionsView;
import com.intrackerone.ui.views.question.QuestionsView;
import com.intrackerone.ui.views.recruiters.RecruiterListView;
import com.intrackerone.ui.views.industry.DefaultIndustryView;
import com.intrackerone.ui.views.status.DefaultStatusView;
import com.intrackerone.ui.views.technology.DefaultTechnologiesView;
import com.intrackerone.ui.views.tokens.ApiTokenListView;
import com.intrackerone.domain.user.UserService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout implements RouterLayout {

    private final SecurityService securityService;
    private final UserService userService;

    public MainLayout(SecurityService securityService, UserService userService) {
        this.securityService = securityService;
        this.userService = userService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout(
            new DrawerToggle(),
            new H3("InTrackerOne")
        );
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(new com.vaadin.flow.component.html.Div());

        User current = securityService.getCurrentUser();
        Span userBadge = new Span(current.getUsername() + " (" + current.getRole() + ")");
        Button changePassword = new Button("Change Password", e -> openChangePasswordDialog(current));
        Button logout = new Button("Logout", e -> {
            VaadinSession.getCurrent().getSession().invalidate();
            VaadinSession.getCurrent().close();
            getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
        });
        header.add(userBadge, changePassword, logout);
        setPrimarySection(Section.DRAWER);
        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));

        nav.addItem(new SideNavItem("Companies", CompanyListView.class, VaadinIcon.BUILDING.create()));
        nav.addItem(new SideNavItem("Projects", ProjectListView.class, VaadinIcon.FOLDER.create()));
        nav.addItem(new SideNavItem("Recruiters", RecruiterListView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem("Jobs", JobListView.class, VaadinIcon.BRIEFCASE.create()));
        nav.addItem(new SideNavItem("Pipeline", PipelineView.class, VaadinIcon.CONNECT.create()));
        nav.addItem(new SideNavItem("Questions", QuestionsView.class, VaadinIcon.QUESTION.create()));
        nav.addItem(new SideNavItem("API Tokens", ApiTokenListView.class, VaadinIcon.KEY.create()));
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            nav.addItem(new SideNavItem("Users", UserAdminView.class, VaadinIcon.USER_CARD.create()));
            nav.addItem(new SideNavItem("Default Statuses", DefaultStatusView.class, VaadinIcon.GLOBE.create()));
            nav.addItem(new SideNavItem("Default Industries", DefaultIndustryView.class, VaadinIcon.COG.create()));
            nav.addItem(new SideNavItem("Default Questions", DefaultQuestionsView.class, VaadinIcon.BOOK.create()));
            nav.addItem(new SideNavItem("Default Technologies", DefaultTechnologiesView.class, VaadinIcon.PLUS.create()));
        }

        addToDrawer(new VerticalLayout(nav));
    }

    private void openChangePasswordDialog(User current) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Change Password");

        PasswordField currentPassword = new PasswordField("Current Password");
        currentPassword.setRequiredIndicatorVisible(true);
        PasswordField newPassword = new PasswordField("New Password");
        newPassword.setRequiredIndicatorVisible(true);
        PasswordField confirmPassword = new PasswordField("Confirm New Password");
        confirmPassword.setRequiredIndicatorVisible(true);

        Button save = new Button("Save", e -> {
            if (currentPassword.getValue() == null || currentPassword.getValue().isBlank()) {
                Notification.show("Current password is required", 3000, Notification.Position.MIDDLE);
                currentPassword.focus();
                return;
            }
            if (newPassword.getValue() == null || newPassword.getValue().isBlank()) {
                Notification.show("New password is required", 3000, Notification.Position.MIDDLE);
                newPassword.focus();
                return;
            }
            if (!newPassword.getValue().equals(confirmPassword.getValue())) {
                Notification.show("Passwords do not match", 3000, Notification.Position.MIDDLE);
                confirmPassword.focus();
                return;
            }
            try {
                userService.changePassword(current.getId(), currentPassword.getValue(), newPassword.getValue());
                dialog.close();
                Notification.show("Password changed");
            } catch (Exception ex) {
                Notification.show("Failed to change password: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(currentPassword, newPassword, confirmPassword),
            new HorizontalLayout(save, cancel));
        dialog.open();
    }
}