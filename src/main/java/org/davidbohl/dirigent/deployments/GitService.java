package org.davidbohl.dirigent.deployments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Service
public class GitService {

    private final Logger logger = LoggerFactory.getLogger(GitService.class);

    @Value("${dirigent.git.authToken}")
    private String authToken;

    public void cloneOrPull(String source, String destination) throws IOException, InterruptedException {

        logger.info("Cloning or pulling git repository '{}' to dir '{}'", source, destination);

        File destinationDir = new File(destination);

        if (destinationDir.exists()) {
            logger.debug("Local Repo exists. Pulling latest changes.");
            new ProcessBuilder("git", "fetch", "--all")
                    .directory(destinationDir).start().waitFor();
            new ProcessBuilder("git", "reset", "--hard", "HEAD")
                    .directory(destinationDir).start().waitFor();
            new ProcessBuilder("git", "pull")
                    .directory(destinationDir).start().waitFor();
        } else {
            logger.debug("Local Repo does not exist. Cloning repository.");
            deleteDirectory(destinationDir);
            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(source);

            if (uriComponentsBuilder.build().getUserInfo() == null || Objects.requireNonNull(uriComponentsBuilder.build().getUserInfo()).isEmpty()) {
                uriComponentsBuilder = uriComponentsBuilder.userInfo(authToken);
            }


            new ProcessBuilder("git", "clone", uriComponentsBuilder.toUriString(), destination)
                    .start().waitFor();
        }
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}


