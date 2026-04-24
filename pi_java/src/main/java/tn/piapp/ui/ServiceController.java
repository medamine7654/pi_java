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
import tn.piapp.dao.CategoryDao;
import tn.piapp.dao.ServiceDao;
import tn.piapp.model.Category;
import tn.piapp.model.Service;
import tn.piapp.service.QualityScoreResult;
import tn.piapp.service.QualityScoreService;
import tn.piapp.util.Alerts;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ServiceController {

    // ── Table columns ──────────────────────────────────────────────────────────
    @FXML private TableView<Service>               tableView;
    @FXML private TableColumn<Service, String>     colName;
    @FXML private TableColumn<Service, String>     colDescription;
    @FXML private TableColumn<Service, BigDecimal> colBasePrice;
    @FXML private TableColumn<Service, Integer>    colDurationMinutes;
    @FXML private TableColumn<Service, String>     colLocation;
    @FXML private TableColumn<Service, Boolean>    colIsActive;
    @FXML private TableColumn<Service, String>     colImageName;

    // ── Toolbar ────────────────────────────────────────────────────────────────
    @FXML private Button            btnAdd;
    @FXML private Button            btnEdit;
    @FXML private Button            btnDelete;
    @FXML private Button            btnRefresh;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label             lblStatus;
    @FXML private TextField         tfSearch;

    // ── Filter bar ─────────────────────────────────────────────────────────────
    @FXML private RadioButton           rbAll;
    @FXML private RadioButton           rbActiveOnly;
    @FXML private RadioButton           rbPending;
    @FXML private ComboBox<Category>    cbCategoryFilter;
    @FXML private TextField             tfLocationFilter;
    @FXML private TextField             tfMinPrice;
    @FXML private TextField             tfMaxPrice;
    @FXML private ComboBox<String>      cbSortBy;
    @FXML private Button                btnClearFilters;

    private final ToggleGroup tgApproval = new ToggleGroup();

    // ── Data ───────────────────────────────────────────────────────────────────
    private final ServiceDao          dao                 = new ServiceDao();
    private final CategoryDao         categoryDao         = new CategoryDao();
    private final QualityScoreService qualityScoreService = new QualityScoreService();
    private final ObservableList<Service> masterList      = FXCollections.observableArrayList();
    private FilteredList<Service> filteredList;
    private SortedList<Service>   sortedList;

    // Sort options
    private static final String SORT_DATE_DESC  = "Date (Newest First)";
    private static final String SORT_DATE_ASC   = "Date (Oldest First)";
    private static final String SORT_PRICE_DESC = "Price (High to Low)";
    private static final String SORT_PRICE_ASC  = "Price (Low to High)";
    private static final String SORT_NAME_ASC   = "Name (A–Z)";
    private static final String SORT_NAME_DESC  = "Name (Z–A)";

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Column bindings
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colBasePrice.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
        colDurationMinutes.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colIsActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colImageName.setCellValueFactory(new PropertyValueFactory<>("imageName"));

        // Status badge cell
        colIsActive.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(value ? "Active" : "Pending");
                badge.getStyleClass().add(value ? "badge-active" : "badge-inactive");
                HBox box = new HBox(badge);
                box.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 4 0;");
                setGraphic(box); setText(null);
            }
        });

        // Approve / Hide action column
        TableColumn<Service, Void> colActions = new TableColumn<>("ACTIONS");
        colActions.setPrefWidth(110);
        colActions.setSortable(false);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setOnAction(e -> {
                    Service item = getTableView().getItems().get(getIndex());
                    if (item == null) return;
                    try { dao.setActive(item.getId(), !item.isActive()); loadData(); }
                    catch (SQLException ex) { Alerts.showError("Update Error", ex.getMessage()); }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Service item = getTableView().getItems().get(getIndex());
                if (item == null) { setGraphic(null); return; }
                btn.setText(item.isActive() ? "Hide" : "Approve");
                btn.getStyleClass().setAll(item.isActive() ? "btn-hide" : "btn-approve");
                setGraphic(btn);
            }
        });
        tableView.getColumns().add(colActions);

        // Quality score badge column
        TableColumn<Service, Void> colQuality = new TableColumn<>("QUALITY");
        colQuality.setPrefWidth(130);
        colQuality.setSortable(false);
        colQuality.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Service item = getTableView().getItems().get(getIndex());
                if (item == null) { setGraphic(null); return; }
                QualityScoreResult result = qualityScoreService.score(item);
                Label badge = new Label(result.getScore() + "% · " + result.getRating());
                String styleClass = switch (result.getRating()) {
                    case "Excellent" -> "badge-quality-excellent";
                    case "Good"      -> "badge-quality-good";
                    default          -> "badge-quality-poor";
                };
                badge.getStyleClass().add(styleClass);
                badge.setOnMouseClicked(e -> showQualityDetail(result));
                HBox box = new HBox(badge);
                box.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 4 0;");
                setGraphic(box);
            }
        });
        tableView.getColumns().add(colQuality);

        // Approval toggle group
        rbAll.setToggleGroup(tgApproval);
        rbActiveOnly.setToggleGroup(tgApproval);
        rbPending.setToggleGroup(tgApproval);
        rbAll.setSelected(true);
        tgApproval.selectedToggleProperty().addListener((obs, o, n) -> updatePredicate());

        // Sort-by ComboBox
        cbSortBy.getItems().setAll(
            SORT_DATE_DESC, SORT_DATE_ASC,
            SORT_PRICE_DESC, SORT_PRICE_ASC,
            SORT_NAME_ASC, SORT_NAME_DESC);
        cbSortBy.setValue(SORT_DATE_DESC);
        cbSortBy.valueProperty().addListener((obs, o, n) -> applySort());

        // Category filter ComboBox — load async
        cbCategoryFilter.getItems().add(null);
        loadCategoryFilter();
        cbCategoryFilter.valueProperty().addListener((obs, o, n) -> updatePredicate());

        // Location / price filter listeners
        tfLocationFilter.textProperty().addListener((obs, o, n) -> updatePredicate());
        tfMinPrice.textProperty().addListener((obs, o, n) -> updatePredicate());
        tfMaxPrice.textProperty().addListener((obs, o, n) -> updatePredicate());

        // Disable Edit/Delete when nothing selected
        btnEdit.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        // Double-click to edit
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Service selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) openEditDialog(selected);
            }
        });

        progressIndicator.setVisible(false);

        // FilteredList + SortedList pipeline
        filteredList = new FilteredList<>(masterList, p -> true);
        tfSearch.textProperty().addListener((obs, o, n) -> updatePredicate());

        sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);

        loadData();
    }

    // ── Category filter loader ─────────────────────────────────────────────────
    private void loadCategoryFilter() {
        Task<List<Category>> task = new Task<>() {
            @Override protected List<Category> call() throws Exception {
                return categoryDao.findByType("service");
            }
        };
        task.setOnSucceeded(e -> cbCategoryFilter.getItems().addAll(task.getValue()));
        task.setOnFailed(e -> { /* silently ignore */ });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Combined predicate ─────────────────────────────────────────────────────
    private void updatePredicate() {
        String text     = tfSearch.getText();
        Toggle selected = tgApproval.getSelectedToggle();
        Category catFilter = cbCategoryFilter.getValue();
        String locFilter   = tfLocationFilter.getText();

        BigDecimal minPrice = null, maxPrice = null;
        try { minPrice = new BigDecimal(tfMinPrice.getText().trim()); } catch (Exception ignored) {}
        try { maxPrice = new BigDecimal(tfMaxPrice.getText().trim()); } catch (Exception ignored) {}

        final BigDecimal fMin = minPrice, fMax = maxPrice;

        filteredList.setPredicate(service -> {
            // 1. Text search
            if (text != null && !text.isBlank()) {
                String lower = text.toLowerCase();
                boolean textMatch =
                    (service.getName() != null && service.getName().toLowerCase().contains(lower))
                    || (service.getDescription() != null && service.getDescription().toLowerCase().contains(lower))
                    || (service.getLocation() != null && service.getLocation().toLowerCase().contains(lower))
                    || (service.getBasePrice() != null && service.getBasePrice().toPlainString().contains(lower));
                if (!textMatch) return false;
            }
            // 2. Approval toggle
            if (selected == rbActiveOnly && !service.isActive()) return false;
            if (selected == rbPending    &&  service.isActive()) return false;
            // 3. Category filter
            if (catFilter != null) {
                if (service.getCategoryId() == null || service.getCategoryId() != catFilter.getId()) return false;
            }
            // 4. Location filter
            if (locFilter != null && !locFilter.isBlank()) {
                if (service.getLocation() == null || !service.getLocation().toLowerCase().contains(locFilter.toLowerCase())) return false;
            }
            // 5. Min price
            if (fMin != null && service.getBasePrice() != null && service.getBasePrice().compareTo(fMin) < 0) return false;
            // 6. Max price
            if (fMax != null && service.getBasePrice() != null && service.getBasePrice().compareTo(fMax) > 0) return false;

            return true;
        });

        lblStatus.setText("Showing " + filteredList.size() + " of " + masterList.size() + " services");
    }

    // ── Sort ───────────────────────────────────────────────────────────────────
    private void applySort() {
        String option = cbSortBy.getValue();
        if (option == null) return;
        Comparator<Service> comparator = switch (option) {
            case SORT_DATE_ASC   -> Comparator.comparing(Service::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case SORT_PRICE_DESC -> Comparator.comparing(Service::getBasePrice, Comparator.nullsLast(Comparator.reverseOrder()));
            case SORT_PRICE_ASC  -> Comparator.comparing(Service::getBasePrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case SORT_NAME_ASC   -> Comparator.comparing(Service::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case SORT_NAME_DESC  -> Comparator.comparing(Service::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed();
            default              -> Comparator.comparing(Service::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        sortedList.setComparator(comparator);
    }

    // ── Data loading ───────────────────────────────────────────────────────────
    private void loadData() {
        Task<List<Service>> task = new Task<>() {
            @Override protected List<Service> call() throws Exception { return dao.findAll(); }
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

    // ── Dialog helpers ─────────────────────────────────────────────────────────
    private void openEditDialog(Service service) {
        ServiceFormDialog dialog = new ServiceFormDialog(service);
        Optional<Service> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            try { dao.update(updated); loadData(); }
            catch (SQLException e) { Alerts.showError("Update Error", e.getMessage()); }
        });
    }

    // ── Quality score detail popup ─────────────────────────────────────────────
    private void showQualityDetail(QualityScoreResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score: ").append(result.getScore()).append("/100 — ").append(result.getRating()).append("\n\n");
        sb.append("Checklist:\n");
        result.getChecklist().forEach(item -> sb.append("  ").append(item).append("\n"));
        if (!result.getSuggestions().isEmpty()) {
            sb.append("\nSuggestions:\n");
            result.getSuggestions().forEach(s -> sb.append("  • ").append(s).append("\n"));
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quality Score Details");
        alert.setHeaderText("Listing Quality — " + result.getRating());
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    // ── FXML handlers ──────────────────────────────────────────────────────────
    @FXML private void onAdd() {
        ServiceFormDialog dialog = new ServiceFormDialog(null);
        Optional<Service> result = dialog.showAndWait();
        result.ifPresent(service -> {
            try { dao.insert(service); loadData(); }
            catch (SQLException e) { Alerts.showError("Insert Error", e.getMessage()); }
        });
    }

    @FXML private void onEdit() {
        Service selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        openEditDialog(selected);
    }

    @FXML private void onDelete() {
        Service selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        boolean confirmed = Alerts.showConfirmation("Delete", "Delete this service?");
        if (confirmed) {
            try { dao.delete(selected.getId()); loadData(); }
            catch (SQLException e) { Alerts.showError("Delete Error", e.getMessage()); }
        }
    }

    @FXML private void onRefresh() {
        onClearFilters();
        loadData();
    }

    @FXML private void onClearFilters() {
        tfSearch.clear();
        rbAll.setSelected(true);
        cbCategoryFilter.setValue(null);
        tfLocationFilter.clear();
        tfMinPrice.clear();
        tfMaxPrice.clear();
        cbSortBy.setValue(SORT_DATE_DESC);
        sortedList.setComparator(null);
        updatePredicate();
    }
}
