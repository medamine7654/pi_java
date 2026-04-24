package tn.piapp.ui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.piapp.dao.CategoryDao;
import tn.piapp.dao.ServiceDao;
import tn.piapp.model.Category;
import tn.piapp.model.Service;
import tn.piapp.service.CategorySuggestionResult;
import tn.piapp.service.CategorySuggestionService;
import tn.piapp.service.PriceSuggestionResult;
import tn.piapp.service.PriceSuggestionService;
import tn.piapp.util.Validation;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ServiceFormDialog {

    private final Stage stage;
    private Service result;

    private final TextField tfName           = new TextField();
    private final TextArea  taDescription    = new TextArea();
    private final TextField tfBasePrice      = new TextField();
    private final TextField tfDurationMinutes = new TextField();
    private final TextField tfLocation       = new TextField();
    private final TextField tfImageName      = new TextField();
    private final ComboBox<Category> cbCategory = new ComboBox<>();
    private final Label lblError             = new Label();
    private final Label lblPriceSuggestion   = new Label();

    private final ServiceDao serviceDao = new ServiceDao();
    private final PriceSuggestionService priceSuggestionService = new PriceSuggestionService();
    private final CategorySuggestionService categorySuggestionService = new CategorySuggestionService();

    // Loaded categories — kept for category suggestion
    private List<Category> loadedCategories = List.of();

    // Last category suggestion — used by Apply button
    private CategorySuggestionResult lastSuggestion = null;

    public ServiceFormDialog(Service existing) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setTitle(existing == null ? "Add Service" : "Edit Service");

        String css = getClass().getResource("/tn/piapp/ui/styles.css").toExternalForm();

        // Style fields
        tfName.getStyleClass().add("dialog-text-field");
        taDescription.getStyleClass().add("dialog-text-area");
        taDescription.setPrefRowCount(3);
        taDescription.setWrapText(true);
        tfBasePrice.getStyleClass().add("dialog-text-field");
        tfDurationMinutes.getStyleClass().add("dialog-text-field");
        tfLocation.getStyleClass().add("dialog-text-field");
        tfImageName.getStyleClass().add("dialog-text-field");
        cbCategory.getStyleClass().add("dialog-text-field");
        cbCategory.setMaxWidth(Double.MAX_VALUE);

        lblError.getStyleClass().add("dialog-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Load categories
        loadCategories(existing);

        // Pre-populate fields in edit mode
        if (existing != null) {
            tfName.setText(existing.getName() != null ? existing.getName() : "");
            taDescription.setText(existing.getDescription() != null ? existing.getDescription() : "");
            tfBasePrice.setText(existing.getBasePrice() != null ? existing.getBasePrice().toPlainString() : "");
            tfDurationMinutes.setText(String.valueOf(existing.getDurationMinutes()));
            tfLocation.setText(existing.getLocation() != null ? existing.getLocation() : "");
            tfImageName.setText(existing.getImageName() != null ? existing.getImageName() : "");
        }

        // Form grid
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 8, 20));

        addRow(grid, 0, "Name",            tfName);
        addRow(grid, 1, "Description",     taDescription);
        addRow(grid, 2, "Base Price",      tfBasePrice);
        addRow(grid, 3, "Duration (min)",  tfDurationMinutes);
        addRow(grid, 4, "Location",        tfLocation);
        addRow(grid, 5, "Image Name",      tfImageName);
        addRow(grid, 6, "Category",        cbCategory);

        // Price suggestion label (below category row)
        lblPriceSuggestion.getStyleClass().add("price-suggestion-label");
        lblPriceSuggestion.setVisible(false);
        lblPriceSuggestion.setManaged(false);
        lblPriceSuggestion.setWrapText(true);
        grid.add(lblPriceSuggestion, 1, 7);

        // Wire price suggestion on category selection
        cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                lblPriceSuggestion.setVisible(false);
                lblPriceSuggestion.setManaged(false);
                return;
            }
            Task<java.util.List<java.math.BigDecimal>> priceTask = new Task<>() {
                @Override protected java.util.List<java.math.BigDecimal> call() throws Exception {
                    return serviceDao.getPricesByCategory(newVal.getId());
                }
            };
            priceTask.setOnSucceeded(e -> {
                java.util.List<java.math.BigDecimal> prices = priceTask.getValue();
                if (prices.isEmpty()) {
                    lblPriceSuggestion.setText("No price data available for this category.");
                } else {
                    PriceSuggestionResult r = priceSuggestionService.suggest(prices);
                    lblPriceSuggestion.setText(String.format(
                        "Suggested: %.2f  (min %.2f · max %.2f · avg %.2f · %d listings)",
                        r.getSuggested(), r.getMin(), r.getMax(), r.getMean(), r.getCount()));
                }
                lblPriceSuggestion.setVisible(true);
                lblPriceSuggestion.setManaged(true);
            });
            priceTask.setOnFailed(e -> {
                lblPriceSuggestion.setText("Could not load price data.");
                lblPriceSuggestion.setVisible(true);
                lblPriceSuggestion.setManaged(true);
            });
            Thread t = new Thread(priceTask);
            t.setDaemon(true);
            t.start();
        });

        // Category suggestion label + Apply button
        Label lblCategorySuggestion = new Label();
        lblCategorySuggestion.getStyleClass().add("category-suggestion-label");
        lblCategorySuggestion.setVisible(false);
        lblCategorySuggestion.setManaged(false);

        Button btnApplySuggestion = new Button("Apply");
        btnApplySuggestion.getStyleClass().add("btn-apply-suggestion");
        btnApplySuggestion.setVisible(false);
        btnApplySuggestion.setManaged(false);
        btnApplySuggestion.setOnAction(e -> {
            if (lastSuggestion != null && lastSuggestion.hasMatch()) {
                cbCategory.setValue(lastSuggestion.getCategory());
            }
        });

        HBox suggestionRow = new HBox(6, lblCategorySuggestion, btnApplySuggestion);
        suggestionRow.setStyle("-fx-alignment: center-left;");
        grid.add(suggestionRow, 1, 8);

        // Wire category suggestion on name/description typing
        javafx.beans.value.ChangeListener<String> suggestionListener = (obs, o, n) -> {
            String combined = tfName.getText() + " " + taDescription.getText();
            lastSuggestion = categorySuggestionService.suggest(combined, loadedCategories);
            if (lastSuggestion.hasMatch()) {
                lblCategorySuggestion.setText(
                    "Suggested: " + lastSuggestion.getCategory().getName()
                    + " (" + lastSuggestion.getConfidence() + "% match)");
                lblCategorySuggestion.setVisible(true);
                lblCategorySuggestion.setManaged(true);
                btnApplySuggestion.setVisible(true);
                btnApplySuggestion.setManaged(true);
            } else {
                lblCategorySuggestion.setVisible(false);
                lblCategorySuggestion.setManaged(false);
                btnApplySuggestion.setVisible(false);
                btnApplySuggestion.setManaged(false);
            }
        };
        tfName.textProperty().addListener(suggestionListener);
        taDescription.textProperty().addListener(suggestionListener);

        // Buttons
        Button btnSave   = new Button("Save");
        Button btnCancel = new Button("Cancel");
        btnSave.getStyleClass().add("btn-save");
        btnCancel.getStyleClass().add("btn-cancel");
        btnSave.setOnAction(e -> onSave(existing));
        btnCancel.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(10, btnCancel, btnSave);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 20, 16, 20));

        // Title strip
        Label title = new Label(existing == null ? "Add Service" : "Edit Service");
        title.getStyleClass().add("dialog-title-label");

        Pane strip = new Pane();
        strip.getStyleClass().add("dialog-gradient-strip");
        strip.setMinHeight(4);
        strip.setMaxHeight(4);

        VBox root = new VBox(strip, title, grid, lblError, btnRow);
        root.getStyleClass().add("dialog-root");

        Scene scene = new Scene(root, 440, 460);
        scene.getStylesheets().add(css);
        stage.setScene(scene);
    }

    private void loadCategories(Service existing) {
        try {
            loadedCategories = new CategoryDao().findByType("service");
            cbCategory.getItems().setAll(loadedCategories);

            // Pre-select matching category in edit mode
            if (existing != null && existing.getCategoryId() != null) {
                loadedCategories.stream()
                        .filter(c -> c.getId() == existing.getCategoryId())
                        .findFirst()
                        .ifPresent(cbCategory::setValue);
            }
        } catch (SQLException e) {
            // Leave ComboBox empty on error — not a blocking failure
            lblError.setText("Could not load categories.");
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }

    private void addRow(GridPane grid, int row, String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("dialog-field-label");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void onSave(Service existing) {
        BigDecimal basePrice;
        int durationMinutes;

        try {
            basePrice = new BigDecimal(tfBasePrice.getText().trim());
        } catch (NumberFormatException e) {
            showError("Base price must be a valid number.");
            return;
        }
        try {
            durationMinutes = Integer.parseInt(tfDurationMinutes.getText().trim());
        } catch (NumberFormatException e) {
            showError("Duration must be a valid integer.");
            return;
        }

        Service s = new Service();
        if (existing != null) {
            s.setId(existing.getId());
            s.setCreatedAt(existing.getCreatedAt());
            s.setHostId(existing.getHostId());
            s.setActive(existing.isActive()); // preserve existing active state on edit
        }
        s.setName(tfName.getText().trim());
        s.setDescription(taDescription.getText().trim());
        s.setBasePrice(basePrice);
        s.setDurationMinutes(durationMinutes);
        s.setLocation(tfLocation.getText().trim());
        s.setImageName(tfImageName.getText().trim());
        s.setCategoryId(cbCategory.getValue() != null ? cbCategory.getValue().getId() : null);

        String error = Validation.validateService(s);
        if (error != null) {
            showError(error);
            return;
        }

        result = s;
        stage.close();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    /** Exposes loaded categories for use by category suggestion (added in a later task). */
    public List<Category> getLoadedCategories() {
        return loadedCategories;
    }

    public Optional<Service> showAndWait() {
        stage.showAndWait();
        return Optional.ofNullable(result);
    }

    public Service getResult() {
        return result;
    }
}
