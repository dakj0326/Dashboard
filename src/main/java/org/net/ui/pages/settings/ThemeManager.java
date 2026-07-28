package org.net.ui.pages.settings;

import javafx.scene.Scene;
import org.net.settings.AppSettings;

public final class ThemeManager {

    private static Scene scene;
    private static Theme currentTheme = loadSavedTheme();
    private static String currentThemeStylesheet;

    private ThemeManager() {}

    public static void initialize(Scene scene) {
        ThemeManager.scene = scene;
        setTheme(currentTheme);
    }

    public static void setTheme(Theme theme) {

        if (scene == null) {
            throw new IllegalStateException("ThemeManager has not been initialized.");
        }

        currentTheme = theme;
        AppSettings.getInstance().set("appearance.theme", theme.name());

        // Remove only the previous theme stylesheet
        if (currentThemeStylesheet != null) {
            scene.getStylesheets().remove(currentThemeStylesheet);
        }

        currentThemeStylesheet = ThemeManager.class
                .getResource("/css/themes/" + theme.getStylesheet())
                .toExternalForm();

        scene.getStylesheets().add(currentThemeStylesheet);
    }

    public static Theme getTheme() {
        return currentTheme;
    }

    private static Theme loadSavedTheme() {
        String savedTheme = AppSettings.getInstance().get("appearance.theme", Theme.MIDNIGHT.name());
        try {
            return Theme.valueOf(savedTheme);
        } catch (IllegalArgumentException exception) {
            return Theme.MIDNIGHT;
        }
    }
}
