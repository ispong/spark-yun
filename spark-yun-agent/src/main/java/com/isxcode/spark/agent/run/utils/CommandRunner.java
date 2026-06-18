package com.isxcode.spark.agent.run.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class CommandRunner {

    private CommandRunner() {

    }

    public static CommandResult run(List<String> command, Duration timeout) throws IOException, InterruptedException {

        Process process = new ProcessBuilder(command).start();
        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> read(process.getInputStream()));
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            String stdout = getFutureValue(stdoutFuture);
            String stderr = getFutureValue(stderrFuture);
            throw new IOException(
                "Command timed out: " + String.join(" ", command) + "\n" + stdout + "\n" + stderr);
        }

        return new CommandResult(process.exitValue(), getFutureValue(stdoutFuture), getFutureValue(stderrFuture));
    }

    private static String read(InputStream inputStream) {

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (IOException e) {
            output.append(e.getMessage()).append("\n");
        }
        return output.toString();
    }

    private static String getFutureValue(CompletableFuture<String> future) throws InterruptedException {

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            return e.getMessage();
        }
    }

    public static class CommandResult {

        private final int exitCode;

        private final String stdout;

        private final String stderr;

        public CommandResult(int exitCode, String stdout, String stderr) {

            this.exitCode = exitCode;
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
        }

        public int getExitCode() {

            return exitCode;
        }

        public String getStdout() {

            return stdout;
        }

        public String getStderr() {

            return stderr;
        }

        public boolean isSuccess() {

            return exitCode == 0;
        }

        public String getOutput() {

            return stdout + stderr;
        }
    }
}
