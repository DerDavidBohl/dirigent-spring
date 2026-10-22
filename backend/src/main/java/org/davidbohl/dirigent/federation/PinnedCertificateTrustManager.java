package org.davidbohl.dirigent.federation;

import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.Locale;

import javax.net.ssl.X509TrustManager;

final class PinnedCertificateTrustManager implements X509TrustManager {

    private final byte[] expectedFingerprint;

    PinnedCertificateTrustManager(String fingerprint) {
        this.expectedFingerprint = HexFormat.of().parseHex(normalizeFingerprint(fingerprint));
    }

    static String normalizeFingerprint(String fingerprint) {
        if (fingerprint == null) {
            throw new IllegalArgumentException("Certificate fingerprint is required");
        }
        String normalized = fingerprint.replace(":", "").trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Certificate fingerprint must be a SHA-256 hex value");
        }
        return normalized;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new CertificateException("Client certificates are not trusted");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Remote certificate chain is empty");
        }
        X509Certificate leaf = chain[0];
        leaf.checkValidity();
        try {
            byte[] actualFingerprint = MessageDigest.getInstance("SHA-256").digest(leaf.getEncoded());
            if (!MessageDigest.isEqual(expectedFingerprint, actualFingerprint)) {
                throw new CertificateException("Remote certificate fingerprint does not match");
            }
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new CertificateException("SHA-256 is unavailable", exception);
        } catch (java.security.cert.CertificateEncodingException exception) {
            throw new CertificateException("Remote certificate cannot be encoded", exception);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}