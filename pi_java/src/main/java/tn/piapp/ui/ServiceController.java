package tn.piapp.ui;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.piapp.dao.ServiceDao;
import tn.piapp.model.Service;
import tn.piapp.util.Alerts;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ServiceController {

    @FXML private TableView<Service> tableView;
    @FXML private TableColumn<Service, String> colName;
    @FXML private TableColumn<Service, String> colDescription;
    @FXML private TableColumn<Service, BigDecimal> colBasePrice;
    @FXML private TableColumn<Service, Integer> colDurationMinutes;
    @FXML private TableColumn<Service, String> colLocation;
    @FXML private TableColumn<Service, Boolean> colIsActive;
    @FXML private TableColumn<Service, String> colImageName;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblStatus;

    private final ServiceDao dao = new ServiceDao();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colBasePrice.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
        colDurationMinutes.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colIsActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colImageName.setCellValueFactory(new PropertyValueFactory<>("imageName"));

        btnEdit.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        progressIndicator.setVisible(false);
        loadData();
    }

    private void loadData() {
        Task<List<Service>> task = new Task<>() {
            @Override
            protected List<Service> call() throws Exception {
                return dao.findAll();
            }
        };
        task.setOnSucceeded(e -> {
            List<Service> items = task.getValue();
            tableView.setItems(FXCollections.observableArrayList(items));
            progressIndicator.setVisible(false);
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            lblStatus.setText("Loaded " + items.size() + " services · Last refreshed at " + time);
        });
        task.setOnFailed(e -> {
            Alerts.showError("Load Error", task.getException().getMessage());
            progressIndicator.setVisible(false);
            lblStatus.setText("Failed to load data.");
        });
        progressIndicator.setVisible(true);
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onAdd() {
        ServiceFormDialog dialog = new ServiceFormDialog(null);
        Optional<Service> result = dialog.showAndWait();
        result.ifPresent(service -> {
            try {
                dao.insert(service);
                loadData();
            } catch (SQLException e) {
                Alerts.showError("Insert Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        Service selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ServiceFormDialog dialog = new ServiceFormDialog(selected);
        Optional<Service> result = dialog.showAndWait();
        result.ifPresent(service -> {
            try {
                dao.update(service);
                loadData();
            } catch (SQLException e) {
                Alerts.showError("Update Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        Service selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        boolean confirmed = Alerts.showConfirmation("Delete", "Delete this service?");
        if (confirmed) {
            try {
                dao.delete(selected.getId());
                loadData();
            } catch (SQLException e) {
                Alerts.showError("Delete Error", e.getMessage());
            }
        }
    }

    @FXML
    private void onRefresh() {
        loadData();
    }
}
