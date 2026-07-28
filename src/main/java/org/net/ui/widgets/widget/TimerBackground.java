package org.net.ui.widgets.widget;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

final class TimerBackground extends Canvas {
    private static final long FRAME_INTERVAL = 50_000_000L;
    private long lastFrame;
    private boolean active;

    private final AnimationTimer animation = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (now - lastFrame < FRAME_INTERVAL) return;
            lastFrame = now;
            draw(now / 1_000_000_000.0);
        }
    };

    TimerBackground() {
        setMouseTransparent(true);
        widthProperty().addListener((observable, oldValue, newValue) -> draw(0));
        heightProperty().addListener((observable, oldValue, newValue) -> draw(0));
    }

    void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        if (active) {
            lastFrame = 0;
            animation.start();
        } else {
            animation.stop();
            draw(0);
        }
    }

    private void draw(double seconds) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) return;

        GraphicsContext graphics = getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        graphics.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(12, 18, 29, 0.90)),
                new Stop(0.55, Color.rgb(20, 28, 42, 0.82)),
                new Stop(1, Color.rgb(11, 16, 25, 0.94))
        ));
        graphics.fillRect(0, 0, width, height);

        double glowX = width * (0.38 + Math.sin(seconds * 0.08) * 0.05);
        double glowY = height * (0.55 + Math.cos(seconds * 0.07) * 0.06);
        graphics.setFill(new RadialGradient(
                0, 0, glowX, glowY, Math.max(width, height) * 0.54,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(78, 116, 170, 0.18)),
                new Stop(0.48, Color.rgb(47, 73, 112, 0.07)),
                new Stop(1, Color.TRANSPARENT)
        ));
        graphics.fillRect(0, 0, width, height);

        double centerX = width * 0.17;
        double centerY = height * 0.76;
        double rotation = seconds * 2.2;
        graphics.setLineCap(StrokeLineCap.ROUND);

        drawArc(graphics, centerX, centerY, 250, rotation, 215,
                Color.rgb(117, 167, 255, 0.12), 3.0);
        drawArc(graphics, centerX, centerY, 310, 150 - rotation * 0.55, 150,
                Color.rgb(151, 176, 214, 0.08), 2.0);
        drawArc(graphics, centerX, centerY, 372, 250 + rotation * 0.34, 105,
                Color.rgb(117, 167, 255, 0.055), 5.0);

        drawTicks(graphics, centerX, centerY, 154, rotation * 0.18);
    }

    private static void drawArc(
            GraphicsContext graphics,
            double centerX,
            double centerY,
            double diameter,
            double start,
            double extent,
            Color color,
            double width
    ) {
        graphics.setStroke(color);
        graphics.setLineWidth(width);
        graphics.strokeArc(
                centerX - diameter / 2,
                centerY - diameter / 2,
                diameter,
                diameter,
                start,
                extent,
                ArcType.OPEN
        );
    }

    private static void drawTicks(
            GraphicsContext graphics,
            double centerX,
            double centerY,
            double radius,
            double rotation
    ) {
        graphics.setStroke(Color.rgb(210, 224, 245, 0.07));
        graphics.setLineWidth(1.2);
        for (int index = 0; index < 24; index++) {
            double angle = Math.toRadians(index * 15 + rotation);
            double inner = radius - (index % 4 == 0 ? 12 : 6);
            graphics.strokeLine(
                    centerX + Math.cos(angle) * inner,
                    centerY + Math.sin(angle) * inner,
                    centerX + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius
            );
        }
    }
}
