package heig.tb.jsmithfx;

import javafx.application.Application;

import java.awt.*;

public class Launcher {

    public static void main(String[] args) {

        try {
            // Get the default graphics environment
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            // Get the default screen device (primary monitor)
            GraphicsDevice gd = ge.getDefaultScreenDevice();

            // Get the current display mode
            int refreshRate = gd.getDisplayMode().getRefreshRate();

            // The API returns REFRESH_RATE_UNKNOWN if it's not known.
            // A common value for this is 0. We only set the property if we get a valid rate.
            if (refreshRate != java.awt.DisplayMode.REFRESH_RATE_UNKNOWN) {
                System.out.println("Detected Monitor Refresh Rate: " + refreshRate);
                // Set the JavaFX animation pulse rate to match the monitor's refresh rate
                System.setProperty("javafx.animation.pulse", String.valueOf(refreshRate));
            } else {
                System.out.println("Could not determine monitor refresh rate. Using default.");
            }
        } catch (Exception e) {
            // In case of a headless environment or other errors, catch the exception.
            System.err.println("Could not query graphics device for refresh rate: " + e.getMessage());
        }

        Application.launch(JSmithFXApplication.class, args);
    }
}
