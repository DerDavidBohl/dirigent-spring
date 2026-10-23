package org.davidbohl.dirigent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// REQ-021, SEC-016: Generated certificates renew before expiry; the SSL bundle reloads the new keystore.
@Component
@ConditionalOnProperty(name = "dirigent.tls.generated", havingValue = "true")
public class TlsCertificateRotationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TlsCertificateRotationScheduler.class);

    private final ConfigurableEnvironment environment;
    private final TlsBootstrapEnvironmentPostProcessor processor = new TlsBootstrapEnvironmentPostProcessor();

    public TlsCertificateRotationScheduler(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Scheduled(fixedDelayString = "${dirigent.tls.rotation.check-interval:PT12H}", initialDelayString = "${dirigent.tls.rotation.check-interval:PT12H}")
    void rotateIfDue() {
        try {
            String fingerprint = processor.rotateIfDue(environment);
            if (fingerprint != null) {
                logger.warn("Rotated generated TLS certificate; new SHA-256 fingerprint {}. Update federation pins.",
                        fingerprint);
            }
        } catch (RuntimeException exception) {
            logger.error("Automatic TLS certificate rotation failed", exception);
        }
    }
}
