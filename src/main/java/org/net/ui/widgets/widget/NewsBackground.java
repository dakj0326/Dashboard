package org.net.ui.widgets.widget;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

final class NewsBackground extends Canvas {
    private boolean active;
    private long lastFrame;
    private double phase;

    NewsBackground() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!active || now - lastFrame < 50_000_000) return;
                if (lastFrame != 0) {
                    phase = (phase + (now - lastFrame) / 1_000_000_000.0 * 0.055) % 1.0;
                }
                lastFrame = now;
                draw();
            }
        }.start();
    }

    void setActive(boolean visible, boolean animated) {
        this.active = visible && animated;
        if (visible) {
            lastFrame = 0;
            draw();
        }
    }

    private void draw() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) return;

        GraphicsContext graphics = getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        graphics.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(5, 9, 15, 0.58)),
                new Stop(1, Color.rgb(10, 20, 30, 0.42))
        ));
        graphics.fillRect(0, 0, width, height);

        double centerX = width * 0.84;
        double centerY = height * 0.53;
        double radius = height * 0.72;
        drawGlobe(graphics, centerX, centerY, radius);
        drawSignal(graphics, width, height);
    }

    private void drawGlobe(
            GraphicsContext graphics,
            double centerX,
            double centerY,
            double radius
    ) {
        graphics.save();
        graphics.beginPath();
        graphics.arc(centerX, centerY, radius, radius, 0, 360);
        graphics.closePath();
        graphics.clip();

        graphics.setFill(Color.rgb(40, 105, 140, 0.045));
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        graphics.setStroke(Color.rgb(125, 190, 215, 0.085));
        graphics.setLineWidth(1);

        for (int latitude = -60; latitude <= 60; latitude += 30) {
            double y = centerY + Math.sin(Math.toRadians(latitude)) * radius;
            double halfWidth = Math.cos(Math.toRadians(latitude)) * radius;
            graphics.strokeOval(
                    centerX - halfWidth,
                    y - radius * 0.075,
                    halfWidth * 2,
                    radius * 0.15
            );
        }

        double drift = phase * Math.PI * 2;
        for (int longitude = 0; longitude < 6; longitude++) {
            double angle = drift + longitude * Math.PI / 3;
            double horizontalRadius = Math.max(6, Math.abs(Math.cos(angle)) * radius);
            graphics.strokeOval(
                    centerX - horizontalRadius,
                    centerY - radius,
                    horizontalRadius * 2,
                    radius * 2
            );
        }
        graphics.restore();

        graphics.setStroke(Color.rgb(145, 205, 225, 0.11));
        graphics.setLineWidth(1.4);
        graphics.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    private void drawSignal(GraphicsContext graphics, double width, double height) {
        double pulse = (Math.sin(phase * Math.PI * 2) + 1) / 2;
        double y = height * 0.82;
        graphics.setStroke(Color.rgb(115, 190, 220, 0.05 + pulse * 0.035));
        graphics.setLineWidth(1);
        graphics.strokeLine(width * 0.04, y, width * 0.64, y);

        double markerX = width * (0.08 + phase * 0.52);
        graphics.setFill(Color.rgb(135, 205, 230, 0.12));
        graphics.fillOval(markerX - 2, y - 2, 4, 4);
    }
}
