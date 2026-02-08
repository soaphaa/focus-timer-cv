module org.example.focustimercv {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;
    requires java.desktop;


    opens org.example.focustimercv to javafx.fxml;
    exports org.example.focustimercv;
}