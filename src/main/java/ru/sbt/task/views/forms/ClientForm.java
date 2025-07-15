package ru.sbt.task.views.forms;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.RegexpValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Client;
import ru.sbt.task.model.repository.ClientRepository;

import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ClientForm extends FormLayout {

    private static final Logger logger = LoggerFactory.getLogger(ClientForm.class);

    private final Binder<Client> binder = new Binder<>(Client.class);
    private Client client;

    private final TextField fullName = new TextField("ФИО");
    private final TextField phone = new TextField("Телефон");
    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");

    private Runnable saveHandler;

    @Autowired
    public ClientForm(ClientRepository clientRepository) {
        setupBinder(clientRepository);
        setupLayout();
        setVisible(false);
    }

    private void setupBinder(ClientRepository clientRepository) {
        binder.forField(fullName)
                .asRequired("Обязательное поле")
                .withValidator(name -> name.length() >= 3, "Минимум 3 символа")
                .bind(Client::getFullName, Client::setFullName);

        binder.forField(phone)
                .asRequired("Обязательное поле")
                .withValidator(new RegexpValidator(
                        "Неверный формат телефона", "^\\+?[0-9\\s\\-]{10,15}$"))
                .bind(Client::getPhone, Client::setPhone);

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
    }

    private void setupLayout() {
        setWidth("400px");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> save());
        cancel.addClickListener(e -> {
            setVisible(false);
            if (client != null) {
                binder.readBean(client);
            } else {
                clear();
            }
        });

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        add(fullName, phone, buttons);
    }

    public void setClient(Client client) {
        this.client = client;
        binder.readBean(client);
    }

    public Client getClient() {
        return client;
    }

    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }

    public void clear() {
        binder.readBean(new Client());
    }
    public Button getSaveButton() {
        return save;
    }

    public Button getCancelButton() {
        return cancel;
    }
    private void save() {
        try {
            if (client == null) {
                client = new Client();
            }
            binder.writeBean(client);
            if (saveHandler != null) {
                saveHandler.run();
            }
            setVisible(false);
        } catch (ValidationException e) {
            logger.warn("Validation error saving client", e);
        } catch (Exception e) {
            Notification.show("Ошибка сохранения: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_END);
            logger.error("Error saving client", e);
        }
    }
}