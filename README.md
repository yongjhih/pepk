[![](https://jitpack.io/v/yongjhih/pepk.svg)](https://jitpack.io/#yongjhih/pepk)
[![javadoc](https://img.shields.io/github/tag/yongjhih/pepk.svg?label=javadoc)](https://jitpack.io/com/github/yongjhih/pepk/-SNAPSHOT/javadoc/)

# PEPK - Play Encrypt Private Key

pepk (Play Encrypt Private Key) is a tool for exporting private keys from a
Java Keystore and encrypting them for transfer to Google Play as part of
enrolling in App Signing by Google Play.

Use this code (the code performs EC-P256+AES-GCM hybrid encryption) with the hex encoded public key (a 4-byte identity followed by a 64-byte P256 point) to create your own tool exporting in a zip file the encrypted private key and the public certificate in PEM format.

## About

This repository is totally imported from Google [pepk-src.jar](https://www.gstatic.com/play-apps-publisher-rapid/signing-tool/prod/pepk-src.jar),
just make it capable of modifying and building with gradle.

## Usage of command line

Use the command below to run the tool, which will export and encrypt your private key and its public certificate. Ensure that you replace the arguments. Then enter your store and key passwords when prompted.

Using [installed](#installation) pepk.jar

```sh
java -jar pepk.jar --keystore=foo.keystore --alias=foo --output=output.zip --encryptionkey=xxx --include-cert
```

or using docker:

```sh
docker run --rm -it -v $(pwd):$(pwd) -w $(pwd) yongjhih/pepk --keystore=foo.keystore --alias=foo --output=output.zip --encryptionkey=xxx --include-cert
```

or using [docker-pepk](docker-pepk) into `~/bin/pepk`:

```sh
curl -L https://raw.githubusercontent.com/yongjhih/pepk/master/docker-pepk -o ~/bin/pepk && chmod a+x ~/bin/pepk
pepk --keystore=foo.keystore --alias=foo --output=output.zip --encryptionkey=xxx --include-cert
```

You will see the output.zip contains

```
├── certificate.pem
└── encryptedPrivateKey
```

## Usage as library


## Development

```sh
./gradlew run --args='--keystore=foo.keystore --alias=foo --output=output.zip --encryptionkey=xxx --include-cert'
```

or

```sh
./gradlew shadowJar
java -jar build/libs/pepk.jar --keystore=foo.keystore --alias=foo --output=output.zip --encryptionkey=xxx --include-cert
```

## Deployment

```sh
./gradlew shadowJar
```

Upload build/libs/pepk.jar into [releases](https://github.com/yongjhih/pepk/releases)

## Installation of command line tool

If you dont use docker, you have to download pepk.jar from [releases](https://github.com/yongjhih/pepk/releases)

```sh
java -jar pepk.jar ARGS
```

## Installation of as library

```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.yongjhih:pepk:-SNAPSHOT'
}
```

## Changelogs

Latest known pepk.jar/build-data.properties:

```
build.target=blaze-out/k8-opt/bin/third_party/java/pepk/ExportEncryptedPrivateKeyTool_deploy.jar
main.class=com.google.wireless.android.vending.developer.signing.tools.extern.export.ExportEncryptedPrivateKeyTool
build.gplatform=gcc-4.X.Y-crosstool-v18-llvm-grtev4-k8
build.depot.path=//depot/branches/play-apps-publisher-app-signing-encryption-tool_release_branch/229150960.1/google3
build.client_mint_status=1
build.client=build-secure-info\:source-uri
build.verifiable=1
build.location=play-apps-publisher-releaser@iojf11.prod.google.com\:/google/src/files/229167424/depot/branches/play-apps-publisher-app-signing-encryption-tool_release_branch/229150960.1/OVERLAY_READONLY/google3
build.tool=Blaze, release blaze-2018.12.11-3 (mainline @224801075)
build.label=play-apps-publisher-app-signing-encryption-tool_20190114_RC00
build.timestamp.as.int=1547473306
build.versionmap=map 0 { // }
build.changelist.as.int=229167424
build.baseline.changelist.as.int=229150960
build.timestamp=1547473306
build.build_id=1221793a-c127-4962-815a-1f6b61ad2cca
build.changelist=229167424
build.time=Mon Jan 14 05\:41\:46 2019 (1547473306)
build.citc.snapshot=-1
```

## References

* https://developer.android.com/studio/publish/app-signing
* Original [pepk.jar](https://www.gstatic.com/play-apps-publisher-rapid/signing-tool/prod/pepk.jar)
* Original [pepk-src.jar](https://www.gstatic.com/play-apps-publisher-rapid/signing-tool/prod/pepk-src.jar)

## License

Apache 2.0 - Google Inc, 2017
