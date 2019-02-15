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
package com.google.security.keymaster.lite;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A portable implementation of keymaster's hybrid encryption. Corresponds to the key type
 * HYBRID_P256_AES_256_GCM_PUBLIC, which is a modernized version of Shoup's ISO propsal. The class
 * is thread-safe.
 */
public class KeymaestroHybridEncrypter {
  private static final byte VERSION = 0;
  private static final int KEY_ID_LENGTH = 4;
  private static final int FIELD_LENGTH = 32;
  private static final int POINT_LENGTH = FIELD_LENGTH * 2;
  private static final String AES = "AES";
  private static final String AES_GCM = AES + "/GCM/NoPadding";
  private static final int DEM_KEY_LENGTH = 16;
  private static final int NONCE_LENGTH = 12;
  private static final int TAG_LENGTH = 16;

  static final int CONSTRUCTOR_KEY_LENGTH = KEY_ID_LENGTH + POINT_LENGTH;

  static final ECFieldFp FIELD;
  static final EllipticCurve CURVE;
  static final ECParameterSpec CURVE_SPEC;

  static {
    // Curve P-256
    // http://csrc.nist.gov/publications/fips/archive/fips186-2/fips186-2.pdf
    BigInteger p =
        new BigInteger(
            "115792089210356248762697446949407573530086143415290314195533631308867097853951");
    BigInteger n =
        new BigInteger(
            "115792089210356248762697446949407573529996955224135760342422259061068512044369");
    BigInteger a = p.subtract(new BigInteger("3"));
    BigInteger b =
        new BigInteger("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16);
    BigInteger gx =
        new BigInteger("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296", 16);
    BigInteger gy =
        new BigInteger("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5", 16);
    int h = 1;
    ECPoint g = new ECPoint(gx, gy);

    FIELD = new ECFieldFp(p);
    CURVE = new EllipticCurve(FIELD, a, b);
    CURVE_SPEC = new ECParameterSpec(CURVE, g, n, h);
  }

  private static final SecureRandom random = new SecureRandom();

  private final byte[] keyId = new byte[KEY_ID_LENGTH];
  private final byte[] kemToken;
  private final byte[] aesKey;

  /**
   * Creates a KeymasterHybridEncrypter from a serialized public key.
   *
   * @param publicKey is a 4-byte identity followed by a 64-byte P256 point.
   */
  public KeymaestroHybridEncrypter(byte[] publicKey) throws GeneralSecurityException {
    if (publicKey == null) {
      throw new IllegalArgumentException("publicKey is null");
    }
    if (publicKey.length != CONSTRUCTOR_KEY_LENGTH) {
      throw new IllegalArgumentException(
          "publicKey should be " + CONSTRUCTOR_KEY_LENGTH + " bytes");
    }
    System.arraycopy(publicKey, 0, keyId, 0, KEY_ID_LENGTH);
    ECPoint peerPublicKey = deserializePoint(publicKey, KEY_ID_LENGTH, POINT_LENGTH);

    // Generate a keypair
    KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
    gen.initialize(CURVE_SPEC);
    KeyPair keypair = gen.genKeyPair();
    ECPrivateKey jcePrivateKey = (ECPrivateKey) keypair.getPrivate();
    ECPoint jcePublicKey = ((ECPublicKey) keypair.getPublic()).getW();

    kemToken = serializePoint(jcePublicKey);

    byte[] ecdhResult = ecdh(jcePrivateKey, peerPublicKey);
    aesKey =
        hkdf(
            cat(kemToken, ecdhResult),
            null /* salt */,
            "GOOGLE_KEYMASTER".getBytes(UTF_8),
            DEM_KEY_LENGTH);
  }

  /**
   * Performs hybrid encryption on the plaintext.
   *
   * @param plaintext The plaintext to be encrypted.
   * @return The encrypted ciphertext formatted as a zero byte, followed by the 4-byte key identity,
   *     followed by the per-message 64-byte P256 point, followed by a random 12-byte AES-GCM nonce,
   *     followed by the AES-256-GCM ciphertext, followed by a 16-byte AES-GCM tag.
   * @throws GeneralSecurityException if jce encryption does something unexpected.
   */
  public byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
    byte[] nonce = new byte[NONCE_LENGTH];
    random.nextBytes(nonce);
    ByteArrayOutputStream ciphertext =
        new ByteArrayOutputStream(
            1 + KEY_ID_LENGTH + POINT_LENGTH + NONCE_LENGTH + plaintext.length + TAG_LENGTH);
    ciphertext.write(VERSION);
    ciphertext.write(keyId, 0, keyId.length);
    ciphertext.write(kemToken, 0, kemToken.length);
    ciphertext.write(nonce, 0, nonce.length);
    byte[] demResult = demEncrypt(aesKey, nonce, plaintext);
    ciphertext.write(demResult, 0, demResult.length);
    return ciphertext.toByteArray();
  }

  /** Returns the concatenation of the two inputs. */
  private static byte[] cat(byte[] left, byte[] right) {
    byte[] result = new byte[left.length + right.length];
    System.arraycopy(left, 0, result, 0, left.length);
    System.arraycopy(right, 0, result, left.length, right.length);
    return result;
  }

  /**
   * Performs encryption using AES-256-GCM.
   *
   * @param aesKey The 256-bit AES-GCM key.
   * @param nonce The 12-byte nonce.
   * @param plaintext The plaintext to be encrypted.
   * @return The encrypted ciphertext, containing the 16-byte AES-GCM tag.
   * @throws GeneralSecurityException if jce encryption does something unexpected.
   */
  private static byte[] demEncrypt(byte[] aesKey, byte[] nonce, byte[] plaintext)
      throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(AES_GCM);
    cipher.init(
        Cipher.ENCRYPT_MODE,
        new SecretKeySpec(aesKey, AES),
        new GCMParameterSpec(TAG_LENGTH * 8, nonce));

    byte[] ciphertext = new byte[plaintext.length + TAG_LENGTH];
    int resultLength = cipher.doFinal(plaintext, 0, plaintext.length, ciphertext);
    if (resultLength != ciphertext.length) {
      throw new GeneralSecurityException("Length mismatch");
    }
    return ciphertext;
  }

  /**
   * Performs an elliptic-curve Diffie-Hellmen exchange, using P256. Package-visibility for testing.
   */
  static byte[] ecdh(ECPrivateKey privateKey, ECPoint publicPoint) throws GeneralSecurityException {
    ECPublicKeySpec publicSpec = new ECPublicKeySpec(publicPoint, CURVE_SPEC);
    KeyFactory kf = KeyFactory.getInstance("EC");
    PublicKey publicKey = kf.generatePublic(publicSpec);
    KeyAgreement ka = KeyAgreement.getInstance("ECDH");
    ka.init(privateKey);
    ka.doPhase(publicKey, true);
    return ka.generateSecret();
  }

  /**
   * Performs HKDF using HMAC-SHA256. See https://tools.ietf.org/html/rfc5869. Package-visibility
   * for testing.
   *
   * @param ikm The key material.
   * @param salt An optional (can be null) non-secret random value.
   * @param info An optional (cannot be null, can be zero-length) context or application-specific
   *     information.
   * @param extractBytes The number of bytes to generate.
   * @return extractBytes many pseudo-random bytes.
   * @throws GeneralSecurityException if jce MAC does something unexpected.
   */
  static byte[] hkdf(byte[] ikm, byte[] salt, byte[] info, int extractBytes)
      throws GeneralSecurityException {
    Mac mac = Mac.getInstance("HMACSHA256");
    if (salt == null) {
      salt = new byte[mac.getMacLength()];
    }

    // Extract
    mac.init(new SecretKeySpec(salt, mac.getAlgorithm()));
    byte[] prk = mac.doFinal(ikm);

    // Expand
    mac.init(new SecretKeySpec(prk, mac.getAlgorithm()));
    int digestLength = mac.getMacLength();
    int blocks = extractBytes / digestLength;
    if (blocks * digestLength != extractBytes) {
      // Need a partial block
      blocks++;
    }
    if (blocks > 255) {
      throw new IllegalArgumentException("extracting too many bytes at once");
    }
    byte[] result = new byte[extractBytes];
    byte[] digest = new byte[0];
    for (int i = 0; i < blocks; i++) {
      mac.update(digest);
      mac.update(info);
      mac.update((byte) (i + 1));
      digest = mac.doFinal();
      if (i < blocks - 1) {
        System.arraycopy(digest, 0, result, i * digestLength, digestLength);
      } else {
        // Copy a possibly partial block
        System.arraycopy(
            digest, 0, result, i * digestLength, extractBytes - (blocks - 1) * digestLength);
      }
    }
    return result;
  }

  /**
   * Copies a BigInteger to a fixed-sized array as an unsigned big-endian integer, padding with
   * leading zeros. The output array is assumed to contain |length| zero bytes starting at
   * output[offset]. This is used as an alternative to BigInteger.toByteArray(), which will not
   * return a fixed-sized array. In particular, BigInteger.toByteArray() would add an extra sign
   * byte and drop leading zeros. Package-visibility for testing.
   *
   * @param bigInt The integer to fit.
   * @param output The output buffer.
   * @param offset The offset in output where the output starts.
   * @param length The number of bytes we are allowed to write to output.
   */
  static void fitBigInteger(BigInteger bigInt, byte[] output, int offset, int length) {
    byte[] array = bigInt.toByteArray();
    if (array.length > length + 1) {
      throw new IllegalArgumentException("Array is too small to hold this BigInteger");
    } else if (array.length > length) {
      // Clip off extra sign byte
      System.arraycopy(array, array.length - length, output, offset, length);
    } else {
      // Preserve leading zeros
      System.arraycopy(array, 0, output, offset + length - array.length, array.length);
    }
  }

  /**
   * Converts a 32-byte big-endian serialization of p256 exponent into an ECPrivateKey object.
   * Package-visibility for testing.
   */
  static ECPrivateKey deserializePrivateKey(byte[] privateKey) throws GeneralSecurityException {
    KeyFactory kf = KeyFactory.getInstance("EC");
    BigInteger exponent = new BigInteger(1 /* positive */, privateKey);
    ECPrivateKeySpec spec = new ECPrivateKeySpec(exponent, CURVE_SPEC);
    return (ECPrivateKey) kf.generatePrivate(spec);
  }

  /**
   * Returns the juxtaposition of the two serialized ECPoint coordinates, where each point is
   * serialized as a 32-byte big-endian integer.
   */
  private static byte[] serializePoint(ECPoint point) throws GeneralSecurityException {
    byte[] result = new byte[POINT_LENGTH];
    fitBigInteger(point.getAffineX(), result, 0, FIELD_LENGTH);
    fitBigInteger(point.getAffineY(), result, FIELD_LENGTH, FIELD_LENGTH);
    return result;
  }

  /**
   * Converts the juxtaposition of the two serialized ECPoint coordinates into an ECPoint object,
   * where each point is serialized as a 32-byte big-endian integer. Package-visibility for testing.
   */
  static ECPoint deserializePoint(byte[] serializedPoint) throws GeneralSecurityException {
    return deserializePoint(serializedPoint, 0, serializedPoint.length);
  }

  /**
   * Converts the juxtaposition of the two serialized ECPoint coordinates into an ECPoint object,
   * where each point is serialized as a 32-byte big-endian integer.
   */
  private static ECPoint deserializePoint(byte[] serializedPoint, int offset, int length)
      throws GeneralSecurityException {
    if (length != POINT_LENGTH) {
      throw new IllegalArgumentException("serialized point length is too short");
    }
    BigInteger x =
        new BigInteger(
            1 /* positive */, Arrays.copyOfRange(serializedPoint, offset, offset + FIELD_LENGTH));
    BigInteger y =
        new BigInteger(
            1 /* positive */,
            Arrays.copyOfRange(serializedPoint, offset + FIELD_LENGTH, offset + 2 * FIELD_LENGTH));
    ECPoint point = new ECPoint(x, y);
    if (!isPointOnCurve(point)) {
      throw new GeneralSecurityException("point is not on the curve");
    }
    return point;
  }

  /** Returns true if the given point is on the curve. */
  private static boolean isPointOnCurve(ECPoint point) throws GeneralSecurityException {
    BigInteger p = FIELD.getP();
    BigInteger x = point.getAffineX();
    BigInteger y = point.getAffineY();
    if (x == null || y == null) {
      throw new GeneralSecurityException("point is at infinity");
    }
    // Check 0 <= x < p and 0 <= y < p
    if (x.signum() == -1 || x.compareTo(p) != -1) {
      throw new GeneralSecurityException("x is out of range");
    }
    if (y.signum() == -1 || y.compareTo(p) != -1) {
      throw new GeneralSecurityException("y is out of range");
    }
    // Check y^2 == x^3 + a x + b (mod p)
    BigInteger lhs = y.multiply(y).mod(p);
    BigInteger rhs = x.multiply(x).add(CURVE.getA()).multiply(x).add(CURVE.getB()).mod(p);
    return lhs.equals(rhs);
  }
}
