# PEPK - Export Encrypted Private Key Tool

## Usage

Use the command below to run the tool, which will export and encrypt your private key and its public certificate. Ensure that you replace the arguments highlighted in bold. Then enter your store and key passwords when prompted.

```
java -jar pepk.jar --keystore=foo.keystore --alias=foo --output=output.zip --encryptionkey=xxx --include-cert
```

## Build

```
./gradlew assemble
```

## License

Android 2.0 - Google Inc, 2017
