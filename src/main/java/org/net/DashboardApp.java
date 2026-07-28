package org.net;

import javafx.application.Application;
import javafx.stage.Stage;
import org.net.ui.MainWindow;

public class DashboardApp extends Application {

    @Override
    public void start(Stage stage) {

        MainWindow window = new MainWindow(stage);
        window.show();
    }

    public static void main(String[] args) {
        launch();
    }
}