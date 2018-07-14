package eu.hansolo.fx.gradientpicker;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class Demo extends Application {
    private GradientPicker gradientPicker;

    @Override public void init() {
        gradientPicker = new GradientPicker();
    }

    @Override public void start(Stage stage) {
        StackPane pane = new StackPane(gradientPicker);
        //pane.setPadding(new Insets(10));

        Scene scene = new Scene(pane);

        stage.setTitle("GradientPicker");
        stage.setScene(scene);
        stage.show();
    }

    @Override public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}