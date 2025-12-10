package org.davidbohl.dirigent.utility.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProcessRunner {

    // Track active result handlers to ensure cleanup
    private final Map<DefaultExecuteResultHandler, Long> activeHandlers = new ConcurrentHashMap<>();
    
    // Background thread to clean up any stale processes
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "process-cleanup-thread");
        t.setDaemon(true);
        return t;
    });

    public ProcessRunner() {
        // Start periodic cleanup task to catch any missed processes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleHandlers, 30, 30, TimeUnit.SECONDS);
        log.info("ProcessRunner initialized with background cleanup thread");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ProcessRunner and cleaning up remaining processes");
        cleanupStaleHandlers();
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

    private void cleanupStaleHandlers() {
        long now = System.currentTimeMillis();
        activeHandlers.entrySet().removeIf(entry -> {
            DefaultExecuteResultHandler handler = entry.getKey();
            long startTime = entry.getValue();
            
            // If process has been running for more than 2 minutes, consider it stale
            if (handler.hasResult() || (now - startTime) > 120000) {
                log.debug("Cleaning up stale process handler (hasResult: {}, age: {}s)", 
                    handler.hasResult(), (now - startTime) / 1000);
                return true;
            }
            return false;
        });
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

        if(env != null && env.size() > 0)
            finalEnv.putAll(env);

        CommandLine command = new CommandLine(commandParts.get(0));
        for (int i = 1; i < commandParts.size(); i++) {
            command.addArgument(commandParts.get(i));
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(stdout, stderr);

        ExecuteWatchdog watchdog = ExecuteWatchdog.builder()
                    .setTimeout(Duration.ofMinutes(1))
                    .get();

        int exitCode = 1;
        String stdoutString = "";
        String stderrString = "";

        Executor executor = DefaultExecutor.builder().get();
        executor.setStreamHandler(streamHandler);
        executor.setWorkingDirectory(workingDirectory);
        executor.setWatchdog(watchdog);
        executor.setExitValue(1);
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        
        // Track this handler for cleanup
        activeHandlers.put(resultHandler, System.currentTimeMillis());

        log.debug("Running command <{}>", String.join(" ", commandParts));

        try {
            executor.execute(command, finalEnv, resultHandler);

            try {
                resultHandler.waitFor(Duration.ofMinutes(1));
            } catch (InterruptedException e) {
                log.warn("Process got interrupted", e);
                Thread.currentThread().interrupt();
                // Ensure process is killed on interrupt
                watchdog.destroyProcess();
            }
            
            // Only destroy if the process is still running (hasn't completed naturally)
            if (!resultHandler.hasResult()) {
                log.debug("Process timeout - destroying process");
                watchdog.destroyProcess();
                
                // Give it a moment to terminate, then force if needed
                try {
                    resultHandler.waitFor(Duration.ofSeconds(2));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            exitCode = resultHandler.getExitValue();
            
        } finally {
            // Remove from active tracking
            activeHandlers.remove(resultHandler);
            
            // Ensure streams are closed
            streamHandler.stop();
            
            try {
                stdout.close();
            } catch (IOException ignored) {
            }
            try {
                stderr.close();
            } catch (IOException ignored) {
            }
        }

        stdoutString = stdout.toString(StandardCharsets.UTF_8);
        stderrString = stderr.toString(StandardCharsets.UTF_8);

        log.debug("Finished command <{}>\nExit code: <{}>\nstdout: {}\nstderr: {}", 
            String.join(" ", commandParts), exitCode, stdoutString, stderrString);
        
        log.debug("Process killed: {}", watchdog.killedProcess());

        return new ProcessResult(exitCode, stdoutString, stderrString);
    }

}
