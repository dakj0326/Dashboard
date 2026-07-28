package org.net.ui.widgets.widget;

import java.time.Duration;
import java.time.Instant;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import org.net.ui.widgets.BaseWidget;
import org.net.ui.widgets.WidgetID;

public final class TimerWidget extends BaseWidget {
    private static final double PREFERRED_WIDTH = 330;
    private static final double DIAL_SIZE = 142;
    private static final int MAX_SECONDS = 12 * 60 * 60;
    private static final double DEGREES_PER_MINUTE = 6.0;

    private final Canvas dial = new Canvas(DIAL_SIZE, DIAL_SIZE);
    private final Label timeLabel = new Label();
    private final Label stateLabel = new Label("DRAG TO SET");
    private final Button startPause = new Button("START");
    private final Button reset = new Button("RESET");
    private final Timeline ticker;
    private final TimerBackground animatedBackground = new TimerBackground();

    private int selectedSeconds = 25 * 60;
    private int remainingSeconds = selectedSeconds;
    private boolean running;
    private boolean hasStarted;
    private Instant endTime;
    private double previousAngle;
    private double dragRemainder;
    private boolean enabled = true;
    private boolean dashboardVisible;

    public TimerWidget() {
        super("Timer", new HBox(), WidgetID.TIMER, PREFERRED_WIDTH);
        HBox content = (HBox) getCenter();
        animatedBackground.widthProperty().bind(widthProperty());
        animatedBackground.heightProperty().bind(heightProperty());
        installBackgroundLayer(animatedBackground);
        content.getStyleClass().add("timer-content");
        content.setAlignment(Pos.CENTER);

        timeLabel.getStyleClass().add("timer-time");
        stateLabel.getStyleClass().add("timer-state");
        StackPane wheel = new StackPane(dial, new VBox(2, timeLabel, stateLabel));
        wheel.getStyleClass().add("timer-wheel");
        wheel.setPickOnBounds(true);
        ((VBox) wheel.getChildren().get(1)).setAlignment(Pos.CENTER);

        startPause.getStyleClass().addAll("timer-button", "timer-primary-button");
        reset.getStyleClass().add("timer-button");
        startPause.setOnAction(event -> toggleTimer());
        reset.setOnAction(event -> resetTimer());
        VBox controls = new VBox(9, startPause, reset);
        controls.setAlignment(Pos.CENTER);

        content.getChildren().addAll(wheel, controls);
        configureDialInput(wheel);
        refreshDisplay();

        ticker = new Timeline(new KeyFrame(
                javafx.util.Duration.millis(200),
                event -> updateCountdown()
        ));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
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

    private void configureDialInput(StackPane wheel) {
        dial.getStyleClass().add("timer-dial");
        wheel.setOnMousePressed(event -> {
            beginDrag(event.getX(), event.getY());
            event.consume();
        });
        wheel.setOnMouseDragged(event -> {
            dragTo(event.getX(), event.getY());
            event.consume();
        });
        wheel.setOnTouchPressed(event -> {
            beginDrag(event.getTouchPoint().getX(), event.getTouchPoint().getY());
            event.consume();
        });
        wheel.setOnTouchMoved(event -> {
            dragTo(event.getTouchPoint().getX(), event.getTouchPoint().getY());
            event.consume();
        });
    }

    private void beginDrag(double x, double y) {
        if (running) return;
        previousAngle = angleAt(x, y);
        dragRemainder = 0;
    }

    private void dragTo(double x, double y) {
        if (running) return;
        double angle = angleAt(x, y);
        double delta = angle - previousAngle;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        previousAngle = angle;
        dragRemainder += delta;

        int minuteDelta = (int) (dragRemainder / DEGREES_PER_MINUTE);
        if (minuteDelta == 0) return;
        dragRemainder -= minuteDelta * DEGREES_PER_MINUTE;
        selectedSeconds = clamp(selectedSeconds + minuteDelta * 60);
        remainingSeconds = selectedSeconds;
        hasStarted = false;
        refreshDisplay();
    }

    private void toggleTimer() {
        if (running) {
            remainingSeconds = secondsUntilEnd();
            running = false;
            stateLabel.setText("PAUSED");
        } else if (remainingSeconds > 0) {
            endTime = Instant.now().plusSeconds(remainingSeconds);
            running = true;
            hasStarted = true;
        }
        refreshDisplay();
    }

    private void resetTimer() {
        running = false;
        hasStarted = false;
        selectedSeconds = 0;
        remainingSeconds = 0;
        refreshDisplay();
    }

    private void updateCountdown() {
        if (!running) return;
        remainingSeconds = secondsUntilEnd();
        if (remainingSeconds <= 0) {
            remainingSeconds = 0;
            running = false;
            stateLabel.setText("FINISHED");
        }
        refreshDisplay();
    }

    private int secondsUntilEnd() {
        if (endTime == null) return remainingSeconds;
        long milliseconds = Duration.between(Instant.now(), endTime).toMillis();
        return (int) Math.max(0, Math.ceil(milliseconds / 1000.0));
    }

    private void refreshDisplay() {
        int hours = remainingSeconds / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;
        timeLabel.setText(hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds));

        if (running) {
            stateLabel.setText("RUNNING");
            startPause.setText("PAUSE");
        } else {
            startPause.setText(hasStarted && remainingSeconds > 0 ? "RESUME" : "START");
            if (!hasStarted && remainingSeconds > 0) stateLabel.setText("DRAG TO SET");
            if (remainingSeconds == 0 && !"FINISHED".equals(stateLabel.getText())) {
                stateLabel.setText("DRAG TO SET");
            }
        }
        startPause.setDisable(!running && remainingSeconds == 0);
        drawDial();
    }

    private void drawDial() {
        GraphicsContext graphics = dial.getGraphicsContext2D();
        graphics.clearRect(0, 0, DIAL_SIZE, DIAL_SIZE);
        double inset = 10;
        double diameter = DIAL_SIZE - inset * 2;

        graphics.setLineWidth(9);
        graphics.setStroke(Color.rgb(255, 255, 255, 0.08));
        graphics.strokeOval(inset, inset, diameter, diameter);

        double progress = selectedSeconds <= 0
                ? 0
                : (double) remainingSeconds / selectedSeconds;
        graphics.setStroke(running
                ? Color.web("#75A7FF")
                : Color.web("#7D8FA8"));
        graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        graphics.strokeArc(
                inset,
                inset,
                diameter,
                diameter,
                90,
                -360 * progress,
                ArcType.OPEN
        );

        graphics.setFill(Color.rgb(235, 240, 248, 0.48));
        double center = DIAL_SIZE / 2;
        for (int minute = 0; minute < 60; minute += 5) {
            double angle = Math.toRadians(minute * 6 - 90);
            double radius = DIAL_SIZE / 2 - 2;
            graphics.fillOval(
                    center + Math.cos(angle) * radius - 1.5,
                    center + Math.sin(angle) * radius - 1.5,
                    3,
                    3
            );
        }
    }

    private static double angleAt(double x, double y) {
        return Math.toDegrees(Math.atan2(y - DIAL_SIZE / 2, x - DIAL_SIZE / 2));
    }

    private static int clamp(int seconds) {
        return Math.max(0, Math.min(MAX_SECONDS, seconds));
    }
}
