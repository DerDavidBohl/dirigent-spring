package org.davidbohl.dirigent.federation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

class PinnedCertificateTrustManagerTest {

    @Test
    void acceptsOnlyTheValidPinnedLeafCertificate() throws Exception {
        byte[] encodedCertificate = {1, 2, 3, 4};
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encodedCertificate));
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getEncoded()).thenReturn(encodedCertificate);
        doNothing().when(certificate).checkValidity();
        PinnedCertificateTrustManager trustManager = new PinnedCertificateTrustManager(fingerprint);

        assertDoesNotThrow(() -> trustManager.checkServerTrusted(new X509Certificate[]{certificate}, "RSA"));
    }

    @Test
    void rejectsWrongPinsAndExpiredCertificates() throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getEncoded()).thenReturn(new byte[]{1, 2, 3, 4});
        doNothing().when(certificate).checkValidity();
        PinnedCertificateTrustManager wrongPin = new PinnedCertificateTrustManager("0".repeat(64));
        assertThrows(CertificateException.class,
                () -> wrongPin.checkServerTrusted(new X509Certificate[]{certificate}, "RSA"));

        doThrow(new CertificateExpiredException()).when(certificate).checkValidity();
        PinnedCertificateTrustManager matchingPin = new PinnedCertificateTrustManager(
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(new byte[]{1, 2, 3, 4})));
        assertThrows(CertificateExpiredException.class,
                () -> matchingPin.checkServerTrusted(new X509Certificate[]{certificate}, "RSA"));
    }

    @Test
    void rejectsMalformedFingerprint() {
        assertThrows(IllegalArgumentException.class, () -> new PinnedCertificateTrustManager("not-a-fingerprint"));
    }
}