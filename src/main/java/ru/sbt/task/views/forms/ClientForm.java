package ru.sbt.task.views.forms;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
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

    private final TextField fullName = new TextField("ФИО");
    private final TextField phone = new TextField("Телефон");
    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");

    private final Binder<Client> binder = new Binder<>(Client.class);
    private Client client;
    private Dialog parentDialog;
    private Runnable saveHandler;
    private final ClientRepository clientRepository;

    @Autowired
    public ClientForm(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
        configureForm();
    }

    private void configureForm() {
        setWidth("400px");
        setResponsiveSteps(new ResponsiveStep("0", 1));

        fullName.setWidthFull();
        phone.setWidthFull();

        binder.forField(fullName)
                .asRequired("Обязательное поле")
                .withValidator(name -> name.length() >= 3, "Минимум 3 символа")
                .bind(Client::getFullName, Client::setFullName);

        binder.forField(phone)
                .asRequired("Обязательное поле")
                .withValidator(new RegexpValidator(
                        "Неверный формат телефона", "^\\+?[0-9\\s\\-]{10,15}$"))
                .bind(Client::getPhone, Client::setPhone);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> save());

        cancel.addClickListener(e -> {
            if (parentDialog != null) {
                parentDialog.close();
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

    public void setParentDialog(Dialog dialog) {
        this.parentDialog = dialog;
    }

    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }

    private void save() {
        try {
            if (binder.writeBeanIfValid(client)) {
                Client savedClient = clientRepository.save(client);
                if (saveHandler != null) {
                    saveHandler.run();
                }
                if (parentDialog != null) {
                    parentDialog.close();
                }
                Notification.show("Клиент сохранен", 3000, Notification.Position.BOTTOM_END);
            }
        } catch (Exception e) {
            Notification.show("Ошибка сохранения: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_END);
        }
    }
}