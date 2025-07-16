package ru.sbt.task.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;

@Component
@UIScope
@Route(value = "contracts", layout = MainView.class)
@PageTitle("Договоры и клиенты")
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
                .setHeader("Срок до")
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

            Button downloadBtn = new Button(VaadinIcon.DOWNLOAD.create());
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            downloadBtn.addClickListener(e -> downloadPdfContract(contract));

            actions.add(editBtn, deleteBtn, downloadBtn);
            return actions;
        })).setHeader("Действия").setWidth("200px");
    }
    private void downloadPdfContract(Contract contract) {
        try {
            byte[] pdfBytes = generatePdfBytes(contract);
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            UI.getCurrent().getPage().executeJs(
                    "const byteChars = atob($0);" +
                            "const byteNumbers = new Array(byteChars.length);" +
                            "for (let i = 0; i < byteChars.length; i++) {" +
                            "    byteNumbers[i] = byteChars.charCodeAt(i);" +
                            "}" +
                            "const byteArray = new Uint8Array(byteNumbers);" +
                            "const blob = new Blob([byteArray], {type: 'application/pdf'});" +
                            "const link = document.createElement('a');" +
                            "link.href = URL.createObjectURL(blob);" +
                            "link.download = 'contract_" + contract.getId() + ".pdf';" +
                            "link.click();" +
                            "setTimeout(() => URL.revokeObjectURL(link.href), 100);",
                    base64Pdf
            );

        } catch (Exception e) {
            Notification.show("Ошибка: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private byte[] generatePdfBytes(Contract contract) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDFont font;
            try (InputStream fontStream = getClass().getResourceAsStream("/fonts/arial.ttf")) {
                if (fontStream != null) {
                    font = PDType0Font.load(doc, fontStream);
                } else {
                    throw new IOException("Шрифт arial.ttf не найден в resources/fonts/");
                }
            }

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 12);
                cs.setLeading(16f);
                cs.newLineAtOffset(50, 700);
                cs.setFont(font, 14);
                cs.showText("ДОГОВОР №" + contract.getId());
                cs.newLine();
                cs.newLine();
                cs.setFont(font, 12);
                cs.showText("Клиент: " + (contract.getClient() != null ?
                        contract.getClient().getFullName() : "Не указан"));
                cs.newLine();
                cs.showText("Сумма: " + contract.getAmount());
                cs.newLine();
                cs.showText("Срок до: " + contract.getTerm());
                cs.newLine();
                cs.showText("Менеджер: " + (contract.getEmployee() != null ?
                        contract.getEmployee().getFullName() : "Не назначен"));
                cs.newLine();
                cs.showText("Статус: " + contract.getStatus());
                cs.newLine();
                cs.newLine();
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
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
        HorizontalLayout contractToolbar = new HorizontalLayout(contractFilter, addContractBtn, refreshContractBtn);
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
        HorizontalLayout clientToolbar = new HorizontalLayout(clientFilter, addClientBtn, refreshClientBtn);
        clientToolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        clientsLayout.add(clientToolbar, clientGrid, clientForm);

        tabSheet.add(new Tab("Договоры"), contractsLayout);
        tabSheet.add(new Tab("Клиенты"), clientsLayout);
        tabSheet.addSelectedChangeListener(event -> {
            contractForm.setVisible(false);
            clientForm.setVisible(false);
            contractGrid.asSingleSelect().clear();
            clientGrid.asSingleSelect().clear();
            if (event.getSelectedTab().getLabel().equals("Договоры")) {
                contractForm.refreshComboBoxItems();
            }
        });
        tabSheet.setWidthFull();
        tabSheet.getStyle().set("margin", "0 auto");
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
                contractForm.refreshComboBoxItems(); 
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
                contractForm.refreshComboBoxItems();
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
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                contractForm.setContract(contract);
                contractForm.setVisible(true);
            });
        } else {
            contractForm.setContract(contract);
            contractForm.setVisible(true);
        }
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