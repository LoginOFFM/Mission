package ru.sbt.task.views;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;


@Route("login")
@PageTitle("Вход в систему")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthenticationManager authManager;
    private final LoginForm loginForm = new LoginForm();

    public LoginView(AuthenticationManager authManager) {
        this.authManager = authManager;
        initView();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (isUserLoggedIn()) {
            event.forwardTo("contracts");
        }
    }

    private boolean isUserLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null &&
                auth.isAuthenticated() &&
                !(auth instanceof AnonymousAuthenticationToken);
    }

    private void initView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.setI18n(createRussianI18n());
        loginForm.addLoginListener(this::authenticate);
        add(loginForm);
    }

    private void authenticate(LoginForm.LoginEvent event) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            event.getUsername(),
                            event.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            UI.getCurrent().navigate("contracts");

        } catch (BadCredentialsException e) {
            showError("Неверный логин или пароль");
            loginForm.setEnabled(true);
        } catch (AuthenticationException e) {
            showError("Ошибка аутентификации");
            loginForm.setEnabled(true);
        }
    }

    private void showError(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private LoginI18n createRussianI18n() {
        LoginI18n i18n = LoginI18n.createDefault();

        i18n.getForm().setTitle("Вход в систему");
        i18n.getForm().setUsername("Логин");
        i18n.getForm().setPassword("Пароль");
        i18n.getForm().setSubmit("Войти");

        return i18n;
    }
}