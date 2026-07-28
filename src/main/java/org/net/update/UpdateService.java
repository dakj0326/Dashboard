package org.net.update;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import org.net.system.HealthSeverity;
import org.net.system.SystemHealthService;

public final class UpdateService {
    private static final long CHECK_INTERVAL_MINUTES = 10;
    private static final String REPOSITORY_ISSUE = "update.repository";
    private static final String FETCH_ISSUE = "update.fetch";
    private static final String COMPARE_ISSUE = "update.compare";
    private final Path projectRoot = resolveProjectRoot();
    private final String operatingSystem =
            System.getProperty("os.name", "").toLowerCase();
    private final SystemHealthService health = SystemHealthService.getInstance();
    private final ReadOnlyBooleanWrapper updateAvailable = new ReadOnlyBooleanWrapper(false);
    private final ScheduledExecutorService checker;
    private boolean started;

    public UpdateService() {
        checker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "github-update-checker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void start() {
        if (started) return;
        started = true;
        checker.scheduleWithFixedDelay(
                this::checkForUpdate,
                0,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public ReadOnlyBooleanProperty updateAvailableProperty() {
        return updateAvailable.getReadOnlyProperty();
    }

    public boolean isWorkingTreeClean() {
        CommandResult result = runGit(List.of(
                "status", "--porcelain", "--untracked-files=normal"
        ), Duration.ofSeconds(10));
        return result.success() && result.output().isBlank();
    }

    public boolean launchUpdater() {
        List<String> command = updaterCommand();
        if (command.isEmpty()) return false;
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(projectRoot.toFile());
            if (!isWindows()) {
                builder.redirectErrorStream(true);
                builder.redirectOutput(ProcessBuilder.Redirect.appendTo(
                        projectRoot.resolve("dashboard-updater-launch.log").toFile()
                ));
            }
            builder.start();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private void checkForUpdate() {
        if (!isSupported() || !Files.isDirectory(projectRoot.resolve(".git"))) {
            reportOnly(REPOSITORY_ISSUE);
            setAvailable(false);
            return;
        }
        CommandResult fetch = runGit(
                List.of("fetch", "--quiet", "origin", "main"),
                Duration.ofSeconds(30)
        );
        if (!fetch.success()) {
            reportOnly(FETCH_ISSUE);
            setAvailable(false);
            return;
        }
        CommandResult behind = runGit(
                List.of("rev-list", "--count", "HEAD..origin/main"),
                Duration.ofSeconds(10)
        );
        try {
            if (!behind.success()) {
                reportOnly(COMPARE_ISSUE);
                setAvailable(false);
                return;
            }
            setAvailable(Integer.parseInt(behind.output().strip()) > 0);
            clearCheckIssues();
        } catch (NumberFormatException exception) {
            reportOnly(COMPARE_ISSUE);
            setAvailable(false);
        }
    }

    private void reportOnly(String source) {
        clearCheckIssuesExcept(source);
        health.report(source, HealthSeverity.WARNING);
    }

    private void clearCheckIssues() {
        clearCheckIssuesExcept("");
    }

    private void clearCheckIssuesExcept(String retainedSource) {
        if (!REPOSITORY_ISSUE.equals(retainedSource)) health.clear(REPOSITORY_ISSUE);
        if (!FETCH_ISSUE.equals(retainedSource)) health.clear(FETCH_ISSUE);
        if (!COMPARE_ISSUE.equals(retainedSource)) health.clear(COMPARE_ISSUE);
    }

    private CommandResult runGit(List<String> arguments, Duration timeout) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(projectRoot.toString());
        command.addAll(arguments);
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(false, "");
            }
            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            return new CommandResult(process.exitValue() == 0, output);
        } catch (IOException exception) {
            return new CommandResult(false, "");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new CommandResult(false, "");
        }
    }

    private void setAvailable(boolean available) {
        Platform.runLater(() -> updateAvailable.set(available));
    }

    private List<String> updaterCommand() {
        String pid = Long.toString(ProcessHandle.current().pid());
        if (isWindows()) {
            Path script = projectRoot.resolve("update-dashboard.ps1");
            if (!Files.isRegularFile(script)) return List.of();
            return List.of(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-File", script.toString(),
                    "-ProjectPath", projectRoot.toString(),
                    "-ProcessId", pid
            );
        }
        if (isLinux()) {
            Path script = projectRoot.resolve("update-dashboard.sh");
            if (!Files.isRegularFile(script)) return List.of();
            return List.of("/bin/bash", script.toString(), projectRoot.toString(), pid);
        }
        return List.of();
    }

    private boolean isSupported() {
        return isWindows() || isLinux();
    }

    private boolean isWindows() {
        return operatingSystem.contains("win");
    }

    private boolean isLinux() {
        return operatingSystem.contains("linux");
    }

    private static Path resolveProjectRoot() {
        String configured = System.getProperty(
                "dashboard.projectDir",
                System.getProperty("user.dir")
        );
        Path candidate = Path.of(configured).toAbsolutePath().normalize();
        for (Path current = candidate; current != null; current = current.getParent()) {
            if (Files.isDirectory(current.resolve(".git"))
                    && Files.isRegularFile(current.resolve("gradlew"))) {
                return current;
            }
        }
        return candidate;
    }

    private record CommandResult(boolean success, String output) {}
}
