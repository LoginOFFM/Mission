package ru.sbt.task.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.component.HasElement;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.ServiceLoader;

@Component
@UIScope
@Route("main")
@PageTitle("Главная")
public class MainView extends VerticalLayout implements RouterLayout {

    private final AuthenticationContext authContext;
    private final ClientContractView clientContractView;
    private final AdminView adminView;

    public MainView(AuthenticationContext authContext,
                    ClientContractView clientContractView,
                    AdminView adminView) {
        this.authContext = authContext;
        this.clientContractView = clientContractView;
        this.adminView = adminView;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        createNavigation();
    }

    private void createNavigation() {
        HorizontalLayout navigation = new HorizontalLayout();
        navigation.setPadding(true);
        navigation.setSpacing(true);

        // Кнопка для клиентской части
        Button contractsBtn = new Button("Договоры и клиенты", e -> {
            UI.getCurrent().navigate("contracts");
        });

        navigation.add(contractsBtn);

        // Кнопка админки (только для администраторов)
        authContext.getAuthenticatedUser(UserDetails.class).ifPresent(user -> {
            if (user.getAuthorities().stream()
                    .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"))) {
                Button adminBtn = new Button("Администрирование", e -> {
                    UI.getCurrent().navigate("admin");
                });
                navigation.add(adminBtn);
            }
        });

        add(navigation);
    }
}