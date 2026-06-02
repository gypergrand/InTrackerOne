package com.intrackerone.ui.views;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import com.intrackerone.ui.views.companies.CompanyListView;
import com.intrackerone.ui.views.jobs.JobListView;
import com.intrackerone.ui.views.pipeline.PipelineView;
import com.intrackerone.ui.views.projects.ProjectListView;
import com.intrackerone.ui.views.recruiters.RecruiterListView;
import com.intrackerone.ui.views.tokens.ApiTokenListView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout implements RouterLayout {

    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
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
        Button logout = new Button("Logout", e -> {
            VaadinSession.getCurrent().getSession().invalidate();
            VaadinSession.getCurrent().close();
            getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
        });
        header.add(userBadge, logout);
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
        nav.addItem(new SideNavItem("API Tokens", ApiTokenListView.class, VaadinIcon.KEY.create()));
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MODERATOR) {
            nav.addItem(new SideNavItem("Users", UserAdminView.class, VaadinIcon.USER_CARD.create()));
        }

        addToDrawer(new VerticalLayout(nav));
    }
}