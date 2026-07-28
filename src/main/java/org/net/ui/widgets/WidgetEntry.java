package org.net.ui.widgets;

public class WidgetEntry {

    private boolean visible = true;
    private final BaseWidget widget;


    public WidgetEntry(BaseWidget widget) {
        this.widget = widget;
    }

    public BaseWidget getWidget() {
        return widget;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
