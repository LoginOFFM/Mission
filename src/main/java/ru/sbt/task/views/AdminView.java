package ru.sbt.task.views;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.AttachEvent;
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
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Contract;
import ru.sbt.task.model.entity.Employee;
import ru.sbt.task.model.entity.Point;
import ru.sbt.task.model.entity.Procuration;
import ru.sbt.task.model.repository.ContractRepository;
import ru.sbt.task.model.repository.EmployeeRepository;
import ru.sbt.task.model.repository.PointRepository;
import ru.sbt.task.model.repository.ProcurationRepository;
import ru.sbt.task.service.DataChangedEvent;
import ru.sbt.task.views.forms.EmployeeForm;
import ru.sbt.task.views.forms.PointForm;
import ru.sbt.task.views.forms.ProcurationForm;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private final ContractRepository contractRepository;
    private final EmployeeForm employeeForm;
    private final PointForm pointForm;
    private final ProcurationForm procurationForm;
    private final Grid<Employee> employeeGrid = new Grid<>();
    private final Grid<Point> pointGrid = new Grid<>();
    private final Grid<Procuration> procurationGrid = new Grid<>();
    private final TextField employeeFilter = new TextField();
    private final TextField pointFilter = new TextField();
    private final TextField procurationFilter = new TextField();
    private final EventBus eventBus;

    @Autowired
    public AdminView(EmployeeRepository employeeRepo,
                     PointRepository pointRepo,
                     ProcurationRepository procurationRepo,
                     ContractRepository contractRepo,
                     EmployeeForm employeeForm,
                     PointForm pointForm,
                     ProcurationForm procurationForm,
                     EventBus eventBus) {
        this.employeeRepository = employeeRepo;
        this.pointRepository = pointRepo;
        this.procurationRepository = procurationRepo;
        this.contractRepository = contractRepo;
        this.employeeForm = employeeForm;
        this.pointForm = pointForm;
        this.procurationForm = procurationForm;
        this.eventBus = eventBus;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        initTabs();
        add(tabs);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshAllData();
    }

    private void initTabs() {
        initEmployeeTab();
        initPointTab();
        initProcurationTab();
        initReportsTab();

        tabs.addSelectedChangeListener(event -> {
            employeeForm.setVisible(false);
            pointForm.setVisible(false);
            procurationForm.setVisible(false);
            employeeGrid.asSingleSelect().clear();
            pointGrid.asSingleSelect().clear();
            procurationGrid.asSingleSelect().clear();

            if (event.getSelectedTab().getLabel().equals("Сотрудники")) {
                employeeForm.loadProcurations();
            }
        });

        tabs.setWidthFull();
        tabs.getStyle().set("margin", "0 auto");
    }

    private void initReportsTab() {
        VerticalLayout reportsLayout = new VerticalLayout();
        reportsLayout.setSizeFull();
        reportsLayout.setPadding(true);
        reportsLayout.setSpacing(true);

        Button reportBtn = new Button("Сводный отчет по точкам выдачи", e -> showSummaryReport());

        HorizontalLayout toolbar = new HorizontalLayout(reportBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        reportsLayout.add(toolbar);

        tabs.add(new Tab("Отчеты"), reportsLayout);
    }

    private void showSummaryReport() {
        List<Point> points = pointRepository.findAll();

        Grid<List<String>> reportGrid = new Grid<>();
        reportGrid.setWidthFull();

        reportGrid.addColumn(items -> items.get(0)).setHeader("Точка").setWidth("200px");
        reportGrid.addColumn(items -> items.get(1)).setHeader("Всего договоров").setWidth("150px");
        reportGrid.addColumn(items -> items.get(2)).setHeader("Активные договоры").setWidth("150px");
        reportGrid.addColumn(items -> items.get(3)).setHeader("Сумма активных").setWidth("150px");
        reportGrid.addColumn(items -> items.get(4)).setHeader("Неактивные договоры").setWidth("150px");
        reportGrid.addColumn(items -> items.get(5)).setHeader("Сумма неактивных").setWidth("150px");

        List<List<String>> reportData = new ArrayList<>();

        for (Point point : points) {
            List<Contract> allContracts = contractRepository.findByPoint(point);
            List<Contract> activeContracts = contractRepository.findByPointAndStatus(point, "Активен");
            List<Contract> inactiveContracts = contractRepository.findByPointAndStatus(point, "Неактивен");

            BigDecimal activeSum = activeContracts.stream()
                    .map(Contract::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal inactiveSum = inactiveContracts.stream()
                    .map(Contract::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<String> row = new ArrayList<>();
            row.add(point.getName());
            row.add(String.valueOf(allContracts.size()));
            row.add(String.valueOf(activeContracts.size()));
            row.add(activeSum.toString());
            row.add(String.valueOf(inactiveContracts.size()));
            row.add(inactiveSum.toString());

            reportData.add(row);
        }

        reportGrid.setItems(reportData);

        Dialog reportDialog = new Dialog();
        reportDialog.setWidth("90%");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Span title = new Span("Сводный отчет по точкам выдачи:");
        Button closeButton = new Button(VaadinIcon.CLOSE.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(event -> reportDialog.close());

        headerLayout.add(title, closeButton);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setWidthFull();
        dialogLayout.add(headerLayout, reportGrid);

        reportDialog.add(dialogLayout);
        reportDialog.open();
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
                Notification.show("Ошибка загрузки данных: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        }));
    }

    private void initEmployeeTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        configureEmployeeGrid();
        configureEmployeeFilter();

        Button addBtn = new Button("Добавить сотрудника", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> {
            Dialog dialog = new Dialog();
            dialog.setWidth("600px");
            employeeForm.setParentDialog(dialog);
            dialog.add(employeeForm);
            employeeForm.editEmployee(new Employee());
            employeeForm.setSaveHandler(() -> {
                employeeRepository.save(employeeForm.getEmployee());
                refreshEmployeeData();
                eventBus.post(new DataChangedEvent(Employee.class));
            });
            dialog.open();
        });

        Button refreshBtn = new Button("Обновить", VaadinIcon.REFRESH.create());
        refreshBtn.addClickListener(e -> refreshEmployeeData());

        HorizontalLayout toolbar = new HorizontalLayout(employeeFilter, addBtn, refreshBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.add(toolbar, employeeGrid);
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
            editBtn.addClickListener(e -> {
                Dialog dialog = new Dialog();
                dialog.setWidth("600px");
                employeeForm.setParentDialog(dialog);
                dialog.add(employeeForm);
                employeeForm.editEmployee(employee);
                employeeForm.setSaveHandler(() -> {
                    employeeRepository.save(employeeForm.getEmployee());
                    refreshEmployeeData();
                    eventBus.post(new DataChangedEvent(Employee.class));
                });
                dialog.open();
            });

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> confirmEmployeeDeletion(employee));

            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Действия").setWidth("150px");
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
            Notification.show("Ошибка удаления: " + e.getMessage(), 3000, Notification.Position.BOTTOM_END);
        }
    }

    private void refreshEmployeeData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                employeeGrid.setItems(employeeRepository.findAll());
                employeeFilter.clear();
            } catch (Exception e) {
                Notification.show("Ошибка при обновлении данных сотрудников: " + e.getMessage(), 3000, Notification.Position.BOTTOM_END);
            }
        }));
    }

    private void initPointTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        configurePointGrid();
        configurePointFilter();

        Button addBtn = new Button("Добавить точку", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> {
            Dialog dialog = new Dialog();
            dialog.setWidth("600px");
            pointForm.setParentDialog(dialog);
            dialog.add(pointForm);
            pointForm.editPoint(new Point());
            pointForm.setSaveHandler(() -> {
                pointRepository.save(pointForm.getPoint());
                refreshPointData();
                eventBus.post(new DataChangedEvent(Point.class));
            });
            dialog.open();
        });

        Button refreshBtn = new Button("Обновить", VaadinIcon.REFRESH.create());
        refreshBtn.addClickListener(e -> refreshPointData());

        HorizontalLayout toolbar = new HorizontalLayout(pointFilter, addBtn, refreshBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.add(toolbar, pointGrid);
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
            editBtn.addClickListener(e -> {
                Dialog dialog = new Dialog();
                dialog.setWidth("600px");
                pointForm.setParentDialog(dialog);
                dialog.add(pointForm);
                pointForm.editPoint(point);
                pointForm.setSaveHandler(() -> {
                    pointRepository.save(pointForm.getPoint());
                    refreshPointData();
                    eventBus.post(new DataChangedEvent(Point.class));
                });
                dialog.open();
            });

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
            Notification.show("Ошибка удаления: " + e.getMessage(), 3000, Notification.Position.BOTTOM_END);
        }
    }

    private void refreshPointData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                pointGrid.setItems(pointRepository.findAll());
                pointFilter.clear();
            } catch (Exception e) {
                Notification.show("Ошибка загрузки точек: " + e.getMessage(), 3000, Notification.Position.BOTTOM_END);
            }
        }));
    }

    private void initProcurationTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        configureProcurationGrid();
        configureProcurationFilter();

        Button addBtn = new Button("Добавить доверенность", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> {
            Dialog dialog = new Dialog();
            dialog.setWidth("600px");
            procurationForm.setParentDialog(dialog);
            dialog.add(procurationForm);
            procurationForm.editProcuration(new Procuration());
            procurationForm.setSaveHandler(() -> {
                procurationRepository.save(procurationForm.getProcuration());
                refreshProcurationData();
                // Доверенности влияют на сотрудников
                eventBus.post(new DataChangedEvent(Employee.class));
            });
            dialog.open();
        });

        Button refreshBtn = new Button("Обновить", VaadinIcon.REFRESH.create());
        refreshBtn.addClickListener(e -> refreshProcurationData());

        HorizontalLayout toolbar = new HorizontalLayout(procurationFilter, addBtn, refreshBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.add(toolbar, procurationGrid);
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
            editBtn.addClickListener(e -> {
                Dialog dialog = new Dialog();
                dialog.setWidth("600px");
                procurationForm.setParentDialog(dialog);
                dialog.add(procurationForm);
                procurationForm.editProcuration(procuration);
                procurationForm.setSaveHandler(() -> {
                    procurationRepository.save(procurationForm.getProcuration());
                    refreshProcurationData();
                    // Доверенности влияют на сотрудников
                    eventBus.post(new DataChangedEvent(Employee.class));
                });
                dialog.open();
            });

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
            Notification.show("Ошибка удаления: " + e.getMessage(), 3000, Notification.Position.BOTTOM_END);
        }
    }

    private void refreshProcurationData() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                procurationGrid.setItems(procurationRepository.findAll());
                procurationFilter.clear();
            } catch (Exception e) {
                Notification.show("Ошибка загрузки доверенностей: " + e.getMessage(), 3000, Notification.Position.BOTTOM_END);
            }
        }));
    }
}