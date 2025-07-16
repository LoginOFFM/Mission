package ru.sbt.task.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Employee;
import ru.sbt.task.model.entity.Point;
import ru.sbt.task.model.entity.Procuration;
import ru.sbt.task.model.repository.EmployeeRepository;
import ru.sbt.task.model.repository.PointRepository;
import ru.sbt.task.model.repository.ProcurationRepository;
import ru.sbt.task.views.editor.EmployeeEditor;
import ru.sbt.task.views.editor.PointEditor;
import ru.sbt.task.views.editor.ProcurationEditor;

import java.util.List;
@Component
@UIScope
@Route(value = "admin", layout = MainView.class)
@PageTitle("Администрирование")
@RolesAllowed("ADMIN")
public class AdminView extends VerticalLayout {

    private final TabSheet tabs = new TabSheet();
    private final EmployeeRepository employeeRepository;
    private final PointRepository pointRepository;
    private final ProcurationRepository procurationRepository;

    private final Grid<Employee> employeeGrid = new Grid<>();
    private final Grid<Point> pointGrid = new Grid<>();
    private final Grid<Procuration> procurationGrid = new Grid<>();

    private TextField employeeFilter = new TextField();
    private TextField pointFilter = new TextField();
    private TextField procurationFilter = new TextField();

    @Autowired
    public AdminView(EmployeeRepository employeeRepo,
                     PointRepository pointRepo,
                     ProcurationRepository procurationRepo,
                     EmployeeEditor employeeEditor,
                     PointEditor pointEditor,
                     ProcurationEditor procurationEditor) {

        this.employeeRepository = employeeRepo;
        this.pointRepository = pointRepo;
        this.procurationRepository = procurationRepo;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        initTabs(employeeEditor, pointEditor, procurationEditor);
        add(tabs);

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshAllData();
    }

    private void initTabs(EmployeeEditor employeeEditor, PointEditor pointEditor, ProcurationEditor procurationEditor) {
        initEmployeeTab(employeeEditor);
        initPointTab(pointEditor);
        initProcurationTab(procurationEditor);



        tabs.addSelectedChangeListener(event -> {
            employeeEditor.setVisible(false);
            pointEditor.setVisible(false);
            procurationEditor.setVisible(false);
            employeeGrid.asSingleSelect().clear();
            pointGrid.asSingleSelect().clear();
            procurationGrid.asSingleSelect().clear();
            if (event.getSelectedTab().getLabel().equals("Сотрудники")) {

                employeeEditor.loadProcurations();
            }
        });
        tabs.setWidthFull();
        tabs.getStyle().set("margin", "0 auto");
    }

    private void refreshAllData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {

                List<Employee> employees = employeeRepository.findAll();
                List<Point> points = pointRepository.findAll();
                List<Procuration> procurations = procurationRepository.findAll();

                employeeGrid.setItems(employees);
                pointGrid.setItems(points);
                procurationGrid.setItems(procurations);

                employeeFilter.clear();
                pointFilter.clear();
                procurationFilter.clear();
            } catch (Exception e) {
                Notification.show("Ошибка загрузки данных: " + e.getMessage(),
                        3000, Notification.Position.BOTTOM_END);
            }
        }));
    }
    private void initEmployeeTab(EmployeeEditor editor) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        configureEmployeeGrid();
        configureEmployeeFilter();
        Button addBtn = new Button("Добавить сотрудника", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> {
            editor.editEmployee(new Employee());
            employeeGrid.asSingleSelect().clear();
        });
        Button refreshBtn = new Button("Обновить", VaadinIcon.REFRESH.create());
        refreshBtn.addClickListener(e -> refreshEmployeeData());
        editor.setSaveHandler(() -> {
            employeeRepository.save(editor.getEmployee());
            refreshEmployeeData();
            editor.setVisible(false);
            Notification.show("Сотрудник сохранен", 3000, Notification.Position.BOTTOM_END);
        });

        employeeGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                editor.editEmployee(e.getValue());
            }
        });
        HorizontalLayout toolbar = new HorizontalLayout(employeeFilter, addBtn, refreshBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.add(toolbar, employeeGrid, editor);
        tabs.add(new Tab("Сотрудники"), layout);
    }

    private void configureEmployeeGrid() {
        employeeGrid.removeAllColumns();

        employeeGrid.addColumn(Employee::getFullName)
                .setHeader("ФИО")
                .setSortable(true)
                .setAutoWidth(true);

        employeeGrid.addColumn(e -> e.getProcuration() != null ? e.getProcuration().getNumber() : "—")
                .setHeader("Доверенность")
                .setSortable(true)
                .setAutoWidth(true);

        employeeGrid.addColumn(Employee::getRole)
                .setHeader("Роль")
                .setSortable(true)
                .setAutoWidth(true);

        employeeGrid.addColumn(new ComponentRenderer<>(employee -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> employeeGrid.select(employee));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> confirmEmployeeDeletion(employee));

            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Действия").setWidth("150px");

        employeeGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
    }

    private void configureEmployeeFilter() {
        employeeFilter.setPlaceholder("Фильтр по ФИО...");
        employeeFilter.setClearButtonVisible(true);
        employeeFilter.setValueChangeMode(ValueChangeMode.LAZY);
        employeeFilter.addValueChangeListener(e -> updateEmployeeList());
    }

    private void updateEmployeeList() {
        if (employeeFilter.getValue().isEmpty()) {
            employeeGrid.setItems(employeeRepository.findAll());
        } else {
            employeeGrid.setItems(employeeRepository.findByFullNameContainingIgnoreCase(employeeFilter.getValue()));
        }
    }

    private void confirmEmployeeDeletion(Employee employee) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setCloseOnEsc(false);
        confirmDialog.setCloseOnOutsideClick(false);

        Span message = new Span("Вы действительно хотите удалить сотрудника " + employee.getFullName() + "?");

        Button confirmBtn = new Button("Удалить", VaadinIcon.TRASH.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmBtn.addClickListener(e -> {
            deleteEmployee(employee);
            confirmDialog.close();
        });

        Button cancelBtn = new Button("Отмена");
        cancelBtn.addClickListener(e -> confirmDialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmBtn, cancelBtn);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        confirmDialog.add(new VerticalLayout(message, buttons));
        confirmDialog.open();
    }

    private void deleteEmployee(Employee employee) {
        try {
            employeeRepository.delete(employee);
            refreshEmployeeData();
            Notification.show("Сотрудник удален", 3000, Notification.Position.BOTTOM_END);
        } catch (Exception e) {
            Notification.show("Ошибка удаления: " + e.getMessage(),
                    3000, Notification.Position.BOTTOM_END);
        }
    }

    private void initPointTab(PointEditor editor) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        configurePointGrid();
        configurePointFilter();

        Button addBtn = new Button("Добавить точку", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> {
            editor.editPoint(new Point());
            pointGrid.asSingleSelect().clear();
        });

        Button refreshBtn = new Button("Обновить", VaadinIcon.REFRESH.create());
        refreshBtn.addClickListener(e -> refreshPointData());

        editor.setSaveHandler(() -> {
            pointRepository.save(editor.getPoint());
            refreshPointData();
            editor.setVisible(false);
            Notification.show("Точка сохранена", 3000, Notification.Position.BOTTOM_END);
        });

        pointGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                editor.editPoint(e.getValue());
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(pointFilter, addBtn, refreshBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);

        layout.add(toolbar, pointGrid, editor);
        tabs.add(new Tab("Точки выдачи"), layout);
    }

    private void configurePointGrid() {
        pointGrid.removeAllColumns();

        pointGrid.addColumn(Point::getName)
                .setHeader("Название")
                .setSortable(true)
                .setAutoWidth(true);

        pointGrid.addColumn(Point::getAddress)
                .setHeader("Адрес")
                .setSortable(true)
                .setAutoWidth(true);

        pointGrid.addColumn(new ComponentRenderer<>(point -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> pointGrid.select(point));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> confirmPointDeletion(point));

            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Действия").setWidth("150px");
    }

    private void configurePointFilter() {
        pointFilter.setPlaceholder("Фильтр по названию...");
        pointFilter.setClearButtonVisible(true);
        pointFilter.setValueChangeMode(ValueChangeMode.LAZY);
        pointFilter.addValueChangeListener(e -> updatePointList());
    }

    private void updatePointList() {
        if (pointFilter.getValue().isEmpty()) {
            pointGrid.setItems(pointRepository.findAll());
        } else {
            pointGrid.setItems(pointRepository.findByNameContainingIgnoreCase(pointFilter.getValue()));
        }
    }

    private void confirmPointDeletion(Point point) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setCloseOnEsc(false);
        confirmDialog.setCloseOnOutsideClick(false);

        Span message = new Span("Вы действительно хотите удалить точку " + point.getName() + "?");

        Button confirmBtn = new Button("Удалить", VaadinIcon.TRASH.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmBtn.addClickListener(e -> {
            deletePoint(point);
            confirmDialog.close();
        });

        Button cancelBtn = new Button("Отмена");
        cancelBtn.addClickListener(e -> confirmDialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmBtn, cancelBtn);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        confirmDialog.add(new VerticalLayout(message, buttons));
        confirmDialog.open();
    }

    private void deletePoint(Point point) {
        try {
            pointRepository.delete(point);
            refreshPointData();
            Notification.show("Точка удалена", 3000, Notification.Position.BOTTOM_END);
        } catch (Exception e) {
            Notification.show("Ошибка удаления: " + e.getMessage(),
                    3000, Notification.Position.BOTTOM_END);
        }
    }

    private void initProcurationTab(ProcurationEditor editor) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        configureProcurationGrid();
        configureProcurationFilter();
        Button addBtn = new Button("Добавить доверенность", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> {
            editor.editProcuration(new Procuration());
            procurationGrid.asSingleSelect().clear();
        });
        Button refreshBtn = new Button("Обновить", VaadinIcon.REFRESH.create());
        refreshBtn.addClickListener(e -> refreshProcurationData());
        editor.setSaveHandler(() -> {
            procurationRepository.save(editor.getProcuration());
            refreshAllData();
            editor.setVisible(false);
            Notification.show("Доверенность сохранена", 3000, Notification.Position.BOTTOM_END);
        });
        procurationGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                editor.editProcuration(e.getValue());
            }
        });
        HorizontalLayout toolbar = new HorizontalLayout(procurationFilter, addBtn, refreshBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.add(toolbar, procurationGrid, editor);
        tabs.add(new Tab("Доверенности"), layout);
    }

    private void configureProcurationGrid() {
        procurationGrid.removeAllColumns();

        procurationGrid.addColumn(Procuration::getNumber)
                .setHeader("Номер")
                .setSortable(true)
                .setAutoWidth(true);

        procurationGrid.addColumn(Procuration::getDate)
                .setHeader("Дата")
                .setSortable(true)
                .setAutoWidth(true);

        procurationGrid.addColumn(new ComponentRenderer<>(procuration -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> procurationGrid.select(procuration));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> confirmProcurationDeletion(procuration));

            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Действия").setWidth("150px");
    }

    private void configureProcurationFilter() {
        procurationFilter.setPlaceholder("Фильтр по номеру...");
        procurationFilter.setClearButtonVisible(true);
        procurationFilter.setValueChangeMode(ValueChangeMode.LAZY);
        procurationFilter.addValueChangeListener(e -> updateProcurationList());
    }

    private void updateProcurationList() {
        if (procurationFilter.getValue().isEmpty()) {
            procurationGrid.setItems(procurationRepository.findAll());
        } else {
            procurationGrid.setItems(procurationRepository.findByNumberContainingIgnoreCase(procurationFilter.getValue()));
        }
    }

    private void confirmProcurationDeletion(Procuration procuration) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setCloseOnEsc(false);
        confirmDialog.setCloseOnOutsideClick(false);

        Span message = new Span("Вы действительно хотите удалить доверенность №" + procuration.getNumber() + "?");

        Button confirmBtn = new Button("Удалить", VaadinIcon.TRASH.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmBtn.addClickListener(e -> {
            deleteProcuration(procuration);
            confirmDialog.close();
        });

        Button cancelBtn = new Button("Отмена");
        cancelBtn.addClickListener(e -> confirmDialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmBtn, cancelBtn);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        confirmDialog.add(new VerticalLayout(message, buttons));
        confirmDialog.open();
    }

    private void deleteProcuration(Procuration procuration) {
        try {
            procurationRepository.delete(procuration);
            refreshProcurationData();
            Notification.show("Доверенность удалена", 3000, Notification.Position.BOTTOM_END);
        } catch (Exception e) {
            Notification.show("Ошибка удаления: " + e.getMessage(),
                    3000, Notification.Position.BOTTOM_END);
        }
    }

    private void refreshEmployeeData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                employeeGrid.setItems(employeeRepository.findAll());
                employeeFilter.clear();

            } catch (Exception e) {
                Notification.show("Ошибка при обновлении данных сотрудников: " + e.getMessage(),
                        3000, Notification.Position.BOTTOM_END);
            }
        }));
    }

    private void refreshPointData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                List<Point> points = pointRepository.findAll();
                pointGrid.setItems(points);
                pointFilter.clear();
            } catch (Exception e) {
                Notification.show("Ошибка загрузки точек: " + e.getMessage(),
                        3000, Notification.Position.BOTTOM_END);
            }
        }));
    }

    private void refreshProcurationData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                List<Procuration> procurations = procurationRepository.findAll();
                procurationGrid.setItems(procurations);
                procurationFilter.clear();
            } catch (Exception e) {
                Notification.show("Ошибка загрузки доверенностей: " + e.getMessage(),
                        3000, Notification.Position.BOTTOM_END);
            }
        }));
    }
}