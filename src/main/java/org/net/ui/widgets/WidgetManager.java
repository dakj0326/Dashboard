package org.net.ui.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.net.settings.AppSettings;

public class WidgetManager {
    private static final String ORDER_KEY = "widgets.order";

    private final Map<WidgetID, WidgetEntry> widgets = new LinkedHashMap<>();

    public void registerWidget(WidgetID id, WidgetEntry entry) {
        boolean visible = AppSettings.getInstance()
                .getBoolean(widgetVisibilityKey(id), entry.isVisible());
        entry.setVisible(visible);
        entry.getWidget().onVisibilityChanged(visible);
        widgets.put(id, entry);
        applySavedOrder();
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

    public boolean moveWidget(WidgetID id, int offset) {
        List<WidgetID> order = new ArrayList<>(widgets.keySet());
        int currentIndex = order.indexOf(id);
        int targetIndex = currentIndex + offset;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= order.size()) {
            return false;
        }
        Collections.swap(order, currentIndex, targetIndex);
        reorder(order);
        saveOrder();
        return true;
    }

    public boolean canMoveWidget(WidgetID id, int offset) {
        List<WidgetID> order = new ArrayList<>(widgets.keySet());
        int currentIndex = order.indexOf(id);
        int targetIndex = currentIndex + offset;
        return currentIndex >= 0 && targetIndex >= 0 && targetIndex < order.size();
    }

    private void applySavedOrder() {
        String saved = AppSettings.getInstance().get(ORDER_KEY, "");
        if (saved.isBlank()) return;

        List<WidgetID> order = new ArrayList<>();
        for (String value : saved.split(",")) {
            try {
                WidgetID id = WidgetID.valueOf(value.strip());
                if (widgets.containsKey(id) && !order.contains(id)) {
                    order.add(id);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore widget types that no longer exist.
            }
        }
        for (WidgetID id : widgets.keySet()) {
            if (!order.contains(id)) order.add(id);
        }
        reorder(order);
    }

    private void reorder(List<WidgetID> order) {
        Map<WidgetID, WidgetEntry> reordered = new LinkedHashMap<>();
        for (WidgetID id : order) {
            WidgetEntry entry = widgets.get(id);
            if (entry != null) reordered.put(id, entry);
        }
        widgets.clear();
        widgets.putAll(reordered);
    }

    private void saveOrder() {
        String value = widgets.keySet().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        AppSettings.getInstance().set(ORDER_KEY, value);
    }

    private String widgetVisibilityKey(WidgetID id) {
        return "widgets." + id.name().toLowerCase() + ".visible";
    }

}
