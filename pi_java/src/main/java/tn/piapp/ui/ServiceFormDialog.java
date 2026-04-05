package tn.piapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.piapp.model.Service;
import tn.piapp.util.Validation;

import java.math.BigDecimal;
import java.util.Optional;

public class ServiceFormDialog {

    private final Stage stage;
    private Service result;

    private final TextField tfName = new TextField();
    private final TextArea taDescription = new TextArea();
    private final TextField tfBasePrice = new TextField();
    private final TextField tfDurationMinutes = new TextField();
    private final TextField tfLocation = new TextField();
    private final CheckBox cbIsActive = new CheckBox("Active");
    private final TextField tfImageName = new TextField();
    private final Label lblError = new Label();

    public ServiceFormDialog(Service existing) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setTitle(existing == null ? "Add Service" : "Edit Service");

        // Apply CSS
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

        lblError.getStyleClass().add("dialog-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        if (existing != null) {
            tfName.setText(existing.getName() != null ? existing.getName() : "");
            taDescription.setText(existing.getDescription() != null ? existing.getDescription() : "");
            tfBasePrice.setText(existing.getBasePrice() != null ? existing.getBasePrice().toPlainString() : "");
            tfDurationMinutes.setText(String.valueOf(existing.getDurationMinutes()));
            tfLocation.setText(existing.getLocation() != null ? existing.getLocation() : "");
            cbIsActive.setSelected(existing.isActive());
            tfImageName.setText(existing.getImageName() != null ? existing.getImageName() : "");
        }

        // Form grid
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 8, 20));

        addRow(grid, 0, "Name", tfName);
        addRow(grid, 1, "Description", taDescription);
        addRow(grid, 2, "Base Price", tfBasePrice);
        addRow(grid, 3, "Duration (min)", tfDurationMinutes);
        addRow(grid, 4, "Location", tfLocation);
        addRow(grid, 5, "Image Name", tfImageName);

        // Checkbox row
        Label lblActive = new Label("Status");
        lblActive.getStyleClass().add("dialog-field-label");
        grid.add(lblActive, 0, 6);
        grid.add(cbIsActive, 1, 6);

        // Buttons
        Button btnSave = new Button("Save");
        Button btnCancel = new Button("Cancel");
        btnSave.getStyleClass().add("btn-save");
        btnCancel.getStyleClass().add("btn-cancel");
        btnSave.setOnAction(e -> onSave(existing));
        btnCancel.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(10, btnCancel, btnSave);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 20, 16, 20));

        // Title
        Label title = new Label(existing == null ? "Add Service" : "Edit Service");
        title.getStyleClass().add("dialog-title-label");

        // Gradient strip at top
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
        }
        s.setName(tfName.getText().trim());
        s.setDescription(taDescription.getText().trim());
        s.setBasePrice(basePrice);
        s.setDurationMinutes(durationMinutes);
        s.setLocation(tfLocation.getText().trim());
        s.setActive(cbIsActive.isSelected());
        s.setImageName(tfImageName.getText().trim());

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

    public Optional<Service> showAndWait() {
        stage.showAndWait();
        return Optional.ofNullable(result);
    }

    public Service getResult() {
        return result;
    }
}
