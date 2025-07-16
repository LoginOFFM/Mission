package ru.sbt.task.views.forms;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Procuration;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProcurationForm extends FormLayout {
    private final TextField number = new TextField("Номер");
    private final DatePicker date = new DatePicker("Дата");
    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");
    private final Binder<Procuration> binder = new Binder<>(Procuration.class);
    private Procuration procuration;
    private Runnable saveHandler;
    public Dialog getParentDialog() {
        return parentDialog;
    }

    public void setParentDialog(Dialog parentDialog) {
        this.parentDialog = parentDialog;
    }

    private Dialog parentDialog;
    public ProcurationForm() {
        configureLayout();
        configureBinder();
        setupButtons();
        setVisible(false);
    }

    private void configureLayout() {
        setWidth("25em");
        add(number, date, new HorizontalLayout(save, cancel));
    }

    private void configureBinder() {
        binder.forField(number)
                .asRequired("Введите номер")
                .bind(Procuration::getNumber, Procuration::setNumber);

        binder.forField(date)
                .asRequired("Выберите дату")
                .bind(Procuration::getDate, Procuration::setDate);
    }

    private void setupButtons() {
        save.addClickListener(e -> save());
        cancel.addClickListener(e -> cancel());
    }

    public void editProcuration(Procuration procuration) {
        this.procuration = procuration != null ? procuration : new Procuration();
        binder.setBean(this.procuration);
        setVisible(true);
    }

    private void save() {
        try {
            if (!binder.writeBeanIfValid(procuration)) {
                return;
            }

            if (saveHandler != null) {
                saveHandler.run();
            }

            if (getParentDialog() != null) {
                getParentDialog().close();
            }

        } catch (Exception e) {
            Notification.show("Ошибка сохранения: " + e.getMessage(),
                    3000, Notification.Position.BOTTOM_END);
        }
    }

    private void cancel() {
        if (parentDialog != null) {
            parentDialog.close();
        }
        binder.readBean(procuration);
    }

    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }

    public Procuration getProcuration() {
        return procuration;
    }
}