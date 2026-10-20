package org.davidbohl.dirigent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

class TlsBootstrapEnvironmentPostProcessorTest {

    @TempDir
    Path temporaryDirectory;

    private final TlsBootstrapEnvironmentPostProcessor processor = new TlsBootstrapEnvironmentPostProcessor();

    @Test
    void generatesAndReusesPrivateKeystoreByDefault() throws Exception {
        Path tlsDirectory = temporaryDirectory.resolve("tls");
        ConfigurableEnvironment firstEnvironment = environment(tlsDirectory);

        processor.prepare(firstEnvironment, true);

        Path keyStore = tlsDirectory.resolve("dirigent.p12");
        Path password = tlsDirectory.resolve("keystore.password");
        assertTrue(Files.exists(keyStore));
        assertTrue(Files.exists(password));
        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(tlsDirectory)));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(keyStore)));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(password)));
        assertEquals("true", firstEnvironment.getProperty("server.ssl.enabled"));
        assertEquals(8443, firstEnvironment.getProperty("server.port", Integer.class));
        assertEquals(keyStore.toUri().toString(), firstEnvironment.getProperty("server.ssl.key-store"));
        String passwordValue = Files.readString(password);
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(keyStore)) {
            store.load(input, passwordValue.toCharArray());
        }
        X509Certificate certificate = (X509Certificate) store.getCertificate("dirigent");
        Collection<List<?>> subjectAlternativeNames = certificate.getSubjectAlternativeNames();
        assertTrue(subjectAlternativeNames.stream().anyMatch(name -> name.get(1).equals("dirigent.example.test")));
        assertTrue(subjectAlternativeNames.stream().anyMatch(name -> name.get(1).equals("192.0.2.10")));
        byte[] expectedIpv6 = InetAddress.getByName("2001:db8::42").getAddress();
        assertTrue(containsIpSan(subjectAlternativeNames, expectedIpv6));

        ConfigurableEnvironment secondEnvironment = environment(tlsDirectory);
        processor.prepare(secondEnvironment, true);

        assertEquals(passwordValue, secondEnvironment.getProperty("server.ssl.key-store-password"));
        assertEquals(passwordValue, Files.readString(password));
    }

    @Test
    void honorsBringYourOwnKeystoreWithoutGeneratingMaterial() {
        Path supplied = temporaryDirectory.resolve("operator.p12");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.ssl.key-store", supplied.toUri().toString())
                .withProperty("server.ssl.key-store-password", "test-only-password");

        processor.prepare(environment, true);

        assertEquals(supplied.toUri().toString(), environment.getProperty("server.ssl.key-store"));
        assertFalse(Files.exists(temporaryDirectory.resolve("tls")));
        assertEquals(8443, environment.getProperty("server.port", Integer.class));
    }

    @Test
    void allowsExplicitHttpCompatibilityMode() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("dirigent.tls.enabled", "false")
                .withProperty("dirigent.tls.storage-directory", temporaryDirectory.resolve("tls").toString());

        processor.prepare(environment, true);

        assertFalse(Files.exists(temporaryDirectory.resolve("tls")));
        assertEquals("false", environment.getProperty("server.ssl.enabled"));
    }

    private ConfigurableEnvironment environment(Path tlsDirectory) {
        return new MockEnvironment()
                .withProperty("dirigent.tls.storage-directory", tlsDirectory.toString())
            .withProperty("dirigent.tls.hostnames", "dirigent.example.test,192.0.2.10,2001:db8::42");
    }

    private boolean containsIpSan(Collection<List<?>> names, byte[] expectedAddress) throws Exception {
        for (List<?> name : names) {
            if (Integer.valueOf(7).equals(name.get(0))
                    && Arrays.equals(expectedAddress, InetAddress.getByName(name.get(1).toString()).getAddress())) {
                return true;
            }
        }
        return false;
    }
}