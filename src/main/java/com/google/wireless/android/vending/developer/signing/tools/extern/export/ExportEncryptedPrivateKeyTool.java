/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.wireless.android.vending.developer.signing.tools.extern.export;

import static com.google.wireless.android.vending.developer.signing.tools.extern.export.Utils.checkNotNull;
import static java.nio.charset.StandardCharsets.US_ASCII;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.security.keymaster.lite.KeymaestroHybridEncrypter;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tool for extracting a private key from a Java Keystore and then encrypting it (with hybrid public
 * key encryption) for secure transfer to Google.
 */
public class ExportEncryptedPrivateKeyTool {

  private static final String FLAG_KEYSTORE = "keystore";
  private static final String FLAG_ALIAS = "alias";
  private static final String FLAG_ENCRYPTION_KEY = "encryptionkey";
  private static final String FLAG_OUTPUT = "output";
  private static final String FLAG_SIGNING_KEYSTORE = "signing-keystore";
  private static final String FLAG_SIGNING_KEY_ALIAS = "signing-key-alias";
  private static final String FLAG_INCLUDE_CERT = "include-cert";
  private static final ImmutableList<String> SUPPORTED_SIGNING_ALGORITHMS =
      ImmutableList.of("RSA", "DSA");

  private static final String HELP_PAGE = "help.txt";

  private final KeystoreHelper keystoreHelper;

  public static void main(String[] args) {
    if (args.length == 0 || args[0].equals("--help")) {
      printUsage();
      return;
    }

    // Parse flags
    String keystoreFile = null;
    String alias = null;
    String encryptionPublicKeyHex = null;
    String outputFile = null;
    String signingKeyAlias = null;
    String signingKeystoreFile = null;
    String includeCert = null;
    try {
      Map<String, String> parsedFlags = Utils.processArgs(args);
      keystoreFile = getFlagValue(parsedFlags, FLAG_KEYSTORE);
      alias = getFlagValue(parsedFlags, FLAG_ALIAS);
      encryptionPublicKeyHex = getFlagValue(parsedFlags, FLAG_ENCRYPTION_KEY);
      outputFile = getFlagValue(parsedFlags, FLAG_OUTPUT);
      if (parsedFlags.containsKey(FLAG_SIGNING_KEY_ALIAS)) {
        signingKeyAlias = parsedFlags.remove(FLAG_SIGNING_KEY_ALIAS);
        // If signing key alias is provided then signing key keystore is required.
        signingKeystoreFile = getFlagValue(parsedFlags, FLAG_SIGNING_KEYSTORE);
      } else if (parsedFlags.containsKey(FLAG_INCLUDE_CERT)) {
        includeCert = getFlagValue(parsedFlags, FLAG_INCLUDE_CERT);
      }
      if (!parsedFlags.isEmpty()) {
        throw new IllegalArgumentException("Unrecognized flags: " + parsedFlags);
      }
    } catch (Exception e) {
      System.err.println("Error: Unable to parse the input: " + Arrays.toString(args));
      e.printStackTrace();
      printUsage();
      System.exit(1);
    }

    // Run tool
    try {
      ExportEncryptedPrivateKeyTool tool = new ExportEncryptedPrivateKeyTool(new KeystoreHelper());
      KeystoreKey keyToExport = new KeystoreKey(Paths.get(keystoreFile), alias);
      Optional<KeystoreKey> keyToSignWith =
          signingKeystoreFile != null && signingKeyAlias != null
              ? Optional.of(new KeystoreKey(Paths.get(signingKeystoreFile), signingKeyAlias))
              : Optional.empty();
      boolean includeCertificate = Boolean.parseBoolean(includeCert);
      tool.run(encryptionPublicKeyHex, outputFile, keyToExport, keyToSignWith, includeCertificate);
    } catch (Exception e) {
      System.err.println("Error: Unable to export or encrypt the private key");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static String getFlagValue(Map<String, String> parsedFlags, String flagName) {
    return checkNotNull(parsedFlags.remove(flagName), "--" + flagName + " must be specified");
  }

  ExportEncryptedPrivateKeyTool(KeystoreHelper keystoreHelper) {
    this.keystoreHelper = keystoreHelper;
  }

  public ExportEncryptedPrivateKeyTool() {
    this.keystoreHelper = new KeystoreHelper();
  }

  /**
   * Extracts a private key from a Java Keystore and then encrypts it (with hybrid public key
   * encryption). If signing key is specified the encrypted private key is additionally signed with
   * that key and the output is a zip file containing the signature and the encrypted private key.
   *
   * <p>If the signing key uses algorithm different then RSA or DSA an UnsupportedAlgorithmException
   * is thrown.
   */
  public void run(
      String encryptionPublicKeyHex,
      String outputFile,
      KeystoreKey keyToExport,
      Optional<KeystoreKey> keyToSignWith,
      boolean includeCertificate)
      throws Exception {
    KeyStore keyStoreForKeyToExport = keystoreHelper.getKeystore(keyToExport);
    PrivateKey privateKeyToExport =
        keystoreHelper.getPrivateKey(keyStoreForKeyToExport, keyToExport);
    byte[] privateKeyPem = privateKeyToPem(privateKeyToExport);
    byte[] encryptedPrivateKey = encryptPrivateKey(fromHex(encryptionPublicKeyHex), privateKeyPem);
    if (keyToSignWith.isPresent() || includeCertificate) {
      Certificate certificate = keystoreHelper.getCertificate(keyStoreForKeyToExport, keyToExport);
      Optional<byte[]> signature =
          keyToSignWith.isPresent()
              ? Optional.of(sign(encryptedPrivateKey, keyToSignWith.get()))
              : Optional.empty();
      writeToZipFile(outputFile, signature, encryptedPrivateKey, certificateToPem(certificate));
    } else {
      Files.write(Paths.get(outputFile), encryptedPrivateKey);
    }
  }

  public byte[] sign(byte[] payload, KeystoreKey signingKey) throws Exception {
    KeyStore keyStoreOfSigningKey = keystoreHelper.getKeystore(signingKey);
    PrivateKey pk = keystoreHelper.getPrivateKey(keyStoreOfSigningKey, signingKey);
    if (!SUPPORTED_SIGNING_ALGORITHMS.contains(pk.getAlgorithm())) {
      throw new UnsupportedAlgorithmException(
          String.format(
              "The signing key uses an unsupported algorithm. The tool only supports %s .",
              SUPPORTED_SIGNING_ALGORITHMS));
    }
    Signature sig = Signature.getInstance("SHA512with" + pk.getAlgorithm());
    sig.initSign(pk);
    sig.update(payload);
    return sig.sign();
  }

  // Visible for testing
  static byte[] privateKeyToPem(PrivateKey privateKey) {
    String pemString =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getEncoder().encodeToString(privateKey.getEncoded())
            + "\n-----END PRIVATE KEY-----\n";
    return pemString.getBytes(US_ASCII);
  }

  @VisibleForTesting
  public static byte[] certificateToPemTesting(Certificate certificate)
      throws CertificateEncodingException {
      return certificateToPem(certificate);
  }

  private static byte[] certificateToPem(Certificate certificate)
      throws CertificateEncodingException {
    return ("-----BEGIN CERTIFICATE-----\n"
            + Base64.getEncoder().encodeToString(certificate.getEncoded())
            + "\n-----END CERTIFICATE-----\n")
        .getBytes(US_ASCII);
  }

  private byte[] encryptPrivateKey(byte[] encryptionPublicKey, byte[] plaintext)
      throws GeneralSecurityException {
    return new KeymaestroHybridEncrypter(encryptionPublicKey).encrypt(plaintext);
  }

  private void writeToZipFile(
      String outputFile,
      Optional<byte[]> signature,
      byte[] encryptedPrivateKey,
      byte[] pemEncodedCertificate)
      throws Exception {
    Path tempFile = Files.createFile(Paths.get(outputFile));
    try (ZipOutputStream zipOutputStream =
        new ZipOutputStream(new FileOutputStream(tempFile.toString()))) {
      if (signature.isPresent()) {
        zipOutputStream.putNextEntry(new ZipEntry("encryptedPrivateKeySignature"));
        zipOutputStream.write(signature.get());
      }
      zipOutputStream.closeEntry();
      zipOutputStream.putNextEntry(new ZipEntry("encryptedPrivateKey"));
      zipOutputStream.write(encryptedPrivateKey);
      zipOutputStream.closeEntry();
      zipOutputStream.putNextEntry(new ZipEntry("certificate.pem"));
      zipOutputStream.write(pemEncodedCertificate);
      zipOutputStream.closeEntry();
    }
  }

  @VisibleForTesting
  public static byte[] fromHexTesting(String s) {
    return fromHex(s);
  }

  private static byte[] fromHex(String s) {
    int len = s.length();
    if (len % 2 != 0) {
      throw new IllegalArgumentException(
          "Hex encoded byte array must have even length but instead has length: "
              + len
              + ". Hex encoded string: "
              + s);
    }
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  private static void printUsage() {
    try (BufferedReader in =
        new BufferedReader(
            new InputStreamReader(
                ExportEncryptedPrivateKeyTool.class.getResourceAsStream(HELP_PAGE),
                StandardCharsets.UTF_8))) {
      String line;
      while ((line = in.readLine()) != null) {
        System.out.println(line);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read " + HELP_PAGE + " resource");
    }
  }
}
