package org.net.ui.pages.settings;

public enum Theme {
    MIDNIGHT("Midnight", "default(dark).css"),
    OBSIDIAN("Obsidian", "obsidian.css"),
    FOREST("Forest", "forest.css"),
    EMBER("Ember", "ember.css"),
    PLUM("Plum", "plum.css"),
    GRAPHITE("Graphite", "graphite.css"),
    NORD("Nord", "nord.css"),
    COFFEE("Coffee", "coffee.css"),
    CRIMSON("Crimson", "crimson.css"),
    TEAL("Deep Teal", "teal.css");

    private final String displayName;
    private final String stylesheet;

    Theme(String displayName, String stylesheet) {
        this.displayName = displayName;
        this.stylesheet = stylesheet;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
