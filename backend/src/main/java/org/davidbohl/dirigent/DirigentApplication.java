package org.davidbohl.dirigent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
@EnableConfigurationProperties
public class DirigentApplication {

	static Logger logger = LoggerFactory.getLogger(DirigentApplication.class);


	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(DirigentApplication.class, args);
		String composeCommand = context.getEnvironment().getProperty("dirigent.compose.command");
		if(!isComposeInstalled(composeCommand)) {
			logger.error("Compose is not installed. Please install it and try again. Your compose command is: {}", composeCommand);
			System.exit(1);
		}
	}


	private static boolean isComposeInstalled(String composeCommand) {
		try {
			Process process = Runtime.getRuntime().exec((composeCommand + " --version").split(" "));
			int exitCode = process.waitFor();
			return exitCode == 0;
		} catch (Exception e) {
            logger.error("Compose is not installed. Please install it and try again.", e);
			return false;
		}
	}

}
