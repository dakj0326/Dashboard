package org.net.ui.widgets.widget;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

final class StockBackground extends Canvas {
    private boolean active;
    private long lastFrame;
    private double phase;

    StockBackground() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!active || now - lastFrame < 66_000_000) return;
                if (lastFrame != 0) {
                    phase = (phase + (now - lastFrame) / 1_000_000_000.0 * 0.025) % 1;
                }
                lastFrame = now;
                draw();
            }
        }.start();
    }

    void setActive(boolean active) {
        this.active = active;
        if (active) {
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
        graphics.setFill(Color.rgb(5, 11, 13, 0.38));
        graphics.fillRect(0, 0, width, height);

        graphics.setStroke(Color.rgb(125, 195, 175, 0.045));
        graphics.setLineWidth(1);
        for (double x = width * 0.48; x < width; x += 32) {
            graphics.strokeLine(x, 0, x, height);
        }
        for (double y = 25; y < height; y += 32) {
            graphics.strokeLine(width * 0.45, y, width, y);
        }

        graphics.setStroke(Color.rgb(95, 205, 155, 0.11));
        graphics.setLineWidth(2);
        graphics.beginPath();
        for (int i = 0; i <= 12; i++) {
            double x = width * (0.43 + i * 0.052);
            double wave = Math.sin(i * 0.9 + phase * Math.PI * 2) * height * 0.075;
            double trend = height * (0.72 - i * 0.025);
            double y = trend + wave;
            if (i == 0) graphics.moveTo(x, y);
            else graphics.lineTo(x, y);
        }
        graphics.stroke();
    }
}
