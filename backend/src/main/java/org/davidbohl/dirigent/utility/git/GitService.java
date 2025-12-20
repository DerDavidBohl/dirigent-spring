package org.davidbohl.dirigent.utility.git;

import org.davidbohl.dirigent.utility.process.ProcessResult;
import org.davidbohl.dirigent.utility.process.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GitService {

    private final Logger logger = LoggerFactory.getLogger(GitService.class);

    @Value("${dirigent.git.authToken}")
    private String authToken;

    private final ProcessRunner processRunner;

    public boolean updateRepo(String repoUrl, String targetDir, String rev) throws IOException, InterruptedException {

        logger.info("Cloning or pulling git repository '{}' to dir '{}' @ rev '{}'", repoUrl, targetDir, rev);

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

            processRunner.executeCommand(List.of("git", "reset", "--hard", "HEAD"), destinationDir);
            processRunner.executeCommand(List.of("git", "fetch", "--all"), destinationDir);
            processRunner.executeCommand(List.of("git", "checkout", rev), destinationDir);
            processRunner.executeCommand(List.of("git", "pull"), destinationDir);



            String newHeadRev = getHeadRev(destinationDir);
            changed = !currentHeadRev.equals(newHeadRev);
        } else {
            changed = true;
            logger.debug("Local Repo does not exist. Cloning repository.");
            ensureFileOrDirectoryIsDeletedRecursive(destinationDir);

            ProcessResult gitClone = processRunner.executeCommand(List.of("git", "clone", remoteGitUri, targetDir));

            if (gitClone.exitCode() != 0) {
                throw new IOException("Git clone failed with exit code " + gitClone.exitCode() + ": " + gitClone.stderr());
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

    private String getCurrentGitRemoteUrl(File destinationDir) throws IOException, InterruptedException {

        return processRunner.executeCommand(List.of("git", "config", "--get", "remote.origin.url"), destinationDir).stdout().trim();
    }

    private String getHeadRev(File destinationDir) throws IOException, InterruptedException {
        return processRunner.executeCommand(List.of("git", "rev-parse", "HEAD"), destinationDir).stdout().trim();
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


