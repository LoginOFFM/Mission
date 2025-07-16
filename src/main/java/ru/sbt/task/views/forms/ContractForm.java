package ru.sbt.task.views.forms;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToBigDecimalConverter;
import java.math.BigDecimal;

import com.vaadin.flow.data.provider.ListDataView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Client;
import ru.sbt.task.model.entity.Contract;
import ru.sbt.task.model.entity.Employee;
import ru.sbt.task.model.entity.Point;
import ru.sbt.task.model.repository.ClientRepository;
import ru.sbt.task.model.repository.EmployeeRepository;
import ru.sbt.task.model.repository.PointRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.sbt.task.service.DataChangedEvent;
import ru.sbt.task.service.SecurityService;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ContractForm extends FormLayout {

    private static final Logger logger = LoggerFactory.getLogger(ContractForm.class);

    // UI Components
    private final ComboBox<Client> clientCombo = new ComboBox<>("Клиент");
    private final TextField amount = new TextField("Сумма");
    private final DatePicker term = new DatePicker("Срок до");
    private final ComboBox<Employee> employeeCombo = new ComboBox<>("Сотрудник");
    private final ComboBox<Point> pointCombo = new ComboBox<>("Точка выдачи");
    private final ComboBox<String> statusCombo = new ComboBox<>("Статус");
    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");

    // Data
    private final Binder<Contract> binder = new Binder<>(Contract.class);
    private Contract contract;
    private Dialog parentDialog;
    private Runnable saveHandler;

    // Services
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final PointRepository pointRepository;
    private final ClientForm clientForm;
    private final SecurityService securityService;
    private final EventBus eventBus;

    @Autowired
    public ContractForm(ClientRepository clientRepository,
                        EmployeeRepository employeeRepository,
                        PointRepository pointRepository,
                        ClientForm clientForm,
                        SecurityService securityService,
                        EventBus eventBus) {
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.pointRepository = pointRepository;
        this.clientForm = clientForm;
        this.securityService = securityService;
        this.eventBus = eventBus;

        configureForm();
        refreshComboBoxes();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        eventBus.register(this);
        setCurrentEmployee();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        eventBus.unregister(this);
    }


    private void configureForm() {
        setWidth("800px");

        clientCombo.setItemLabelGenerator(Client::getFullName);
        employeeCombo.setItemLabelGenerator(Employee::getFullName);
        pointCombo.setItemLabelGenerator(Point::getName);

        Button addClientBtn = new Button(VaadinIcon.PLUS.create());
        addClientBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        addClientBtn.addClickListener(e -> showClientForm());
        clientCombo.setPrefixComponent(addClientBtn);

        amount.setWidthFull();
        term.setWidthFull();
        employeeCombo.setWidthFull();
        pointCombo.setWidthFull();
        statusCombo.setWidthFull();

        statusCombo.setItems("Активен", "Закрыт");
        statusCombo.setValue("Активен");
        term.setValue(LocalDate.now().plusMonths(1));

        configureBinder();

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> save());
        cancel.addClickListener(e -> {
            if (parentDialog != null) parentDialog.close();
        });

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        add(clientCombo, amount, term, employeeCombo, pointCombo, statusCombo, buttons);
    }

    private void configureBinder() {
        binder.forField(amount)
                .withConverter(new StringToBigDecimalConverter("Введите число"))
                .withValidator(Objects::nonNull, "Укажите сумму")
                .withValidator(value -> value.compareTo(BigDecimal.ZERO) > 0,
                        "Сумма должна быть положительной")
                .bind(Contract::getAmount, Contract::setAmount);

        binder.forField(clientCombo)
                .asRequired("Выберите клиента")
                .bind(Contract::getClient, Contract::setClient);

        binder.forField(employeeCombo)
                .asRequired("Выберите сотрудника")
                .bind(Contract::getEmployee, Contract::setEmployee);

        binder.forField(pointCombo)
                .asRequired("Выберите точку выдачи")
                .bind(Contract::getPoint, Contract::setPoint);

        binder.forField(statusCombo)
                .asRequired("Укажите статус")
                .bind(Contract::getStatus, Contract::setStatus);

        binder.forField(term)
                .asRequired("Укажите срок")
                .withValidator(date -> date.isAfter(LocalDate.now()) || date.isEqual(LocalDate.now()),
                        "Дата должна быть сегодня или позже")
                .bind(Contract::getTerm, Contract::setTerm);
    }

    private void refreshComboBoxes() {
        refreshClients();
        refreshEmployees();
        refreshPoints();
    }

    private void refreshClients() {
        try {
            List<Client> clients = clientRepository.findAll();
            clientCombo.setItems(clients);
            logger.info("Refreshed clients list: {} items", clients.size());
        } catch (Exception e) {
            logger.error("Error refreshing clients", e);
        }
    }

    private void refreshEmployees() {
        try {
            List<Employee> employees = employeeRepository.findAll();
            employeeCombo.setItems(employees);
            logger.info("Refreshed employees list: {} items", employees.size());
        } catch (Exception e) {
            logger.error("Error refreshing employees", e);
        }
    }

    private void refreshPoints() {
        try {
            List<Point> points = pointRepository.findAll();
            pointCombo.setItems(points);
            logger.info("Refreshed points list: {} items", points.size());
        } catch (Exception e) {
            logger.error("Error refreshing points", e);
        }
    }

    private void setCurrentEmployee() {
        try {
            Employee current = securityService.getCurrentEmployee();
            if (current != null) {
                // Проверяем наличие сотрудника в списке
                boolean exists = employeeCombo.getListDataView().getItems()
                        .anyMatch(e -> e.getId().equals(current.getId()));

                if (!exists) {
                    refreshEmployees();
                }
                employeeCombo.setValue(current);
            }
        } catch (Exception e) {
            logger.error("Error setting current employee", e);
        }
    }
    private void showClientForm() {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        Client newClient = new Client();
        clientForm.setClient(newClient);
        clientForm.setParentDialog(dialog);
        clientForm.setSaveHandler(() -> {
            Client savedClient = clientRepository.save(clientForm.getClient());
            List<Client> updatedClients = clientRepository.findAll();
            clientCombo.setItems(updatedClients);
            clientCombo.setValue(savedClient);
        });

        dialog.add(clientForm);
        dialog.open();
    }

    public void setContract(Contract contract) {
        this.contract = contract;
        refreshComboBoxes();

        if (contract != null) {
            binder.readBean(contract);
        } else {
            binder.readBean(new Contract());
            setCurrentEmployee();
        }
    }

    public Contract getContract() {
        return contract;
    }

    public void setParentDialog(Dialog dialog) {
        this.parentDialog = dialog;
    }

    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }

    private void save() {
        try {
            if (binder.writeBeanIfValid(contract)) {
                if (saveHandler != null) {
                    saveHandler.run();
                }
                if (parentDialog != null) {
                    parentDialog.close();
                }
                Notification.show("Договор сохранен", 3000, Notification.Position.BOTTOM_END);
            }
        } catch (Exception e) {
            logger.error("Error saving contract", e);
            Notification.show("Ошибка сохранения: " + e.getMessage(), 5000, Notification.Position.BOTTOM_END);
        }
    }
}