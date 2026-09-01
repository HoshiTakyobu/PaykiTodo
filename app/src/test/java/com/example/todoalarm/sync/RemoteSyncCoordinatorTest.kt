package com.example.todoalarm.sync

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteSyncCoordinatorTest {
    @Test
    fun accountSecretIsCanonicalAndStableAcrossClients() {
        val first = RemoteSyncCoordinator.deriveAccountSecret(" Alice.User ", "correct horse battery staple")
        val second = RemoteSyncCoordinator.deriveAccountSecret("alice.user", "correct horse battery staple")
        val different = RemoteSyncCoordinator.deriveAccountSecret("alice.user", "another password")

        assertEquals(first, second)
        assertEquals(43, first.length)
        assertNotEquals(first, different)
    }

    @Test
    fun encryptedSnapshotRoundTripsAndRejectsWrongSecret() {
        val payload = "PaykiTodo encrypted account snapshot".toByteArray()
        val encrypted = RemoteSyncCrypto.encrypt(payload, "shared-account-secret")

        assertArrayEquals(payload, RemoteSyncCrypto.decrypt(encrypted, "shared-account-secret"))
        assertThrows(IllegalArgumentException::class.java) {
            RemoteSyncCrypto.decrypt(encrypted, "wrong-secret")
        }
    }
}
