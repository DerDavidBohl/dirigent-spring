package org.davidbohl.dirigent.deployments.utility;

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

    public boolean updateRepo(String source, String destination) throws IOException, InterruptedException {

        logger.info("Cloning or pulling git repository '{}' to dir '{}'", source, destination);

        File destinationDir = new File(destination);

        boolean changed = false;

        if (destinationDir.exists() && Arrays.asList(Objects.requireNonNull(destinationDir.list())).contains(".git")) {
            logger.debug("Local Repo exists. Pulling latest changes.");

            String currentHeadRev = getHeadRev(destinationDir);
            new ProcessBuilder("git", "fetch", "--all")
                    .directory(destinationDir).start().waitFor();
            new ProcessBuilder("git", "reset", "--hard", "HEAD")
                    .directory(destinationDir).start().waitFor();
            new ProcessBuilder("git", "pull")
                    .directory(destinationDir).start().waitFor();
            String newHeadRev = getHeadRev(destinationDir);
            changed = !currentHeadRev.equals(newHeadRev);
        } else {
            changed = true;
            logger.debug("Local Repo does not exist. Cloning repository.");
            deleteDirectory(destinationDir);
            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(source);

            if (!authToken.isBlank() && (uriComponentsBuilder.build().getUserInfo() == null || Objects.requireNonNull(uriComponentsBuilder.build().getUserInfo()).isEmpty())) {
                uriComponentsBuilder = uriComponentsBuilder.userInfo(authToken);
            }


            new ProcessBuilder("git", "clone", uriComponentsBuilder.toUriString(), destination)
                    .start().waitFor();
        }

        return changed;
    }

    private static String getHeadRev(File destinationDir) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(destinationDir).start();
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

    void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}


