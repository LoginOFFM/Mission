package ru.sbt.task.views;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.sbt.task.model.entity.Client;
import ru.sbt.task.model.entity.Contract;
import ru.sbt.task.model.repository.ClientRepository;
import ru.sbt.task.model.repository.ContractRepository;
import ru.sbt.task.model.repository.EmployeeRepository;
import ru.sbt.task.model.repository.PointRepository;
import ru.sbt.task.views.forms.ClientForm;
import ru.sbt.task.views.forms.ContractForm;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@UIScope
@Route(value = "contracts", layout = MainView.class)
@PageTitle("Договоры")
public class ClientContractView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ClientContractView.class);

    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final PointRepository pointRepository;
    private final ContractForm contractForm;
    private final ClientForm clientForm;

    private final Grid<Contract> contractGrid = new Grid<>(Contract.class, false);
    private final Grid<Client> clientGrid = new Grid<>(Client.class, false);
    private final TabSheet tabSheet = new TabSheet();

    private final TextField contractFilter = new TextField();
    private final TextField clientFilter = new TextField();

    @Autowired
    public ClientContractView(ContractRepository contractRepository,
                              ClientRepository clientRepository,
                              EmployeeRepository employeeRepository,
                              PointRepository pointRepository,
                              ContractForm contractForm,
                              ClientForm clientForm) {
        this.contractRepository = contractRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.pointRepository = pointRepository;
        this.contractForm = contractForm;
        this.clientForm = clientForm;

        setSizeFull();
        configureGrids();
        setupTabs();
        addListeners();
    }

    private void configureGrids() {
        configureContractGrid();
        configureClientGrid();
    }

    private void configureContractGrid() {
        contractGrid.removeAllColumns();

        contractGrid.addColumn(Contract::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        contractGrid.addColumn(c -> c.getClient() != null ? c.getClient().getFullName() : "")
                .setHeader("Клиент")
                .setSortable(true);

        contractGrid.addColumn(Contract::getAmount)
                .setHeader("Сумма")
                .setSortable(true);

        contractGrid.addColumn(Contract::getTerm)
                .setHeader("Срок")
                .setSortable(true);

        contractGrid.addColumn(c -> c.getEmployee() != null ? c.getEmployee().getFullName() : "")
                .setHeader("Сотрудник");

        contractGrid.addColumn(Contract::getStatus)
                .setHeader("Статус")
                .setSortable(true);

        contractGrid.addColumn(new ComponentRenderer<>(contract -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> editContract(contract));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> confirmContractDeletion(contract));

            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Действия").setWidth("150px");
    }

    private void configureClientGrid() {
        clientGrid.removeAllColumns();

        clientGrid.addColumn(Client::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        clientGrid.addColumn(Client::getFullName)
                .setHeader("ФИО")
                .setSortable(true);

        clientGrid.addColumn(Client::getPhone)
                .setHeader("Телефон")
                .setSortable(true);

        clientGrid.addColumn(new ComponentRenderer<>(client -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> editClient(client));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> confirmClientDeletion(client));

            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Действия").setWidth("150px");
    }

    private void setupTabs() {
        VerticalLayout contractsLayout = new VerticalLayout();
        contractsLayout.setSizeFull();
        contractsLayout.setPadding(true);
        contractsLayout.setSpacing(true);

        configureContractFilter();

        Button addContractBtn = new Button("Добавить договор", VaadinIcon.PLUS.create());
        addContractBtn.addClickListener(e -> addContract());

        Button refreshContractBtn = new Button("Обновить", VaadinIcon.REFRESH.create());
        refreshContractBtn.addClickListener(e -> updateContractList());

        HorizontalLayout contractToolbar = new HorizontalLayout(
                contractFilter, addContractBtn, refreshContractBtn
        );
        contractToolbar.setAlignItems(FlexComponent.Alignment.BASELINE);

        contractsLayout.add(contractToolbar, contractGrid, contractForm);

        VerticalLayout clientsLayout = new VerticalLayout();
        clientsLayout.setSizeFull();
        clientsLayout.setPadding(true);
        clientsLayout.setSpacing(true);

        configureClientFilter();

        Button addClientBtn = new Button("Добавить клиента", VaadinIcon.PLUS.create());
        addClientBtn.addClickListener(e -> addClient());

        Button refreshClientBtn = new Button("Обновить", VaadinIcon.REFRESH.create());
        refreshClientBtn.addClickListener(e -> updateClientList());

        HorizontalLayout clientToolbar = new HorizontalLayout(
                clientFilter, addClientBtn, refreshClientBtn
        );
        clientToolbar.setAlignItems(FlexComponent.Alignment.BASELINE);

        clientsLayout.add(clientToolbar, clientGrid, clientForm);

        tabSheet.add(new Tab("Договоры"), contractsLayout);
        tabSheet.add(new Tab("Клиенты"), clientsLayout);

        add(tabSheet);
        updateLists();
    }

    private void configureContractFilter() {
        contractFilter.setPlaceholder("Фильтр по клиенту...");
        contractFilter.setClearButtonVisible(true);
        contractFilter.setValueChangeMode(ValueChangeMode.LAZY);
        contractFilter.addValueChangeListener(e -> updateContractList());
    }

    private void configureClientFilter() {
        clientFilter.setPlaceholder("Фильтр по ФИО...");
        clientFilter.setClearButtonVisible(true);
        clientFilter.setValueChangeMode(ValueChangeMode.LAZY);
        clientFilter.addValueChangeListener(e -> updateClientList());
    }

    private void addListeners() {
        contractForm.setSaveHandler(() -> {
            try {
                contractRepository.save(contractForm.getContract());
                contractForm.refreshComboBoxItems(); // Обновляем комбобоксы
                updateContractList();
                contractForm.setVisible(false);
                Notification.show("Договор сохранен", 3000, Notification.Position.BOTTOM_END);
                logger.info("Contract saved: {}", contractForm.getContract().getId());
            } catch (Exception e) {
                Notification.show("Ошибка сохранения договора: " + e.getMessage(),
                        5000, Notification.Position.BOTTOM_END);
                logger.error("Error saving contract", e);
            }
        });

        clientForm.setSaveHandler(() -> {
            try {
                clientRepository.save(clientForm.getClient());
                contractForm.refreshComboBoxItems(); // Обновляем комбобоксы в форме договора
                updateClientList();
                clientForm.setVisible(false);
                Notification.show("Клиент сохранен", 3000, Notification.Position.BOTTOM_END);
                logger.info("Client saved: {}", clientForm.getClient().getId());
            } catch (Exception e) {
                Notification.show("Ошибка сохранения клиента: " + e.getMessage(),
                        5000, Notification.Position.BOTTOM_END);
                logger.error("Error saving client", e);
            }
        });
    }

    private void updateLists() {
        updateContractList();
        updateClientList();
    }

    private void updateContractList() {
        try {
            if (contractFilter.getValue().isEmpty()) {
                contractGrid.setItems(contractRepository.findAll());
            } else {
                contractGrid.setItems(contractRepository.findByClientFullNameContainingIgnoreCase(contractFilter.getValue()));
            }
        } catch (Exception e) {
            Notification.show("Ошибка загрузки договоров", 3000, Notification.Position.BOTTOM_END);
            logger.error("Error loading contracts", e);
        }
    }

    private void updateClientList() {
        try {
            if (clientFilter.getValue().isEmpty()) {
                clientGrid.setItems(clientRepository.findAll());
            } else {
                clientGrid.setItems(clientRepository.findByFullNameContainingIgnoreCase(clientFilter.getValue()));
            }
        } catch (Exception e) {
            Notification.show("Ошибка загрузки клиентов", 3000, Notification.Position.BOTTOM_END);
            logger.error("Error loading clients", e);
        }
    }

    private void addContract() {
        contractGrid.asSingleSelect().clear();
        Contract newContract = new Contract();
        newContract.setAmount(BigDecimal.ZERO);
        newContract.setTerm(LocalDate.now().plusMonths(1));
        newContract.setStatus("Активен");
        contractForm.setContract(newContract);
        contractForm.setVisible(true);
    }

    private void editContract(Contract contract) {
        if (contract == null) {
            contractForm.setVisible(false);
            return;
        }
        contractForm.setContract(contract);
        contractForm.setVisible(true);
    }

    private void confirmContractDeletion(Contract contract) {
        if (contract == null) return;

        Dialog confirmDialog = new Dialog();
        confirmDialog.setCloseOnEsc(false);
        confirmDialog.setCloseOnOutsideClick(false);

        Span message = new Span("Вы действительно хотите удалить договор №" + contract.getId() + "?");

        Button confirmBtn = new Button("Удалить", VaadinIcon.TRASH.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmBtn.addClickListener(e -> {
            deleteContract(contract);
            confirmDialog.close();
        });

        Button cancelBtn = new Button("Отмена");
        cancelBtn.addClickListener(e -> confirmDialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmBtn, cancelBtn);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        confirmDialog.add(new VerticalLayout(message, buttons));
        confirmDialog.open();
    }

    private void deleteContract(Contract contract) {
        try {
            contractRepository.delete(contract);
            updateContractList();
            Notification.show("Договор удален", 3000, Notification.Position.BOTTOM_END);
            logger.info("Contract deleted: {}", contract.getId());
        } catch (Exception e) {
            Notification.show("Ошибка удаления: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_END);
            logger.error("Error deleting contract", e);
        }
    }

    private void addClient() {
        clientGrid.asSingleSelect().clear();
        clientForm.setClient(new Client());
        clientForm.setVisible(true);
    }

    private void editClient(Client client) {
        if (client == null) {
            clientForm.setVisible(false);
            return;
        }
        clientForm.setClient(client);
        clientForm.setVisible(true);
    }

    private void confirmClientDeletion(Client client) {
        if (client == null) return;

        Dialog confirmDialog = new Dialog();
        confirmDialog.setCloseOnEsc(false);
        confirmDialog.setCloseOnOutsideClick(false);

        Span message = new Span("Вы действительно хотите удалить клиента " + client.getFullName() + "?");

        Button confirmBtn = new Button("Удалить", VaadinIcon.TRASH.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmBtn.addClickListener(e -> {
            deleteClient(client);
            confirmDialog.close();
        });

        Button cancelBtn = new Button("Отмена");
        cancelBtn.addClickListener(e -> confirmDialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmBtn, cancelBtn);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        confirmDialog.add(new VerticalLayout(message, buttons));
        confirmDialog.open();
    }

    private void deleteClient(Client client) {
        try {
            clientRepository.delete(client);
            updateClientList();
            Notification.show("Клиент удален", 3000, Notification.Position.BOTTOM_END);
            logger.info("Client deleted: {}", client.getId());
        } catch (Exception e) {
            Notification.show("Ошибка удаления: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_END);
            logger.error("Error deleting client", e);
        }
    }
}