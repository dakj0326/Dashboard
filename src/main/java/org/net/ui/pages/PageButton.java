package org.net.ui.pages;

import javafx.scene.control.Button;

public class PageButton extends Button {
    public PageButton(String s, String style) {
        super(s);

        getStyleClass().add(style);
        setMaxWidth(Double.MAX_VALUE);
        setPrefHeight(50);
    }
}
