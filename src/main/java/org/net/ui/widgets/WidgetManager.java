package org.net.ui.widgets;

import java.util.LinkedHashMap;
import java.util.Map;
import org.net.settings.AppSettings;

public class WidgetManager {

    private final Map<WidgetID, WidgetEntry> widgets = new LinkedHashMap<>();

    public void registerWidget(WidgetID id, WidgetEntry entry) {
        boolean visible = AppSettings.getInstance()
                .getBoolean(widgetVisibilityKey(id), entry.isVisible());
        entry.setVisible(visible);
        entry.getWidget().onVisibilityChanged(visible);
        widgets.put(id, entry);
    }

    public void removeWidget(WidgetID id) {
        widgets.remove(id);
    }

    public Map<WidgetID, WidgetEntry> getWidgets() {
        return widgets;
    }

    public WidgetEntry getWidgetEntry(WidgetID id) {
        return widgets.get(id);
    }

    public BaseWidget getWidget(WidgetID id) {
        return widgets.get(id).getWidget();
    }

    public void hideWidget(WidgetID id) {
        widgets.get(id).setVisible(false);
        widgets.get(id).getWidget().onVisibilityChanged(false);
        AppSettings.getInstance().setBoolean(widgetVisibilityKey(id), false);
    }

    public void showWidget(WidgetID id) {
        widgets.get(id).setVisible(true);
        widgets.get(id).getWidget().onVisibilityChanged(true);
        AppSettings.getInstance().setBoolean(widgetVisibilityKey(id), true);
    }

    public boolean isWidgetVisible(WidgetID id) {
        return widgets.get(id).isVisible();
    }

    public int getRegisteredWidgetCount() {
        return widgets.size();
    }

    public long getVisibleWidgetCount() {
        return widgets.values().stream().filter(WidgetEntry::isVisible).count();
    }

    private String widgetVisibilityKey(WidgetID id) {
        return "widgets." + id.name().toLowerCase() + ".visible";
    }

}
