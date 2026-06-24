package com.example.todoalarm.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopSyncAddressSelectorTest {
    @Test
    fun prefersPhysicalWifi192AddressOverVirtual172Address() {
        val selection = DesktopSyncAddressSelector.select(
            listOf(
                DesktopSyncAddressCandidate(
                    address = "172.19.0.1",
                    interfaceName = null,
                    source = DesktopSyncAddressSource.ACTIVE_WIFI
                ),
                DesktopSyncAddressCandidate(
                    address = "172.19.0.1",
                    interfaceName = "br0",
                    source = DesktopSyncAddressSource.NETWORK_INTERFACE
                ),
                DesktopSyncAddressCandidate(
                    address = "192.168.0.100",
                    interfaceName = "wlan0",
                    source = DesktopSyncAddressSource.NETWORK_INTERFACE
                )
            )
        )

        assertTrue(selection.localNetworkAvailable)
        assertEquals(listOf("192.168.0.100"), selection.ipAddresses)
        assertTrue(selection.hint.contains("隐藏"))
    }

    @Test
    fun keepsTrusted172HotspotAddressWhenItIsTheOnlyLanCandidate() {
        val selection = DesktopSyncAddressSelector.select(
            listOf(
                DesktopSyncAddressCandidate(
                    address = "172.20.10.1",
                    interfaceName = "ap0",
                    source = DesktopSyncAddressSource.NETWORK_INTERFACE
                )
            )
        )

        assertTrue(selection.localNetworkAvailable)
        assertEquals(listOf("172.20.10.1"), selection.ipAddresses)
    }

    @Test
    fun hidesMobileDataAndVpnOnlyAddresses() {
        val selection = DesktopSyncAddressSelector.select(
            listOf(
                DesktopSyncAddressCandidate(
                    address = "10.68.13.22",
                    interfaceName = "rmnet_data0",
                    source = DesktopSyncAddressSource.NETWORK_INTERFACE
                ),
                DesktopSyncAddressCandidate(
                    address = "10.8.0.2",
                    interfaceName = "tun0",
                    source = DesktopSyncAddressSource.NETWORK_INTERFACE
                )
            )
        )

        assertFalse(selection.localNetworkAvailable)
        assertEquals(emptyList<String>(), selection.ipAddresses)
    }

    @Test
    fun allowsEthernetTenDotLanAddress() {
        val selection = DesktopSyncAddressSelector.select(
            listOf(
                DesktopSyncAddressCandidate(
                    address = "10.0.0.15",
                    interfaceName = "eth0",
                    source = DesktopSyncAddressSource.NETWORK_INTERFACE
                )
            )
        )

        assertTrue(selection.localNetworkAvailable)
        assertEquals(listOf("10.0.0.15"), selection.ipAddresses)
    }
}
