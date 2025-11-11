package heig.tb.jsmithfx;

import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.pixelduke.window.ThemeWindowManager;
import com.pixelduke.window.ThemeWindowManagerFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

public class JSmithFXApplication extends Application {

    private double x,y = 0;

    @Override
    public void start(Stage stage) throws IOException {

        ThemeWindowManager themeWindowManager = ThemeWindowManagerFactory.create();
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());


        FXMLLoader fxmlLoader = new FXMLLoader(JSmithFXApplication.class.getResource("hello-view.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("css.css")).toExternalForm());

        stage.setResizable(true);
        stage.setTitle("JSmithFX!");
        stage.setScene(scene);
        stage.show();
        themeWindowManager.setDarkModeForWindowFrame(stage, true);


    }
}
