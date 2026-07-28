package org.net.system;

import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class SystemHealthService {
    private static final SystemHealthService INSTANCE = new SystemHealthService();
    private final Map<String, HealthSeverity> issueLevels = new HashMap<>();
    private final ObservableList<HealthIssue> issues = FXCollections.observableArrayList();
    private final ObservableList<HealthIssue> readOnlyIssues =
            FXCollections.unmodifiableObservableList(issues);
    private final ReadOnlyObjectWrapper<HealthSeverity> severity =
            new ReadOnlyObjectWrapper<>(HealthSeverity.OK);

    private SystemHealthService() {}

    public static SystemHealthService getInstance() {
        return INSTANCE;
    }

    public void report(String source, HealthSeverity level) {
        runOnFxThread(() -> {
            if (level == HealthSeverity.OK) {
                if (issueLevels.remove(source) == null) {
                    return;
                }
            } else {
                HealthSeverity previous = issueLevels.put(source, level);
                if (previous == level) {
                    return;
                }
            }
            updateSeverity();
        });
    }

    public void clear(String source) {
        report(source, HealthSeverity.OK);
    }

    public ReadOnlyObjectProperty<HealthSeverity> severityProperty() {
        return severity.getReadOnlyProperty();
    }

    public HealthSeverity getSeverity() {
        return severity.get();
    }

    public ObservableList<HealthIssue> getIssues() {
        return readOnlyIssues;
    }

    private void updateSeverity() {
        HealthSeverity highest = HealthSeverity.OK;
        for (HealthSeverity issue : issueLevels.values()) {
            if (issue.ordinal() > highest.ordinal()) {
                highest = issue;
            }
        }
        severity.set(highest);

        List<HealthIssue> currentIssues = issueLevels.entrySet().stream()
                .map(entry -> new HealthIssue(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing(HealthIssue::severity)
                        .reversed()
                        .thenComparing(HealthIssue::source))
                .toList();
        issues.setAll(currentIssues);
    }

    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
