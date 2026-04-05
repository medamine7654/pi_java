module tn.piapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens tn.piapp.ui to javafx.fxml;
    opens tn.piapp.model to javafx.base;

    exports tn.piapp.ui;
    exports tn.piapp.model;
    exports tn.piapp.dao;
    exports tn.piapp.db;
    exports tn.piapp.util;
}
