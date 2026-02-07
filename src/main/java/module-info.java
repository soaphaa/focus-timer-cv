module org.example.focustimercv {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;


    opens org.example.focustimercv to javafx.fxml;
    exports org.example.focustimercv;
}