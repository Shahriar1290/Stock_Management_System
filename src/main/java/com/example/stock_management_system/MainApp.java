package com.example.stock_management_system;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.image.Image;


public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/stock_management_system/LoginPage.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Stock Management System");
        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/images/Logo.png"))
        );

        stage.setScene(scene);
        stage.show();
        System.out.println(
                com.example.stock_management_system.PasswordUtil
                        .hashPassword("2207005")
        );
    }


    public static void main(String[] args) {
        launch(args);
    }
}
