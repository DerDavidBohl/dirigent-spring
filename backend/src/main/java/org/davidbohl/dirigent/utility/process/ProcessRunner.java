package org.davidbohl.dirigent.utility.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProcessRunner {

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
        if(env != null && !env.isEmpty()) {
            finalEnv.putAll(env);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        processBuilder.directory(workingDirectory);
        processBuilder.environment().putAll(finalEnv);

        log.debug("Running command <{}> in directory {}", String.join(" ", commandParts), workingDirectory);

        Process process = null;
        int exitCode = 1;
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try {
            process = processBuilder.start();
            
            // Read stdout
            Thread stdoutReader = readStream(process.getInputStream(), stdout);
            // Read stderr
            Thread stderrReader = readStream(process.getErrorStream(), stderr);

            // Wait for process to complete (1 minute timeout)
            boolean finished = process.waitFor(1, TimeUnit.MINUTES);
            
            if (!finished) {
                log.warn("Process timed out: {}", String.join(" ", commandParts));
                killProcessTree(process);
            }

            // Wait for output streams to finish reading
            stdoutReader.join(2000);
            stderrReader.join(2000);

            exitCode = process.exitValue();

        } catch (InterruptedException e) {
            log.warn("Process interrupted: {}", String.join(" ", commandParts), e);
            if (process != null && process.isAlive()) {
                killProcessTree(process);
            }
            Thread.currentThread().interrupt();
        } finally {
            // Ensure process tree is terminated and streams are closed
            if (process != null) {
                if (process.isAlive()) {
                    log.warn("Force killing remaining process tree");
                    killProcessTree(process);
                }
                // Close all streams to release resources and prevent leaks
                closeQuietly(process.getInputStream());
                closeQuietly(process.getOutputStream());
                closeQuietly(process.getErrorStream());
            }
        }

        log.debug("Finished command <{}> with exit code {}", String.join(" ", commandParts), exitCode);

        return new ProcessResult(exitCode, stdout.toString().trim(), stderr.toString().trim());
    }

    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Kills the entire process tree to prevent orphaned child processes
     */
    private void killProcessTree(Process process) {
        // First, wait for any child processes to finish and reap them
        process.descendants().forEach(child -> {
            if (child.isAlive()) {
                log.debug("Killing child process: {}", child.pid());
                child.destroyForcibly();
                try {
                    child.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        // Kill the parent process
        process.destroyForcibly();
        
        // Wait for parent to die
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Thread readStream(java.io.InputStream inputStream, StringBuilder output) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                log.debug("Error reading stream", e);
            }
        });
        reader.setDaemon(true);
        reader.start();
        return reader;
    }

}
