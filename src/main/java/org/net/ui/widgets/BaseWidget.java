package org.net.ui.widgets;

import javafx.geometry.Insets;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Rectangle2D;
import org.net.ui.pages.settings.CornerStyleManager;

public class BaseWidget extends BorderPane {
    public static final double STANDARD_HEIGHT = 210;
    public final WidgetID id;
    private final String displayName;
    private final ImageView adaptiveBackground = new ImageView();

    public BaseWidget(String title, Node content, WidgetID id, double preferredWidth) {
        this.id = id;
        this.displayName = title;
        createLayout(title, content, preferredWidth);
    }

    private void createLayout(String title, Node content, double preferredWidth) {
        getStyleClass().add("widget");
        configureAdaptiveBackground();

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("widget-title");

        BorderPane.setMargin(titleLabel, new Insets(0, 0, 10, 0));

        setTop(titleLabel);
        setCenter(content);

        setMinSize(preferredWidth, STANDARD_HEIGHT);
        setPrefSize(preferredWidth, STANDARD_HEIGHT);
        setMaxHeight(STANDARD_HEIGHT);
    }

    public void update(WidgetManager manager) {
        // Live widgets can override this hook.
    }

    public String getDisplayName() {
        return displayName;
    }

    public void onVisibilityChanged(boolean visible) {
        // Widgets with background work can pause or resume it here.
    }

    public void onPageVisibilityChanged(boolean visible) {
        // Animated widgets can pause when their containing page is not shown.
    }

    protected void setAdaptiveBackground(Image image, double darkness) {
        adaptiveBackground.setImage(image);
        adaptiveBackground.setEffect(new ColorAdjust(0, 0, -clamp(darkness), 0));
        updateBackgroundViewport();
        adaptiveBackground.setVisible(image != null);
    }

    protected void clearAdaptiveBackground() {
        adaptiveBackground.setImage(null);
        adaptiveBackground.setVisible(false);
    }

    protected void installBackgroundLayer(Node layer) {
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.setClip(createCornerClip());
        getChildren().add(Math.min(1, getChildren().size()), layer);
    }

    private void configureAdaptiveBackground() {
        adaptiveBackground.setManaged(false);
        adaptiveBackground.setMouseTransparent(true);
        adaptiveBackground.setSmooth(true);
        adaptiveBackground.setPreserveRatio(false);
        adaptiveBackground.setVisible(false);
        adaptiveBackground.fitWidthProperty().bind(widthProperty());
        adaptiveBackground.fitHeightProperty().bind(heightProperty());

        adaptiveBackground.setClip(createCornerClip());

        widthProperty().addListener((observable, oldValue, newValue) -> updateBackgroundViewport());
        heightProperty().addListener((observable, oldValue, newValue) -> updateBackgroundViewport());
        getChildren().add(adaptiveBackground);
    }

    private Rectangle createCornerClip() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.arcWidthProperty().bind(Bindings.when(CornerStyleManager.roundedProperty())
                .then(28.0).otherwise(0.0));
        clip.arcHeightProperty().bind(Bindings.when(CornerStyleManager.roundedProperty())
                .then(28.0).otherwise(0.0));
        return clip;
    }

    private void updateBackgroundViewport() {
        Image image = adaptiveBackground.getImage();
        double targetWidth = getWidth();
        double targetHeight = getHeight();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                || targetWidth <= 0 || targetHeight <= 0) {
            return;
        }

        double imageRatio = image.getWidth() / image.getHeight();
        double targetRatio = targetWidth / targetHeight;
        if (imageRatio > targetRatio) {
            double cropWidth = image.getHeight() * targetRatio;
            adaptiveBackground.setViewport(new Rectangle2D(
                    (image.getWidth() - cropWidth) / 2,
                    0,
                    cropWidth,
                    image.getHeight()
            ));
        } else {
            double cropHeight = image.getWidth() / targetRatio;
            adaptiveBackground.setViewport(new Rectangle2D(
                    0,
                    (image.getHeight() - cropHeight) / 2,
                    image.getWidth(),
                    cropHeight
            ));
        }
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
