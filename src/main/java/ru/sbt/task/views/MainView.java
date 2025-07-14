package ru.sbt.task.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
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
public class MainView extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private final AuthenticationContext authContext;
    private final ObjectProvider<ClientContractView> clientContractViewProvider;
    private final ObjectProvider<AdminView> adminViewProvider;

    public MainView(AuthenticationContext authContext,
                    ObjectProvider<ClientContractView> clientContractViewProvider,
                    ObjectProvider<AdminView> adminViewProvider) {
        this.authContext = authContext;
        this.clientContractViewProvider = clientContractViewProvider;
        this.adminViewProvider = adminViewProvider;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();

        authContext.getAuthenticatedUser(UserDetails.class).ifPresent(user -> {
            TabSheet tabSheet = new TabSheet();
            tabSheet.setSizeFull();


            ClientContractView contractsView = clientContractViewProvider.getObject();
            tabSheet.add("Договоры и клиенты", contractsView);

            if (user.getAuthorities().stream()
                    .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"))) {
                // Создаем новый экземпляр AdminView для текущего UI
                AdminView adminView = adminViewProvider.getObject();
                tabSheet.add("Администрирование", adminView);
            }

            add(tabSheet);
            expand(tabSheet);
        });
    }
}