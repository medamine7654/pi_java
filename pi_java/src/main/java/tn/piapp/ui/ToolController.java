package tn.piapp.ui;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.piapp.dao.ToolDao;
import tn.piapp.model.Tool;
import tn.piapp.util.Alerts;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ToolController {

    @FXML private TableView<Tool> tableView;
    @FXML private TableColumn<Tool, String> colName;
    @FXML private TableColumn<Tool, String> colDescription;
    @FXML private TableColumn<Tool, BigDecimal> colPricePerDay;
    @FXML private TableColumn<Tool, Integer> colStockQuantity;
    @FXML private TableColumn<Tool, String> colLocation;
    @FXML private TableColumn<Tool, Boolean> colIsActive;
    @FXML private TableColumn<Tool, String> colImageName;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblStatus;

    private final ToolDao dao = new ToolDao();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPricePerDay.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));
        colStockQuantity.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colIsActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colImageName.setCellValueFactory(new PropertyValueFactory<>("imageName"));

        btnEdit.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        progressIndicator.setVisible(false);
        loadData();
    }

    private void loadData() {
        Task<List<Tool>> task = new Task<>() {
            @Override
            protected List<Tool> call() throws Exception {
                return dao.findAll();
            }
        };
        task.setOnSucceeded(e -> {
            List<Tool> items = task.getValue();
            tableView.setItems(FXCollections.observableArrayList(items));
            progressIndicator.setVisible(false);
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            lblStatus.setText("Loaded " + items.size() + " tools · Last refreshed at " + time);
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
        ToolFormDialog dialog = new ToolFormDialog(null);
        Optional<Tool> result = dialog.showAndWait();
        result.ifPresent(tool -> {
            try {
                dao.insert(tool);
                loadData();
            } catch (SQLException e) {
                Alerts.showError("Insert Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        Tool selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ToolFormDialog dialog = new ToolFormDialog(selected);
        Optional<Tool> result = dialog.showAndWait();
        result.ifPresent(tool -> {
            try {
                dao.update(tool);
                loadData();
            } catch (SQLException e) {
                Alerts.showError("Update Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        Tool selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        boolean confirmed = Alerts.showConfirmation("Delete", "Delete this tool?");
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
