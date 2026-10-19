package com.example.stock_management_system;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.getIcons().add(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/images/Logo.png"))));
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        Navigator.init(stage);
        Navigator.showLogin();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
