package org.davidbohl.dirigent.utility.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProcessRunner {

    // Track active processes to ensure cleanup
    private final Map<Process, Long> activeProcesses = new ConcurrentHashMap<>();
    
    // Background thread to clean up any stale processes
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "process-cleanup-thread");
        t.setDaemon(true);
        return t;
    });

    public ProcessRunner() {
        // Start periodic cleanup task to catch any missed processes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleProcesses, 10, 10, TimeUnit.SECONDS);
        log.info("ProcessRunner initialized with background cleanup thread");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ProcessRunner and cleaning up {} remaining processes", activeProcesses.size());
        cleanupStaleProcesses();
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void cleanupStaleProcesses() {
        long now = System.currentTimeMillis();
        activeProcesses.entrySet().removeIf(entry -> {
            Process process = entry.getKey();
            long startTime = entry.getValue();
            
            // Check if process is still alive
            if (!process.isAlive()) {
                log.debug("Removing dead process from tracking (age: {}s)", (now - startTime) / 1000);
                return true;
            }
            
            // If process has been running for more than 2 minutes, kill it
            if ((now - startTime) > 120000) {
                log.warn("Killing stale process (age: {}s)", (now - startTime) / 1000);
                process.destroyForcibly();
                try {
                    process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
            return false;
        });
        
        if (!activeProcesses.isEmpty()) {
            log.debug("Active processes being tracked: {}", activeProcesses.size());
        }
    }

    public ProcessResult executeCommand(List<String> commandParts, long timeoutMs, Map<String, String> env)
            throws IOException {
        return executeInternal(commandParts, new File(System.getProperty("user.dir")), timeoutMs, env);
    }

    public ProcessResult executeCommand(List<String> commandParts)
            throws IOException {
        return executeInternal(commandParts, new File(System.getProperty("user.dir")), 0, Map.of());
    }

    public ProcessResult executeCommand(List<String> commandParts, Map<String, String> env)
            throws IOException {
        return executeInternal(commandParts, new File(System.getProperty("user.dir")), 0, env);
    }

    public ProcessResult executeCommand(List<String> commandParts, File workingDirectory)
            throws IOException {
        return executeInternal(commandParts, workingDirectory, 0, Map.of());
    }

    public ProcessResult executeCommand(List<String> commandParts, File workingDirectory, Map<String, String> env)
            throws IOException {
        return executeInternal(commandParts, workingDirectory, 0, env);
    }

    private ProcessResult executeInternal(List<String> commandParts, File workingDirectory, long timeoutMs, Map<String, String> env)
            throws IOException {

        Map<String, String> finalEnv = new HashMap<>();
        finalEnv.putAll(System.getenv());

        if(env != null && !env.isEmpty())
            finalEnv.putAll(env);

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        processBuilder.directory(workingDirectory);
        processBuilder.environment().putAll(finalEnv);
        processBuilder.redirectErrorStream(false);

        log.debug("Running command <{}> in directory {}", String.join(" ", commandParts), workingDirectory);

        Process process = null;
        int exitCode = 1;
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try {
            process = processBuilder.start();
            
            // Track this process immediately
            activeProcesses.put(process, System.currentTimeMillis());
            
            // Read stdout in separate thread
            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.debug("Error reading stdout", e);
                }
            }, "stdout-reader");
            stdoutReader.setDaemon(true);
            stdoutReader.start();

            // Read stderr in separate thread
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.debug("Error reading stderr", e);
                }
            }, "stderr-reader");
            stderrReader.setDaemon(true);
            stderrReader.start();

            // Wait for process with timeout
            boolean finished = process.waitFor(1, TimeUnit.MINUTES);
            
            if (!finished) {
                log.warn("Process timed out, killing it: {}", String.join(" ", commandParts));
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }

            // Wait for reader threads to finish
            stdoutReader.join(2000);
            stderrReader.join(2000);

            exitCode = process.exitValue();

        } catch (InterruptedException e) {
            log.warn("Process got interrupted: {}", String.join(" ", commandParts), e);
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                try {
                    process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            Thread.currentThread().interrupt();
        } finally {
            // CRITICAL: Always remove from tracking and ensure process is dead
            if (process != null) {
                activeProcesses.remove(process);
                
                // Double-check the process is actually dead
                if (process.isAlive()) {
                    log.warn("Process still alive in finally block, force killing");
                    process.destroyForcibly();
                    try {
                        process.waitFor(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        String stdoutString = stdout.toString().trim();
        String stderrString = stderr.toString().trim();

        log.debug("Finished command <{}>\nExit code: <{}>\nstdout: {}\nstderr: {}", 
            String.join(" ", commandParts), exitCode, stdoutString, stderrString);

        return new ProcessResult(exitCode, stdoutString, stderrString);
    }

}
