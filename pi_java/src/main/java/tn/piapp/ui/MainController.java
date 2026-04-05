package tn.piapp.ui;

import javafx.fxml.FXML;

public class MainController {

    @FXML
    private ServiceController serviceController;

    @FXML
    private ToolController toolController;

    @FXML
    public void initialize() {
        // Sub-controllers are injected via fx:include
    }
}
