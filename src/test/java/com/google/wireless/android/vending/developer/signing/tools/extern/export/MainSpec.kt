package com.google.wireless.android.vending.developer.signing.tools.extern.export

import com.google.security.keymaster.lite.KeymaestroHybridEncrypter
import org.junit.jupiter.api.Assertions.assertNotNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey

object MainSpec: Spek({
    describe("Keystore") {
        describe("pepk") {
            it("returns encryptedPrivateKey") {
                val keyStore = KeyStore.getInstance("jks").apply {
                    File("signing.jks").inputStream().use { input -> load(input, "storepass".toCharArray()) }
                }
                val key = keyStore.getKey("signing", "keypass".toCharArray()) as PrivateKey

                val encryptedPrivateKey = KeymaestroHybridEncrypter(ExportEncryptedPrivateKeyTool.fromHexTesting("eb10fe8f7c7c9df715022017b00c6471f8ba8170b13049a11e6c09ffe3056a104a3bbe4ac5a955f4ba4fe93fc8cef27558a3eb9d2a529a2092761fb833b656cd48b9de6a"))
                        .encrypt(ExportEncryptedPrivateKeyTool.privateKeyToPem(key))

                assertNotNull(encryptedPrivateKey)
                println(encryptedPrivateKey)
                val certificate = keyStore.getCertificate("signing")
                assertNotNull(certificate)
                println(ExportEncryptedPrivateKeyTool.certificateToPemTesting(certificate))
            }
        }
    }
})

