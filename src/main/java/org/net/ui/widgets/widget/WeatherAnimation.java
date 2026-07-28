package org.net.ui.widgets.widget;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

final class WeatherAnimation extends Canvas {
    private int weatherCode;
    private boolean active;
    private long lastFrame;

    WeatherAnimation() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!active || now - lastFrame < 41_000_000) {
                    return;
                }
                lastFrame = now;
                draw(now / 1_000_000.0);
            }
        };
        timer.start();
    }

    void setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
    }

    void setActive(boolean active) {
        this.active = active;
        if (active) {
            draw(System.nanoTime() / 1_000_000.0);
        }
    }

    private void draw(double time) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) return;
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        drawBase(graphics, width, height);

        if (isRain(weatherCode)) {
            drawClouds(graphics, width, height, time, 0.22);
            drawRain(graphics, width, height, time);
        } else if (isSnow(weatherCode)) {
            drawClouds(graphics, width, height, time, 0.20);
            drawSnow(graphics, width, height, time);
        } else if (weatherCode == 45 || weatherCode == 48) {
            drawFog(graphics, width, height, time);
        } else if (weatherCode >= 95) {
            drawClouds(graphics, width, height, time, 0.30);
            drawRain(graphics, width, height, time);
            if (Math.sin(time / 310) > 0.985) {
                graphics.setFill(Color.rgb(210, 220, 235, 0.16));
                graphics.fillRect(0, 0, width, height);
            }
        } else if (weatherCode == 0) {
            drawSunGlow(graphics, width, height, time);
        } else {
            drawClouds(graphics, width, height, time, weatherCode == 3 ? 0.28 : 0.18);
        }
    }

    private void drawBase(GraphicsContext graphics, double width, double height) {
        Color top;
        Color bottom;
        if (isRain(weatherCode) || weatherCode >= 95) {
            top = Color.rgb(24, 32, 40);
            bottom = Color.rgb(12, 17, 23);
        } else if (isSnow(weatherCode)) {
            top = Color.rgb(42, 50, 58);
            bottom = Color.rgb(20, 26, 32);
        } else if (weatherCode == 0) {
            top = Color.rgb(42, 48, 55);
            bottom = Color.rgb(22, 27, 32);
        } else {
            top = Color.rgb(31, 37, 43);
            bottom = Color.rgb(16, 21, 26);
        }
        graphics.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)
        ));
        graphics.fillRect(0, 0, width, height);
    }

    private void drawRain(GraphicsContext graphics, double width, double height, double time) {
        graphics.setStroke(Color.rgb(150, 185, 210, 0.42));
        graphics.setLineWidth(1.2);
        for (int i = 0; i < 30; i++) {
            double x = (i * 47 + time * 0.12) % (width + 50) - 25;
            double y = (i * 31 + time * 0.32) % (height + 42) - 21;
            graphics.strokeLine(x, y, x - 5, y + 15);
        }
    }

    private void drawSnow(GraphicsContext graphics, double width, double height, double time) {
        graphics.setFill(Color.rgb(235, 242, 248, 0.62));
        for (int i = 0; i < 24; i++) {
            double y = (i * 37 + time * 0.045) % (height + 18) - 9;
            double x = (i * 53 + Math.sin(time / 700 + i) * 17) % width;
            double size = 1.5 + i % 3;
            graphics.fillOval(x, y, size, size);
        }
    }

    private void drawClouds(
            GraphicsContext graphics,
            double width,
            double height,
            double time,
            double opacity
    ) {
        graphics.setFill(Color.rgb(150, 160, 170, opacity));
        for (int i = 0; i < 4; i++) {
            double x = (i * 145 + time * (0.004 + i * 0.001)) % (width + 170) - 120;
            double y = 20 + i * 32;
            graphics.fillOval(x, y, 150, 54);
            graphics.fillOval(x + 42, y - 18, 82, 60);
        }
    }

    private void drawFog(GraphicsContext graphics, double width, double height, double time) {
        graphics.setStroke(Color.rgb(190, 198, 202, 0.24));
        graphics.setLineWidth(10);
        for (int i = 0; i < 5; i++) {
            double offset = Math.sin(time / 1300 + i) * 20;
            double y = 45 + i * 27;
            graphics.strokeLine(-30 + offset, y, width + offset, y);
        }
    }

    private void drawSunGlow(GraphicsContext graphics, double width, double height, double time) {
        double x = width * 0.83 + Math.sin(time / 3500) * 5;
        double y = height * 0.18;
        graphics.setFill(new RadialGradient(
                0, 0, x, y, 100, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(235, 190, 110, 0.25)),
                new Stop(1, Color.TRANSPARENT)
        ));
        graphics.fillOval(x - 100, y - 100, 200, 200);
    }

    private static boolean isRain(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
    }

    private static boolean isSnow(int code) {
        return (code >= 71 && code <= 77) || code == 85 || code == 86;
    }
}
