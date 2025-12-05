package org.davidbohl.dirigent.utility.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.stereotype.Component;

@Component
public class ProcessRunner {

    public ProcessResult executeCommand(List<String> commandParts, long timeoutMs, Map<String, String> env)
            throws IOException {
        return executeInternal(commandParts, new File(System.getProperty("user.dir")), timeoutMs, env);
    }

    public ProcessResult executeCommand(List<String> commandParts)
            throws IOException {
        return executeInternal(commandParts, new File(System.getProperty("user.dir")), 0, Map.of());
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

        Map<String, String> finalEnv = System.getenv();

        if(env != null && env.size() > 0)
            finalEnv.putAll(env);

        CommandLine command = new CommandLine(commandParts.get(0));
        for (int i = 1; i < commandParts.size(); i++) {
            command.addArgument(commandParts.get(i));
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(stdout, stderr);

        Executor executor = DefaultExecutor.builder().get();
        executor.setExitValue(0);
        executor.setStreamHandler(streamHandler);
        executor.setWorkingDirectory(workingDirectory);

        ExecuteWatchdog watchdog = null;
        if (timeoutMs > 0) {
            watchdog = ExecuteWatchdog.builder()
                    .setTimeout(Duration.ofMillis(timeoutMs))
                    .get();
            executor.setWatchdog(watchdog);
        }

        int exitCode = -1;
        try {
            exitCode = executor.execute(command, finalEnv);
        } catch (ExecuteException e) {
            exitCode = e.getExitValue();
        } catch (InterruptedIOException e) {
            throw e;
        } finally {

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

        String stdoutString = stdout.toString(StandardCharsets.UTF_8);
        String stderrString = stderr.toString(StandardCharsets.UTF_8);

        return new ProcessResult(exitCode, stdoutString, stderrString);
    }

}
