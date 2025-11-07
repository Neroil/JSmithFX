package heig.tb.jsmithfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.catwithawand.borderlessscenefx.scene.BorderlessScene;

import java.io.IOException;
import java.util.Objects;

public class JSmithFXApplication extends Application {

    private double x,y = 0;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(JSmithFXApplication.class.getResource("hello-view.fxml"));
        Parent root = fxmlLoader.load();


//        Scene scene = new Scene(root);


        BorderlessScene scene = new BorderlessScene(stage, StageStyle.TRANSPARENT, root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("css.css")).toExternalForm());

        // Set the title bar for moving the window
        try{
            scene.setMoveControl(root.lookup("#titleBar"));
        } catch(Exception e){
            System.out.println("No title bar found for moving the window.");
        }


        stage.initStyle(StageStyle.TRANSPARENT); // Use TRANSPARENT for a transparent background
        stage.setResizable(true);
        stage.setTitle("JSmithFX!");
        stage.setScene(scene);
        stage.show();


    }
}
