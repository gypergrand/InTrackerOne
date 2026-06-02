package com.intrackerone.ui.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import jakarta.annotation.security.PermitAll;

@Route("login")
@PermitAll
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        login.setForgotPasswordButtonVisible(false);
        login.addLoginListener(this::onLogin);
        add(new H1("InTrackerOne"), new Span("Job Interview Tracker"), login);
    }

    private void onLogin(LoginForm.LoginEvent event) {
        try {
            jakarta.servlet.http.HttpServletRequest request =
                (jakarta.servlet.http.HttpServletRequest) VaadinService.getCurrentRequest();
            request.login(event.getUsername(), event.getPassword());
            getUI().ifPresent(ui -> ui.navigate(""));
        } catch (jakarta.servlet.ServletException ex) {
            login.setError(true);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
