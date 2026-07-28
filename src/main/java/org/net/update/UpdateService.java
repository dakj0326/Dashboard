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

public final class UpdateService {
    private static final long CHECK_INTERVAL_MINUTES = 10;
    private final Path projectRoot = resolveProjectRoot();
    private final String operatingSystem =
            System.getProperty("os.name", "").toLowerCase();
    private final ReadOnlyBooleanWrapper updateAvailable = new ReadOnlyBooleanWrapper(false);
    private final ScheduledExecutorService checker;

    public UpdateService() {
        checker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "github-update-checker");
            thread.setDaemon(true);
            return thread;
        });
        checker.scheduleWithFixedDelay(
                this::checkForUpdate,
                3,
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
            setAvailable(false);
            return;
        }
        CommandResult fetch = runGit(
                List.of("fetch", "--quiet", "origin", "main"),
                Duration.ofSeconds(30)
        );
        if (!fetch.success()) {
            setAvailable(false);
            return;
        }
        CommandResult behind = runGit(
                List.of("rev-list", "--count", "HEAD..origin/main"),
                Duration.ofSeconds(10)
        );
        try {
            setAvailable(behind.success() && Integer.parseInt(behind.output().strip()) > 0);
        } catch (NumberFormatException exception) {
            setAvailable(false);
        }
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
