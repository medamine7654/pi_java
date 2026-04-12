package tn.piapp.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
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
    @FXML private TextField tfSearch;

    private final ToolDao dao = new ToolDao();
    private final ObservableList<Tool> masterList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPricePerDay.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));
        colStockQuantity.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colIsActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colImageName.setCellValueFactory(new PropertyValueFactory<>("imageName"));

        // Sortable columns
        colName.setSortable(true);
        colDescription.setSortable(true);
        colPricePerDay.setSortable(true);
        colStockQuantity.setSortable(true);
        colLocation.setSortable(true);
        colIsActive.setSortable(true);
        colImageName.setSortable(true);

        // Active badge
        colIsActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(value ? "Active" : "Inactive");
                    badge.getStyleClass().add(value ? "badge-active" : "badge-inactive");
                    HBox box = new HBox(badge);
                    box.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 4 0;");
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // Disable Edit/Delete when nothing selected
        btnEdit.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        // Double-click to edit
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Tool selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) openEditDialog(selected);
            }
        });

        progressIndicator.setVisible(false);

        // Search filter
        FilteredList<Tool> filteredList = new FilteredList<>(masterList, p -> true);
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(tool -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return (tool.getName() != null && tool.getName().toLowerCase().contains(lower))
                    || (tool.getDescription() != null && tool.getDescription().toLowerCase().contains(lower))
                    || (tool.getLocation() != null && tool.getLocation().toLowerCase().contains(lower))
                    || (tool.getPricePerDay() != null && tool.getPricePerDay().toPlainString().contains(lower));
            });
            lblStatus.setText("Showing " + filteredList.size() + " of " + masterList.size() + " tools");
        });

        SortedList<Tool> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);

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
            masterList.setAll(items);
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

    private void openEditDialog(Tool tool) {
        ToolFormDialog dialog = new ToolFormDialog(tool);
        Optional<Tool> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            try {
                dao.update(updated);
                loadData();
            } catch (SQLException e) {
                Alerts.showError("Update Error", e.getMessage());
            }
        });
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
        openEditDialog(selected);
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
        tfSearch.clear();
        loadData();
    }
}
