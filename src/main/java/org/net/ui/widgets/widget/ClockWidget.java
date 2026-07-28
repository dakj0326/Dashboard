package org.net.ui.widgets.widget;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import org.net.ui.widgets.BaseWidget;
import org.net.ui.widgets.WidgetID;
import org.net.settings.AppSettings;

public class ClockWidget extends BaseWidget {
    private static final double PREFERRED_WIDTH = 260;
    private static final double SIZE = 145;
    private static final double CENTER = SIZE / 2;
    private final Line hourHand = hand(6, Color.web("#e8ecf6"));
    private final Line minuteHand = hand(4, Color.web("#b7c1d8"));
    private final Line secondHand = hand(2, Color.web("#ff6b78"));
    private final Pane analogFace;
    private final VBox digitalFace = new VBox(4);
    private final Label digitalTime = new Label();
    private final Label digitalDate = new Label();
    private final Label analogDate = new Label();
    private final ClockBackground animatedBackground = new ClockBackground();
    private boolean enabled = true;
    private boolean dashboardVisible;

    public ClockWidget() {
        super("Local time", new StackPane(), WidgetID.CLOCK, PREFERRED_WIDTH);
        StackPane container = (StackPane) getCenter();
        animatedBackground.widthProperty().bind(widthProperty());
        animatedBackground.heightProperty().bind(heightProperty());
        installBackgroundLayer(animatedBackground);
        container.setAlignment(Pos.CENTER);
        analogFace = createClockFace();
        configureDigitalFace();
        container.getChildren().addAll(analogFace, digitalFace);
        updateClock();

        Timeline timer = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateClock()),
                new KeyFrame(Duration.seconds(1))
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        enabled = visible;
        animatedBackground.setActive(enabled && dashboardVisible);
    }

    @Override
    public void onPageVisibilityChanged(boolean visible) {
        dashboardVisible = visible;
        animatedBackground.setActive(enabled && dashboardVisible);
    }

    private Pane createClockFace() {
        Pane face = new Pane();
        face.setMinSize(SIZE, SIZE);
        face.setMaxSize(SIZE, SIZE);
        face.getStyleClass().add("clock-face");

        Circle rim = new Circle(CENTER, CENTER, CENTER - 3);
        rim.getStyleClass().add("clock-rim");
        face.getChildren().add(rim);

        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6 - 90);
            boolean hour = i % 5 == 0;
            double outer = CENTER - 13;
            double inner = outer - (hour ? 10 : 4);
            Line tick = new Line(
                    CENTER + Math.cos(angle) * inner,
                    CENTER + Math.sin(angle) * inner,
                    CENTER + Math.cos(angle) * outer,
                    CENTER + Math.sin(angle) * outer
            );
            tick.getStyleClass().add(hour ? "clock-hour-tick" : "clock-minute-tick");
            face.getChildren().add(tick);
        }

        face.getChildren().addAll(hourHand, minuteHand, secondHand);
        Circle pin = new Circle(CENTER, CENTER, 5);
        pin.getStyleClass().add("clock-pin");
        face.getChildren().add(pin);

        analogDate.getStyleClass().add("clock-date");
        analogDate.setAlignment(Pos.CENTER);
        analogDate.setPrefWidth(SIZE);
        analogDate.setLayoutY(106);
        face.getChildren().add(analogDate);
        return face;
    }

    private void configureDigitalFace() {
        digitalFace.setAlignment(Pos.CENTER);
        digitalTime.getStyleClass().add("digital-clock-time");
        digitalDate.getStyleClass().add("digital-clock-date");
        digitalFace.getChildren().addAll(digitalTime, digitalDate);
    }

    private static Line hand(double width, Color color) {
        Line hand = new Line(CENTER, CENTER, CENTER, CENTER);
        hand.setStrokeWidth(width);
        hand.setStroke(color);
        hand.setStrokeLineCap(StrokeLineCap.ROUND);
        return hand;
    }

    private void updateClock() {
        AppSettings settings = AppSettings.getInstance();
        boolean digital = settings.getBoolean("clock.digital", false);
        boolean use24Hour = settings.getBoolean("clock.use24Hour", true);
        animatedBackground.setAnalog(digital);
        analogFace.setVisible(!digital);
        analogFace.setManaged(!digital);
        digitalFace.setVisible(digital);
        digitalFace.setManaged(digital);

        LocalTime now = LocalTime.now();
        setHand(hourHand, (now.getHour() % 12 + now.getMinute() / 60.0) * 30, 36);
        setHand(minuteHand, (now.getMinute() + now.getSecond() / 60.0) * 6, 51);
        setHand(secondHand, now.getSecond() * 6, 56);
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern(
                use24Hour ? "HH:mm:ss" : "h:mm:ss a"
        );
        digitalTime.setText(now.format(timeFormat));
        LocalDate today = LocalDate.now();
        analogDate.setText(today.format(DateTimeFormatter.ofPattern("EEE, d MMM")));
        digitalDate.setText(today.format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM")
        ));
    }

    private static void setHand(Line hand, double degrees, double length) {
        double angle = Math.toRadians(degrees - 90);
        hand.setEndX(CENTER + Math.cos(angle) * length);
        hand.setEndY(CENTER + Math.sin(angle) * length);
    }
}
