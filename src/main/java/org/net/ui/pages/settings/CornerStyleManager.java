package org.net.ui.pages.settings;

import javafx.scene.Scene;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.net.settings.AppSettings;

public final class CornerStyleManager {
    private static Scene scene;
    private static final BooleanProperty rounded = new SimpleBooleanProperty(
            AppSettings.getInstance().getBoolean("appearance.roundedCorners", true)
    );
    private static String currentStylesheet;

    private CornerStyleManager() {}

    public static void initialize(Scene scene) {
        CornerStyleManager.scene = scene;
        setRounded(rounded.get());
    }

    public static void setRounded(boolean useRoundedCorners) {
        if (scene == null) {
            throw new IllegalStateException("CornerStyleManager has not been initialized.");
        }

        rounded.set(useRoundedCorners);
        AppSettings.getInstance().setBoolean("appearance.roundedCorners", rounded.get());
        if (currentStylesheet != null) {
            scene.getStylesheets().remove(currentStylesheet);
        }

        String profile = rounded.get() ? "rounded.css" : "square.css";
        currentStylesheet = CornerStyleManager.class
                .getResource("/css/corners/" + profile)
                .toExternalForm();
        scene.getStylesheets().add(currentStylesheet);
    }

    public static boolean usesRoundedCorners() {
        return rounded.get();
    }

    public static ReadOnlyBooleanProperty roundedProperty() {
        return rounded;
    }
}
