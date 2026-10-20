package org.davidbohl.dirigent.config;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@SuppressWarnings("removal")
public class TlsBootstrapEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Pattern DNS_NAME = Pattern.compile(
            "(?i)(?=.{1,253}$)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*");
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = PosixFilePermissions.fromString("rw-------");

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        prepare(environment, application.getWebApplicationType() != WebApplicationType.NONE);
    }

    void prepare(ConfigurableEnvironment environment, boolean webApplication) {
        if (!webApplication) {
            return;
        }
        if (!isTlsEnabled(environment)) {
            environment.getPropertySources().addFirst(new MapPropertySource("dirigentTlsDisabled",
                    Map.of("server.ssl.enabled", false)));
            return;
        }

        Map<String, Object> tlsProperties = new java.util.LinkedHashMap<>();
        if (environment.containsProperty("dirigent.tls.enabled")
                || !environment.containsProperty("server.ssl.enabled")) {
            tlsProperties.put("server.ssl.enabled", true);
        }
        if (!environment.containsProperty("server.port")) {
            tlsProperties.put("server.port", 8443);
        }

        String configuredKeyStore = environment.getProperty("server.ssl.key-store");
        if (configuredKeyStore == null || configuredKeyStore.isBlank()) {
            GeneratedKeyStore generated = loadOrCreateKeyStore(environment);
            tlsProperties.put("server.ssl.key-store", generated.path().toUri().toString());
            tlsProperties.put("server.ssl.key-store-type", "PKCS12");
            tlsProperties.put("server.ssl.key-store-password", generated.password());
        }
        environment.getPropertySources().addFirst(new MapPropertySource("dirigentTlsBootstrap", tlsProperties));
    }

    private boolean isTlsEnabled(ConfigurableEnvironment environment) {
        String dirigentTls = environment.getProperty("dirigent.tls.enabled");
        if (dirigentTls != null) {
            return Boolean.parseBoolean(dirigentTls);
        }
        String serverTls = environment.getProperty("server.ssl.enabled");
        return serverTls == null || Boolean.parseBoolean(serverTls);
    }

    private GeneratedKeyStore loadOrCreateKeyStore(ConfigurableEnvironment environment) {
        Path directory = Path.of(environment.getProperty("dirigent.tls.storage-directory", "data/tls"))
                .toAbsolutePath().normalize();
        Path keyStore = directory.resolve("dirigent.p12");
        Path passwordFile = directory.resolve("keystore.password");
        Path lockFile = directory.resolve("bootstrap.lock");
        try {
            createPrivateDirectory(directory);
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                setPrivateFilePermissions(lockFile);
                try (FileLock lock = channel.lock()) {
                    if (!lock.isValid()) {
                        throw new IOException("Could not lock TLS certificate storage");
                    }
                    if (Files.exists(keyStore)) {
                        if (!Files.exists(passwordFile)) {
                            throw new IllegalStateException("Generated TLS keystore password is missing");
                        }
                        setPrivateFilePermissions(keyStore);
                        setPrivateFilePermissions(passwordFile);
                        return new GeneratedKeyStore(keyStore, Files.readString(passwordFile).trim());
                    }
                    String password = loadOrCreatePassword(passwordFile);
                    Path temporaryKeyStore = directory.resolve("dirigent-" + java.util.UUID.randomUUID() + ".p12");
                    try {
                        generateKeyStore(temporaryKeyStore, password, hostNames(environment));
                        validateKeyStore(temporaryKeyStore, password);
                        setPrivateFilePermissions(temporaryKeyStore);
                        try {
                            Files.move(temporaryKeyStore, keyStore, StandardCopyOption.ATOMIC_MOVE);
                        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                            Files.move(temporaryKeyStore, keyStore);
                        }
                    } finally {
                        Files.deleteIfExists(temporaryKeyStore);
                    }
                    setPrivateFilePermissions(keyStore);
                    return new GeneratedKeyStore(keyStore, password);
                }
            }
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Could not prepare persistent HTTPS certificate material", exception);
        }
    }

    private String loadOrCreatePassword(Path passwordFile) throws IOException {
        if (Files.exists(passwordFile)) {
            return Files.readString(passwordFile).trim();
        }
        byte[] random = new byte[48];
        new SecureRandom().nextBytes(random);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        try {
            Files.createFile(passwordFile, PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS));
        } catch (UnsupportedOperationException exception) {
            Files.createFile(passwordFile);
            setPrivateFilePermissions(passwordFile);
        }
        Files.writeString(passwordFile, password, StandardOpenOption.WRITE);
        setPrivateFilePermissions(passwordFile);
        return password;
    }

    private void generateKeyStore(Path keyStore, String password, List<String> hostNames)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-noprompt", "-alias", "dirigent",
                "-keyalg", "RSA", "-keysize", "3072", "-storetype", "PKCS12",
                "-keystore", keyStore.toString(), "-storepass:env", "DIRIGENT_TLS_KEYSTORE_PASSWORD",
                "-keypass:env", "DIRIGENT_TLS_KEYSTORE_PASSWORD", "-validity", "365",
                "-dname", "CN=Dirigent", "-ext", "SAN=" + String.join(",", hostNames)));
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.environment().put("DIRIGENT_TLS_KEYSTORE_PASSWORD", password);
        Process process = builder.start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("TLS certificate generation timed out");
        }
        if (process.exitValue() != 0) {
            Files.deleteIfExists(keyStore);
            throw new IOException("TLS certificate generation failed");
        }
    }

    private void validateKeyStore(Path keyStorePath, String password) throws IOException {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (var input = Files.newInputStream(keyStorePath)) {
                keyStore.load(input, password.toCharArray());
            }
            if (!(keyStore.getCertificate("dirigent") instanceof X509Certificate certificate)
                    || certificate.getSubjectAlternativeNames() == null
                    || certificate.getSubjectAlternativeNames().isEmpty()) {
                throw new IOException("Generated TLS certificate is missing its subject alternative names");
            }
        } catch (java.security.GeneralSecurityException exception) {
            throw new IOException("Generated TLS keystore is invalid", exception);
        }
    }

    private List<String> hostNames(ConfigurableEnvironment environment) {
        String configured = environment.getProperty("dirigent.tls.hostnames", "localhost,127.0.0.1");
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String rawName : configured.split(",")) {
            String name = rawName.trim();
            if (isIpv4(name)) {
                names.add("ip:" + name);
            } else if (isIpv6(name)) {
                names.add("ip:" + name);
            } else if (name.matches("[0-9.]+") || name.contains(":")) {
                throw new IllegalArgumentException("Invalid advertised TLS hostname");
            } else if (DNS_NAME.matcher(name).matches()) {
                names.add("dns:" + name);
            } else {
                throw new IllegalArgumentException("Invalid advertised TLS hostname");
            }
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("At least one TLS hostname is required");
        }
        names.add("dns:localhost");
        names.add("ip:127.0.0.1");
        return List.copyOf(names);
    }

    private boolean isIpv4(String value) {
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (!octet.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(octet) > 255) {
                return false;
            }
        }
        return true;
    }

    private boolean isIpv6(String value) {
        if (!value.contains(":") || !value.matches("[0-9A-Fa-f:.]+")) {
            return false;
        }
        try {
            return InetAddress.getByName(value) instanceof Inet6Address;
        } catch (java.net.UnknownHostException exception) {
            return false;
        }
    }

    private void createPrivateDirectory(Path directory) throws IOException {
        try {
            Files.createDirectories(directory, PosixFilePermissions.asFileAttribute(DIRECTORY_PERMISSIONS));
        } catch (UnsupportedOperationException exception) {
            Files.createDirectories(directory);
            setPrivateDirectoryPermissions(directory);
        }
        setPrivateDirectoryPermissions(directory);
    }

    private void setPrivateDirectoryPermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, DIRECTORY_PERMISSIONS);
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private void setPrivateFilePermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, FILE_PERMISSIONS);
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private record GeneratedKeyStore(Path path, String password) {
    }
}