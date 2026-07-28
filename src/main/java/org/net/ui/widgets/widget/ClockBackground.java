package org.net.ui.widgets.widget;

import java.time.LocalTime;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

final class ClockBackground extends Canvas {
    private boolean active;
    private boolean analog = true;
    private long lastFrame;

    ClockBackground() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!active || now - lastFrame < 41_000_000) {
                    return;
                }
                lastFrame = now;
                draw();
            }
        }.start();
    }

    void setActive(boolean active) {
        this.active = active;
        if (active) draw();
    }

    void setAnalog(boolean analog) {
        if (this.analog == analog) return;
        this.analog = analog;
        if (active) draw();
    }

    private void draw() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) return;

        GraphicsContext graphics = getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        if (analog) {
            drawAnalog(graphics, width, height);
        } else {
            drawDigital(graphics, width, height);
        }
    }

    private void drawAnalog(GraphicsContext graphics, double width, double height) {
        double centerX = width * 0.76;
        double centerY = height * 0.55;
        double radius = Math.min(width, height) * 0.78;

        graphics.setFill(Color.rgb(5, 8, 12, 0.45));
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        graphics.setStroke(Color.rgb(220, 228, 238, 0.09));
        graphics.setLineWidth(3);
        graphics.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6 - 90);
            boolean hour = i % 5 == 0;
            double outer = radius - 18;
            double inner = outer - (hour ? 17 : 7);
            graphics.setStroke(Color.rgb(220, 228, 238, hour ? 0.13 : 0.06));
            graphics.setLineWidth(hour ? 3 : 1);
            graphics.strokeLine(
                    centerX + Math.cos(angle) * inner,
                    centerY + Math.sin(angle) * inner,
                    centerX + Math.cos(angle) * outer,
                    centerY + Math.sin(angle) * outer
            );
        }

        LocalTime now = LocalTime.now();
        double seconds = now.getSecond() + now.getNano() / 1_000_000_000.0;
        drawHand(graphics, centerX, centerY,
                (now.getHour() % 12 + now.getMinute() / 60.0) * 30,
                radius * 0.44, 8, Color.rgb(235, 240, 246, 0.13));
        drawHand(graphics, centerX, centerY,
                (now.getMinute() + seconds / 60.0) * 6,
                radius * 0.64, 5, Color.rgb(220, 228, 238, 0.12));
        drawHand(graphics, centerX, centerY,
                seconds * 6,
                radius * 0.71, 2, Color.rgb(220, 90, 100, 0.16));
    }

    private void drawDigital(GraphicsContext graphics, double width, double height) {
        LocalTime now = LocalTime.now();
        String time = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        double fontSize = Math.max(88, height * 0.78);

        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFont(Font.font("System", FontWeight.BOLD, fontSize));
        graphics.setFill(Color.rgb(5, 8, 12, 0.34));
        graphics.fillText(time, width * 0.62 + 5, height * 0.72 + 6);
        graphics.setFill(Color.rgb(220, 228, 238, 0.075));
        graphics.fillText(time, width * 0.62, height * 0.72);
    }

    private static void drawHand(
            GraphicsContext graphics,
            double centerX,
            double centerY,
            double degrees,
            double length,
            double width,
            Color color
    ) {
        double angle = Math.toRadians(degrees - 90);
        graphics.setStroke(color);
        graphics.setLineWidth(width);
        graphics.setLineCap(StrokeLineCap.ROUND);
        graphics.strokeLine(
                centerX,
                centerY,
                centerX + Math.cos(angle) * length,
                centerY + Math.sin(angle) * length
        );
    }
}
