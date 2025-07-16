package ru.sbt.task.views.forms;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Point;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PointForm extends FormLayout {
    private final TextField name = new TextField("Название");
    private final TextField address = new TextField("Адрес");
    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");
    private final Binder<Point> binder = new Binder<>(Point.class);
    private Point point;
    private Runnable saveHandler;

    public Dialog getParentDialog() {
        return parentDialog;
    }

    private Dialog parentDialog;


    public PointForm() {
        configureLayout();
        configureBinder();
        setupButtons();
        setVisible(false);
    }
    public void setParentDialog(Dialog dialog) {
        this.parentDialog = dialog;
    }
    private void configureLayout() {
        setWidth("25em");
        add(name, address, new HorizontalLayout(save, cancel));
    }

    private void configureBinder() {
        binder.forField(name)
                .asRequired("Введите название")
                .bind(Point::getName, Point::setName);

        binder.forField(address)
                .asRequired("Введите адрес")
                .bind(Point::getAddress, Point::setAddress);
    }

    private void setupButtons() {
        save.addClickListener(e -> save());
        cancel.addClickListener(e -> cancel());
    }

    public void editPoint(Point point) {
        this.point = point != null ? point : new Point();
        binder.setBean(this.point);
        setVisible(true);
    }

    private void save() {
        try {
            if (!binder.writeBeanIfValid(point)) {
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
        binder.readBean(point);
    }
    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }

    public Point getPoint() {
        return point;
    }
}