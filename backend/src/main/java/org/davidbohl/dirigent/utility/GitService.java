package org.davidbohl.dirigent.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

@Service
public class GitService {

    private final Logger logger = LoggerFactory.getLogger(GitService.class);

    @Value("${dirigent.git.authToken}")
    private String authToken;

    public boolean updateRepo(String repoUrl, String targetDir, String rev) throws IOException, InterruptedException {

        logger.info("Cloning or pulling git repository '{}' to dir '{}'", repoUrl, targetDir);

        File destinationDir = new File(targetDir);

        boolean changed;


        String remoteGitUri = enrichUriWithAuthTokenIfConfigured(repoUrl);

        boolean repoTargetDirExistsAndIsGitRepo = false;
        if (destinationDir.exists()) {
            String[] dirList = destinationDir.list();
            if (dirList != null) {
                repoTargetDirExistsAndIsGitRepo = Arrays.asList(dirList).contains(".git");
            }
        }

        boolean repoHasCorrectRemote = false;
        if (repoTargetDirExistsAndIsGitRepo) {
            repoHasCorrectRemote = getCurrentGitRemoteUrl(destinationDir).equals(remoteGitUri);
        }

        boolean onlyPullNeeded = repoTargetDirExistsAndIsGitRepo && repoHasCorrectRemote;
        if (onlyPullNeeded) {
            logger.debug("Local Repo exists. Pulling latest changes.");

            String currentHeadRev = getHeadRev(destinationDir);

            new ProcessBuilder("git", "reset", "--hard", "HEAD")
                    .directory(destinationDir).start().waitFor();
            new ProcessBuilder("git", "fetch", "--all")
                    .directory(destinationDir).start().waitFor();
            new ProcessBuilder("git", "checkout", rev)
                    .directory(destinationDir).start().waitFor();
            new ProcessBuilder("git", "pull")
                    .directory(destinationDir).start().waitFor();

            String newHeadRev = getHeadRev(destinationDir);
            changed = !currentHeadRev.equals(newHeadRev);
        } else {
            changed = true;
            logger.debug("Local Repo does not exist. Cloning repository.");
            ensureFileOrDirectoryIsDeletedRecursive(destinationDir);
            Process process = new ProcessBuilder("git", "clone", remoteGitUri, targetDir)
                    .start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append(System.lineSeparator());
                }
                throw new IOException("Git clone failed with exit code " + exitCode + ": " + stringBuilder);
            }

        }

        return changed;
    }

    private String enrichUriWithAuthTokenIfConfigured(String repoUri) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(repoUri);

        if (!authToken.isBlank() && (uriComponentsBuilder.build().getUserInfo() == null || Objects.requireNonNull(uriComponentsBuilder.build().getUserInfo()).isEmpty())) {
            uriComponentsBuilder = uriComponentsBuilder.userInfo(authToken);
        }

        return uriComponentsBuilder.toUriString();
    }

    private static String getCurrentGitRemoteUrl(File destinationDir) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "config", "--get", "remote.origin.url")
                .directory(destinationDir).start();
        return getStdOutFromProcess(process).trim();
    }

    private static String getHeadRev(File destinationDir) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(destinationDir).start();
        return getStdOutFromProcess(process);
    }

    private static String getStdOutFromProcess(Process process) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        String currentRev = stringBuilder.toString();
        process.waitFor();
        return currentRev;
    }

    private static void ensureFileOrDirectoryIsDeletedRecursive(File directoryOrFileToBeDeleted) {
        File[] allContents = directoryOrFileToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                ensureFileOrDirectoryIsDeletedRecursive(file);
            }
        }
        directoryOrFileToBeDeleted.delete(); // TODO: handle failure
    }
}


