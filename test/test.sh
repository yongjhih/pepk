#!/usr/bin/env bash
# sample encryptionkey from https://www.b4x.com/android/forum/threads/enroll-your-app-in-google-play-app-signing-solved.81705/
pepk --keystore=signing-release.jks --alias=signing-release --output=signing-release.zip --encryptionkey="eb10fe8f7c7c9df715022017b00c6471f8ba8170b13049a11e6c09ffe3056a104a3bbe4ac5a955f4ba4fe93fc8cef27558a3eb9d2a529a2092761fb833b656cd48b9de6a" --include-cert
# Enter password for store 'signing-release.jks': storepass
# Enter password for key 'signing-release': keypass
