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
    @FXML private TextField tfSearch;

    private final ServiceDao dao = new ServiceDao();
    private ObservableList<Service> masterList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Column value factories
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colBasePrice.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
        colDurationMinutes.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colIsActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colImageName.setCellValueFactory(new PropertyValueFactory<>("imageName"));

        // Enable sorting on all columns
        colName.setSortable(true);
        colDescription.setSortable(true);
        colBasePrice.setSortable(true);
        colDurationMinutes.setSortable(true);
        colLocation.setSortable(true);
        colIsActive.setSortable(true);
        colImageName.setSortable(true);

        // Active badge cell factory
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

        progressIndicator.setVisible(false);

        // Wire search filter
        FilteredList<Service> filteredList = new FilteredList<>(masterList, p -> true);
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(service -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return (service.getName() != null && service.getName().toLowerCase().contains(lower))
                    || (service.getDescription() != null && service.getDescription().toLowerCase().contains(lower))
                    || (service.getLocation() != null && service.getLocation().toLowerCase().contains(lower))
                    || (service.getBasePrice() != null && service.getBasePrice().toPlainString().contains(lower));
            });
            lblStatus.setText("Showing " + filteredList.size() + " of " + masterList.size() + " services");
        });

        // Bind sorted list to table (preserves column sort)
        SortedList<Service> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);

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
            masterList.setAll(items);
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
        tfSearch.clear();
        loadData();
    }
}
