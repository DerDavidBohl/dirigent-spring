package org.davidbohl.dirigent.utility.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProcessRunner {

    public ProcessResult executeCommand(List<String> commandParts, long timeoutMs, Map<String, String> env) {
        return executeInternal(commandParts, new File(System.getProperty("user.dir")), timeoutMs, env);
    }

    public ProcessResult executeCommand(List<String> commandParts) {
        return executeInternal(commandParts, new File(System.getProperty("user.dir")), 0, Map.of());
    }

    public ProcessResult executeCommand(List<String> commandParts, Map<String, String> env) {
        return executeInternal(commandParts, new File(System.getProperty("user.dir")), 0, env);
    }

    public ProcessResult executeCommand(List<String> commandParts, File workingDirectory) {
        return executeInternal(commandParts, workingDirectory, 0, Map.of());
    }

    public ProcessResult executeCommand(List<String> commandParts, File workingDirectory, Map<String, String> env) {
        return executeInternal(commandParts, workingDirectory, 0, env);
    }

    private ProcessResult executeInternal(List<String> commandParts, File workingDirectory, long timeoutMs, Map<String, String> env) {
    Map<String, String> finalEnv = new HashMap<>();
    finalEnv.putAll(System.getenv());
    if (env != null && !env.isEmpty()) {
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

        boolean finished = timeoutMs > 0
            ? process.waitFor(timeoutMs, TimeUnit.MILLISECONDS) 
            : process.waitFor() >= 0;
        
        if (!finished) {
            log.warn("Process timed out: {}", String.join(" ", commandParts));
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }

        // Wait for output streams to finish reading (but no need to close them manually—readStream handles it)
        stdoutReader.join(2000);
        stderrReader.join(2000);

        exitCode = process.exitValue();

    } catch (Throwable e) {
        log.warn("Process failed: {}", String.join(" ", commandParts), e);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    log.debug("Finished command <{}> with exit code {}", String.join(" ", commandParts), exitCode);

    return new ProcessResult(exitCode, stdout.toString().trim(), stderr.toString().trim());
}

    private Thread readStream(java.io.InputStream inputStream, StringBuilder output) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug(line);
                }
            } catch (IOException e) {
                log.warn("Error reading stream", e);
            }
        });
        reader.setDaemon(true);
        reader.start();
        return reader;
    }

}
