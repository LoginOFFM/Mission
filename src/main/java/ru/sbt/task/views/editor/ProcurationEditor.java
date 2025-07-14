package ru.sbt.task.views.editor;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Procuration;

import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProcurationEditor extends FormLayout {
    private final TextField number = new TextField("Номер");
    private final DatePicker date = new DatePicker("Дата");
    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");
    private final Binder<Procuration> binder = new Binder<>(Procuration.class);
    private Procuration procuration;
    private Runnable saveHandler;

    public ProcurationEditor() {
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
        if (binder.writeBeanIfValid(procuration) && saveHandler != null) {
            saveHandler.run();
        }
    }

    private void cancel() {
        setVisible(false);
        binder.readBean(procuration);
    }

    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }

    public Procuration getProcuration() {
        return procuration;
    }
}