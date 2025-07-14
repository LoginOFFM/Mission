package ru.sbt.task.views;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
public class LoginView extends VerticalLayout {

    private final AuthenticationManager authManager;
    private final LoginForm loginForm = new LoginForm();

    public LoginView(AuthenticationManager authManager) {
        this.authManager = authManager;
        initView();
        setupLoginListener();
    }

    private void initView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Настраиваем форму входа
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.setI18n(createRussianI18n());

        add(loginForm);
    }

    private void setupLoginListener() {
        loginForm.addLoginListener(event -> {
            try {
                Authentication auth = authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                event.getUsername(),
                                event.getPassword()
                        )
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
                UI.getCurrent().getPage().setLocation("/"); // Перенаправление без перезагрузки

            } catch (BadCredentialsException e) {
                showError("Неверный логин или пароль");
            } catch (AuthenticationException e) {
                showError("Ошибка аутентификации");
            }
        });
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3000,
                Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

        // Очищаем только пароль, сохраняя логин
        loginForm.setEnabled(true);
    }

    private LoginI18n createRussianI18n() {
        LoginI18n i18n = LoginI18n.createDefault();

        i18n.getForm().setTitle("Вход в систему");
        i18n.getForm().setUsername("Логин");
        i18n.getForm().setPassword("Пароль");
        i18n.getForm().setSubmit("Войти");
        i18n.getForm().setForgotPassword("");

        i18n.getErrorMessage().setTitle("Ошибка входа");
        i18n.getErrorMessage().setMessage("Проверьте введенные данные");

        return i18n;
    }
}