package com.example.todoalarm.sync

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteSyncCoordinatorTest {
    @Test
    fun emptyDeviceStartsAtZeroAndDownloadsExistingRemoteSnapshot() {
        assertEquals(
            InitialSyncState(0L, false, 0L),
            RemoteSyncCoordinator.initialSyncState(hasLocalData = false, remoteRevision = 7L)
        )
    }

    @Test
    fun localDataWithExistingRemoteSnapshotStartsAsConflict() {
        assertEquals(
            InitialSyncState(0L, true, 7L),
            RemoteSyncCoordinator.initialSyncState(hasLocalData = true, remoteRevision = 7L)
        )
    }

    @Test
    fun localDataWithEmptyRemoteSnapshotStartsDirtyForUpload() {
        assertEquals(
            InitialSyncState(0L, true, 0L),
            RemoteSyncCoordinator.initialSyncState(hasLocalData = true, remoteRevision = 0L)
        )
    }

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

    @Test
    fun passwordRotationPreservesSnapshotBytesForAnotherClient() {
        val oldSecret = RemoteSyncCoordinator.deriveAccountSecret("account", "old-test-password")
        val newSecret = RemoteSyncCoordinator.deriveAccountSecret("account", "new-test-password")
        val original = ByteArray(4096) { (it % 251).toByte() }
        val oldEnvelope = RemoteSyncCrypto.encrypt(original, oldSecret)
        val newEnvelope = RemoteSyncCrypto.encrypt(RemoteSyncCrypto.decrypt(oldEnvelope, oldSecret), newSecret)
        val otherDeviceSecret = RemoteSyncCoordinator.deriveAccountSecret("ACCOUNT", "new-test-password")
        assertArrayEquals(original, RemoteSyncCrypto.decrypt(newEnvelope, otherDeviceSecret))
        assertThrows(IllegalArgumentException::class.java) { RemoteSyncCrypto.decrypt(newEnvelope, oldSecret) }
    }
}
