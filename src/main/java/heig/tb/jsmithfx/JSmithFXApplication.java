package heig.tb.jsmithfx;

import atlantafx.base.theme.NordDark;
import com.pixelduke.window.ThemeWindowManager;
import com.pixelduke.window.ThemeWindowManagerFactory;
import heig.tb.jsmithfx.utilities.StageController;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class JSmithFXApplication extends Application {

    private static final double REFERENCE_HEIGHT = 1440.0;
    private static final double BASE_FONT_SIZE = 14.0;

    @Override
    public void start(Stage stage) throws IOException {
        Locale.setDefault(Locale.US); // To properly display numbers

        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        ThemeWindowManager themeWindowManager = ThemeWindowManagerFactory.create();

        FXMLLoader fxmlLoader = new FXMLLoader(JSmithFXApplication.class.getResource("hello-view.fxml"));
        Parent root = fxmlLoader.load();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        Scene scene = new Scene(root, screenBounds.getWidth() * 0.75, screenBounds.getHeight() * 0.75);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("css.css")).toExternalForm());

        stage.setResizable(true);
        stage.setTitle("JSmithFX!");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);

        // Start running the stage controller for proper stage name change
        StageController sc = new StageController(stage);

        stage.show();
        themeWindowManager.setDarkModeForWindowFrame(stage, true);
    }
}
