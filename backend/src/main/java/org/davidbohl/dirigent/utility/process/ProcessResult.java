package org.davidbohl.dirigent.utility.process;

public record ProcessResult(int exitCode, String stdout, String stderr) {}