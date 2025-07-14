package ru.sbt.task.views.forms;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToBigDecimalConverter;
import java.math.BigDecimal;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import ru.sbt.task.model.repository.*;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ContractForm extends FormLayout {

    private static final Logger logger = LoggerFactory.getLogger(ContractForm.class);

    private final Binder<Contract> binder = new Binder<>(Contract.class);
    private Contract contract;

    private final ComboBox<Client> client = new ComboBox<>("Клиент");
    private final TextField amount = new TextField("Сумма");
    private final DatePicker term = new DatePicker("Срок");
    private final ComboBox<Employee> employee = new ComboBox<>("Сотрудник");
    private final ComboBox<Point> point = new ComboBox<>("Точка выдачи");
    private final ComboBox<String> status = new ComboBox<>("Статус");

    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final PointRepository pointRepository;

    private Runnable saveHandler;

    @Autowired
    public ContractForm(ClientRepository clientRepository,
                        EmployeeRepository employeeRepository,
                        PointRepository pointRepository) {
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.pointRepository = pointRepository;

        initFields();
        setupBinder();
        setupLayout();
        setVisible(false);
    }

    private void initFields() {
        // Инициализация с актуальными данными
        refreshComboBoxItems();

        client.setItemLabelGenerator(Client::getFullName);
        client.setRequiredIndicatorVisible(true);
        client.setClearButtonVisible(true);

        employee.setItemLabelGenerator(Employee::getFullName);
        employee.setRequiredIndicatorVisible(true);
        employee.setClearButtonVisible(true);

        point.setItemLabelGenerator(Point::getName);
        point.setRequiredIndicatorVisible(true);
        point.setClearButtonVisible(true);

        status.setItems("Активен", "Закрыт", "Просрочен");
        status.setRequiredIndicatorVisible(true);
        status.setValue("Активен");

        term.setValue(LocalDate.now().plusMonths(1));
    }

    public void refreshComboBoxItems() {
        client.setItems(clientRepository.findAll());
        employee.setItems(employeeRepository.findAll());
        point.setItems(pointRepository.findAll());
    }

    private void setupBinder() {
        binder.forField(amount)
                .withConverter(new StringToBigDecimalConverter("Введите число"))
                .withValidator(Objects::nonNull, "Укажите сумму")
                .withValidator(value -> value.compareTo(BigDecimal.ZERO) > 0,
                        "Сумма должна быть положительной")
                .bind(Contract::getAmount, Contract::setAmount);

        binder.forField(client)
                .asRequired("Выберите клиента")
                .withValidator(Objects::nonNull, "Клиент обязателен")
                .bind(Contract::getClient, Contract::setClient);

        binder.forField(employee)
                .asRequired("Выберите сотрудника")
                .withValidator(Objects::nonNull, "Сотрудник обязателен")
                .bind(Contract::getEmployee, Contract::setEmployee);

        binder.forField(point)
                .asRequired("Выберите точку выдачи")
                .withValidator(Objects::nonNull, "Точка выдачи обязательна")
                .bind(Contract::getPoint, Contract::setPoint);

        binder.forField(status)
                .asRequired("Укажите статус")
                .bind(Contract::getStatus, Contract::setStatus);

        binder.forField(term)
                .asRequired("Укажите срок")
                .withValidator(date -> date.isAfter(LocalDate.now()) || date.isEqual(LocalDate.now()),
                        "Дата должна быть сегодня или позже")
                .bind(Contract::getTerm, Contract::setTerm);

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
    }

    private void setupLayout() {
        setWidth("500px");
        setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        client.setWidthFull();
        amount.setWidthFull();
        term.setWidthFull();
        employee.setWidthFull();
        point.setWidthFull();
        status.setWidthFull();

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> save());

        cancel.addClickListener(e -> {
            setVisible(false);
            if (contract != null) {
                binder.readBean(contract);
            } else {
                clear();
            }
        });

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        add(client, amount, term, employee, point, status, buttons);
    }

    public void setContract(Contract contract) {
        this.contract = contract;
        if (contract != null) {
            binder.readBean(contract);
            client.setValue(contract.getClient());
            employee.setValue(contract.getEmployee());
            point.setValue(contract.getPoint());
            status.setValue(contract.getStatus());
            term.setValue(contract.getTerm());
        }
        setVisible(true);
    }

    public Contract getContract() {
        return contract;
    }

    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }

    public void clear() {
        Contract newContract = new Contract();
        newContract.setStatus("Активен");
        newContract.setTerm(LocalDate.now().plusMonths(1));
        binder.readBean(newContract);
        client.clear();
        employee.clear();
        point.clear();
        status.setValue("Активен");
        term.setValue(LocalDate.now().plusMonths(1));
        amount.clear();
    }

    private void save() {
        try {
            if (binder.writeBeanIfValid(contract)) {
                if (saveHandler != null) {
                    saveHandler.run();
                }
                setVisible(false);
            }
        } catch (Exception e) {
            Notification.show("Ошибка сохранения: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_END);
            logger.error("Error saving contract", e);
        }
    }
}
