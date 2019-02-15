package com.google.wireless.android.vending.developer.signing.tools.extern.export;

/** Exception thrown when an unsupported algorithm is provided. */
public class UnsupportedAlgorithmException extends RuntimeException {

  public UnsupportedAlgorithmException(String message) {
    super(message);
  }
}
