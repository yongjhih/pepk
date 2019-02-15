package com.google.wireless.android.vending.developer.signing.tools.extern.export;

import java.nio.file.Path;
import javax.annotation.Nullable;

public final class KeystoreKey {
  private final Path keystorePath;
  private final String keyAlias;
  @Nullable private char[] keystorePassword;
  @Nullable private char[] keyPassword;

  public KeystoreKey(Path keystorePath, String keyAlias) {
    this.keystorePath = keystorePath;
    this.keyAlias = keyAlias;
  }

  public KeystoreKey(
      Path keystorePath, String keyAlias, char[] keystorePassword, char[] keyPassword) {
    this.keystorePath = keystorePath;
    this.keyAlias = keyAlias;
    this.keystorePassword = keystorePassword;
    this.keyPassword = keyPassword;
  }

  public Path getKeystorePath() {
    return keystorePath;
  }

  public String getKeyAlias() {
    return keyAlias;
  }

  @Nullable
  public char[] getKeystorePassword() {
    return keystorePassword;
  }

  @Nullable
  public char[] getKeyPassword() {
    return keyPassword;
  }
}
