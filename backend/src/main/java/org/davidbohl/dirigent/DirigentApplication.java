package org.davidbohl.dirigent;

import org.davidbohl.dirigent.auth.OperatorAccountService;
import org.davidbohl.dirigent.auth.MachineTokenService;
import org.davidbohl.dirigent.auth.dto.MachineTokenCreateRequest;
import org.davidbohl.dirigent.auth.dto.MachineTokenCreatedResponse;
import org.davidbohl.dirigent.federation.FederationProperties;
import org.davidbohl.dirigent.deployments.updates.RegistryAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.Console;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableConfigurationProperties({RegistryAuthProperties.class, FederationProperties.class})
public class DirigentApplication {

	static Logger logger = LoggerFactory.getLogger(DirigentApplication.class);

	public static void main(String[] args) {
		int createMachineTokenIndex = indexOf(args, "--create-machine-token");
		if (createMachineTokenIndex >= 0) {
			MachineTokenCreateRequest request = parseMachineTokenRequest(args, createMachineTokenIndex);
			String[] springArguments = withoutCliArguments(args, createMachineTokenIndex, 6);
			try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DirigentApplication.class)
					.web(WebApplicationType.NONE)
					.properties("spring.main.banner-mode=off")
					.run(withBootstrapDefaults(springArguments))) {
				createMachineToken(request, context.getBean(MachineTokenService.class), System.out);
			}
			return;
		}
		int createAdminIndex = indexOf(args, "--create-admin");
		if (createAdminIndex >= 0) {
			if (createAdminIndex + 1 >= args.length) {
				throw new IllegalArgumentException("Usage: --create-admin <username>");
			}
			createAdmin(args[createAdminIndex + 1], withoutCliArguments(args, createAdminIndex, 2));
			return;
		}
		int resetPasswordIndex = indexOf(args, "--reset-admin-password");
		if (resetPasswordIndex >= 0) {
			if (resetPasswordIndex + 1 >= args.length) {
				throw new IllegalArgumentException("Usage: --reset-admin-password <username>");
			}
			resetAdminPassword(args[resetPasswordIndex + 1], withoutCliArguments(args, resetPasswordIndex, 2));
			return;
		}
		int deleteLocalAccountIndex = indexOf(args, "--delete-local-account");
		if (deleteLocalAccountIndex >= 0) {
			if (deleteLocalAccountIndex + 1 >= args.length) {
				throw new IllegalArgumentException("Usage: --delete-local-account <username>");
			}
			deleteLocalAccount(args[deleteLocalAccountIndex + 1],
					withoutCliArguments(args, deleteLocalAccountIndex, 2));
			return;
		}
		ConfigurableApplicationContext context = SpringApplication.run(DirigentApplication.class, args);
		String composeCommand = context.getEnvironment().getProperty("dirigent.compose.command");
		if (!isComposeInstalled(composeCommand)) {
			logger.error("Compose is not installed. Please install it and try again. Your compose command is: {}",
					composeCommand);
			System.exit(1);
		}
	}

	static MachineTokenCreateRequest parseMachineTokenRequest(String[] args, int commandIndex) {
		if (commandIndex + 5 >= args.length
				|| !"--token-permissions".equals(args[commandIndex + 2])
				|| !"--token-expires-at".equals(args[commandIndex + 4])) {
			throw new IllegalArgumentException("Usage: --create-machine-token <name> "
					+ "--token-permissions <permission,...> --token-expires-at <ISO-8601 instant>");
		}
		Set<String> permissions = Arrays.stream(args[commandIndex + 3].split(",", -1))
				.map(permission -> permission.trim())
				.filter(permission -> !permission.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
		Instant expiresAt;
		try {
			expiresAt = Instant.parse(args[commandIndex + 5]);
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("Token expiry must be an ISO-8601 instant, for example 2027-01-01T00:00:00Z");
		}
		return new MachineTokenCreateRequest(args[commandIndex + 1], permissions, expiresAt);
	}

	static void createMachineToken(MachineTokenCreateRequest request, MachineTokenService tokenService,
			PrintStream output) {
		MachineTokenCreatedResponse created = tokenService.create(request, "server-cli");
		output.printf("Machine token created: %s%n", created.name());
		output.printf("Permissions: %s%n", created.permissions().stream().sorted().collect(Collectors.joining(", ")));
		output.printf("Expires at: %s%n", created.expiresAt());
		output.printf("Token: %s%n", created.token());
	}

	private static void createAdmin(String username, String[] springArguments) {
		Console console = System.console();
		if (console == null) {
			throw new IllegalStateException("Admin bootstrap requires an interactive terminal");
		}
		String passwordValue = readConfirmedPassword(console);

		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DirigentApplication.class)
				.web(WebApplicationType.NONE)
				.properties("spring.main.banner-mode=off")
				.run(withBootstrapDefaults(springArguments))) {
			context.getBean(OperatorAccountService.class).createLocalAccount(username, passwordValue);
			console.printf("Administrator account created.%n");
		}
	}

	private static void resetAdminPassword(String username, String[] springArguments) {
		Console console = requireInteractiveConsole();
		String password = readConfirmedPassword(console);
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DirigentApplication.class)
				.web(WebApplicationType.NONE)
				.properties("spring.main.banner-mode=off")
				.run(withBootstrapDefaults(springArguments))) {
			context.getBean(OperatorAccountService.class).resetLocalPassword(username, password);
			console.printf("Local administrator password reset.%n");
		}
	}

	private static void deleteLocalAccount(String username, String[] springArguments) {
		Console console = requireInteractiveConsole();
		String confirmation = console.readLine("Type the account name '%s' to confirm deletion: ", username);
		if (confirmation == null || !confirmation.trim().equalsIgnoreCase(username.trim())) {
			throw new IllegalArgumentException("Account deletion was not confirmed");
		}
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DirigentApplication.class)
				.web(WebApplicationType.NONE)
				.properties("spring.main.banner-mode=off")
				.run(withBootstrapDefaults(springArguments))) {
			context.getBean(OperatorAccountService.class).deleteLocalAccount(username);
			console.printf("Local administrator account deleted.%n");
		}
	}

	private static Console requireInteractiveConsole() {
		Console console = System.console();
		if (console == null) {
			throw new IllegalStateException("Account maintenance requires an interactive terminal");
		}
		return console;
	}

	private static String readConfirmedPassword(Console console) {
		char[] password = console.readPassword("Password (12 to 72 UTF-8 bytes): ");
		char[] confirmation = console.readPassword("Confirm password: ");
		try {
			if (password == null || confirmation == null || !Arrays.equals(password, confirmation)) {
				throw new IllegalArgumentException("Passwords do not match");
			}
			return new String(password);
		} finally {
			Arrays.fill(password == null ? new char[0] : password, '\0');
			Arrays.fill(confirmation == null ? new char[0] : confirmation, '\0');
		}
	}

	private static int indexOf(String[] args, String option) {
		for (int index = 0; index < args.length; index++) {
			if (option.equals(args[index])) {
				return index;
			}
		}
		return -1;
	}

	private static String[] withoutCliArguments(String[] args, int cliIndex, int argumentCount) {
		return java.util.stream.IntStream.range(0, args.length)
				.filter(index -> index < cliIndex || index >= cliIndex + argumentCount)
				.mapToObj(index -> args[index])
				.toArray(String[]::new);
	}

	private static String[] withBootstrapDefaults(String[] springArguments) {
		java.util.List<String> arguments = new java.util.ArrayList<>(java.util.List.of(springArguments));
		arguments.add("--spring.main.web-application-type=none");
		arguments.add("--dirigent.start.all.on.startup=false");
		arguments.add("--dirigent.deployments.schedule.enabled=false");
		return arguments.toArray(String[]::new);
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
