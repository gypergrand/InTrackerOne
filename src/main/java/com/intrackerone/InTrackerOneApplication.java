package com.intrackerone;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
public class InTrackerOneApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(InTrackerOneApplication.class, args);
    }
}
