package ru.sbt.task.views.editor;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Point;

import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PointEditor extends FormLayout {
    private final TextField name = new TextField("Название");
    private final TextField address = new TextField("Адрес");
    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");
    private final Binder<Point> binder = new Binder<>(Point.class);
    private Point point;
    private Runnable saveHandler;

    public PointEditor() {
        configureLayout();
        configureBinder();
        setupButtons();
        setVisible(false);
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
        if (binder.writeBeanIfValid(point) && saveHandler != null) {
            saveHandler.run();
        }
    }

    private void cancel() {
        setVisible(false);
        binder.readBean(point);
    }

    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }

    public Point getPoint() {
        return point;
    }
}