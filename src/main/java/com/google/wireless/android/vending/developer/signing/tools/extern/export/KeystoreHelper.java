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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import javax.annotation.Nullable;

/** Helper class for reading a key from a Java Keystore. */
class KeystoreHelper {

  PrivateKey getPrivateKey(KeyStore keystore, KeystoreKey key) throws GeneralSecurityException {
    return extractPrivateKey(keystore, key.getKeyAlias(), key.getKeyPassword());
  }

  KeyStore getKeystore(KeystoreKey key) throws GeneralSecurityException, IOException {
    return loadKeystore(key.getKeystorePath(), key.getKeystorePassword());
  }

  public KeyStore loadKeystore(Path keystorePath, @Nullable char[] storePass)
      throws GeneralSecurityException, IOException {
    if (storePass == null) {
      storePass =
          System.console()
              .readPassword(
                  String.format("Enter password for store '%s':", keystorePath.getFileName()));
    }

    KeyStore keyStore = KeyStore.getInstance("jks");
    try (InputStream keyStoreIn = new FileInputStream(keystorePath.toFile())) {
      keyStore.load(keyStoreIn, storePass);
    }
    return keyStore;
  }

  private PrivateKey extractPrivateKey(KeyStore ks, String alias, @Nullable char[] keyPass)
      throws GeneralSecurityException {
    if (keyPass == null) {
      keyPass = System.console().readPassword(String.format("Enter password for key '%s':", alias));
    }
    return checkNotNull((PrivateKey) ks.getKey(alias, keyPass), "No key for alias: " + alias);
  }

  public Certificate getCertificate(KeyStore ks, KeystoreKey key) throws KeyStoreException {
    return ks.getCertificate(key.getKeyAlias());
  }
}
