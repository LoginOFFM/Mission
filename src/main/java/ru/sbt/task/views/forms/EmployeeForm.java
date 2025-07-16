package ru.sbt.task.views.forms;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Employee;
import ru.sbt.task.model.entity.Procuration;
import ru.sbt.task.model.repository.ProcurationRepository;

import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EmployeeForm extends FormLayout {

    private final TextField fullName = new TextField("ФИО");
    private final ComboBox<Procuration> procuration = new ComboBox<>("Доверенность");
    private final TextField login = new TextField("Логин");
    private final PasswordField password = new PasswordField("Пароль");
    private final PasswordField confirmPassword = new PasswordField("Подтвердите пароль");
    private final ComboBox<String> role = new ComboBox<>("Роль");
    private final Button save = new Button("Сохранить");
    private final Button cancel = new Button("Отмена");

    private final Binder<Employee> binder = new Binder<>(Employee.class);
    private final PasswordEncoder passwordEncoder;
    private final ProcurationRepository procurationRepo;

    public Dialog getParentDialog() {
        return parentDialog;
    }

    private Dialog parentDialog;
    private Employee employee;
    private boolean isNewEmployee;
    public Runnable saveHandler;

    @Autowired
    public EmployeeForm(ProcurationRepository procurationRepo,
                        PasswordEncoder passwordEncoder) {
        this.procurationRepo = procurationRepo;
        this.passwordEncoder = passwordEncoder;

        configureFields();
        configureLayout();
        configureBinder();
        setupButtons();
        setVisible(false);

        loadProcurations();
    }

    public void loadProcurations() {
        try {
            List<Procuration> procurations = procurationRepo.findAll();
            procuration.setItems(procurations);
            procuration.setItemLabelGenerator(p -> p != null ? p.getNumber() : "");
        } catch (Exception e) {
            Notification.show("Ошибка загрузки доверенностей",
                    3000, Notification.Position.BOTTOM_END);
        }
    }
    public void setParentDialog(Dialog dialog) {
        this.parentDialog = dialog;
    }
    private void configureFields() {
        role.setItems("ADMIN", "USER");

        password.setPlaceholder("Введите пароль");
        password.setHelperText("Минимум 6 символов");
        password.setRequiredIndicatorVisible(true);

        confirmPassword.setPlaceholder("Повторите пароль");
    }

    private void configureLayout() {
        setWidth("25em");
        add(fullName, login, password, confirmPassword, role, procuration);
        add(createButtonsLayout());
    }

    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        return buttons;
    }

    private void configureBinder() {
        binder.forField(fullName)
                .asRequired("Введите ФИО")
                .bind(Employee::getFullName, Employee::setFullName);

        binder.forField(login)
                .asRequired("Введите логин")
                .bind(Employee::getLogin, Employee::setLogin);

        binder.forField(role)
                .asRequired("Выберите роль")
                .bind(Employee::getRole, Employee::setRole);

        binder.forField(procuration)
                .bind(Employee::getProcuration, Employee::setProcuration);

        binder.forField(password)
                .withValidator(pass -> !isNewEmployee || (pass != null && pass.length() >= 6),
                        "Для нового сотрудника пароль обязателен (мин. 6 символов)")
                .withValidator(pass -> pass == null || pass.isEmpty() || pass.length() >= 6,
                        "Пароль должен быть не менее 6 символов")
                .bind(
                        emp -> "",
                        (emp, pass) -> {
                            if (pass != null && !pass.isEmpty()) {
                                emp.setPassword(passwordEncoder.encode(pass));
                            }
                        }
                );

        binder.forField(confirmPassword)
                .withValidator(
                        confPass -> password.getValue().equals(confPass),
                        "Пароли не совпадают"
                )
                .bind(emp -> "", (emp, confPass) -> {});
    }

    private void setupButtons() {
        save.addClickListener(e -> save());
        cancel.addClickListener(e -> cancel());
    }

    public void editEmployee(Employee employee) {
        this.employee = employee != null ? employee : new Employee();
        this.isNewEmployee = employee == null || employee.getId() == null;
        loadProcurations();
        password.setPlaceholder(isNewEmployee ? "Введите пароль" : "Оставьте пустым, чтобы не менять");
        password.setRequiredIndicatorVisible(isNewEmployee);

        password.setValue("");
        confirmPassword.setValue("");

        binder.setBean(this.employee);
        setVisible(true);
    }

    private void save() {
        try {
            if (isNewEmployee && (password.getValue() == null || password.getValue().isEmpty())) {
                Notification.show("Для нового сотрудника необходимо указать пароль",
                        3000, Notification.Position.MIDDLE);
                return;
            }

            if (!binder.writeBeanIfValid(employee)) {
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
    public void setSaveHandler(Runnable saveHandler) {
        this.saveHandler = saveHandler;
    }
    public Employee getEmployee() {
        return employee;
    }
    private void cancel() {
        if (parentDialog != null) {
            parentDialog.close();
        }
        binder.readBean(employee);
    }
}