package com.example.todoalarm.sync

import java.util.Locale

internal enum class DesktopSyncAddressSource {
    ACTIVE_WIFI,
    NETWORK_INTERFACE
}

internal data class DesktopSyncAddressCandidate(
    val address: String,
    val interfaceName: String?,
    val source: DesktopSyncAddressSource
)

internal data class DesktopSyncAddressSelection(
    val ipAddresses: List<String>,
    val localNetworkAvailable: Boolean,
    val hint: String
)

internal object DesktopSyncAddressSelector {
    fun select(candidates: List<DesktopSyncAddressCandidate>): DesktopSyncAddressSelection {
        val valid = candidates
            .mapNotNull { candidate ->
                val address = candidate.address.trim()
                if (!isShareableIpv4(candidate.copy(address = address))) return@mapNotNull null
                candidate.copy(address = address, interfaceName = candidate.interfaceName?.trim()?.takeIf { it.isNotBlank() })
            }
            .distinctBy { "${it.source}:${it.interfaceName.orEmpty()}:${it.address}" }

        if (valid.isEmpty()) {
            return DesktopSyncAddressSelection(
                ipAddresses = emptyList(),
                localNetworkAvailable = false,
                hint = "当前未检测到 Wi-Fi 或热点局域网地址；仅移动数据、VPN 或虚拟网卡地址不会显示，因为电脑通常无法访问。"
            )
        }

        val shareable = valid.filter { candidate ->
            val name = candidate.interfaceName.normalizedInterfaceName()
            when {
                isTrustedLanInterface(name) -> true
                isBridgeInterface(name) -> true
                candidate.source == DesktopSyncAddressSource.ACTIVE_WIFI && !isDefinitelyVirtualInterface(name) -> true
                else -> false
            }
        }
        val pool = shareable.ifEmpty {
            valid.filter { candidate ->
                !isDefinitelyVirtualInterface(candidate.interfaceName.normalizedInterfaceName())
            }
        }

        if (pool.isEmpty()) {
            return DesktopSyncAddressSelection(
                ipAddresses = emptyList(),
                localNetworkAvailable = false,
                hint = "电脑同步服务已启动，但当前只检测到移动数据、VPN 或虚拟网卡地址；请切换到 Wi-Fi 或手机热点后再显示连接地址。"
            )
        }

        val bestByAddress = pool
            .groupBy { it.address }
            .mapValues { (_, addressCandidates) -> addressCandidates.minBy(::candidateScore) }
            .values
            .sortedWith(compareBy<DesktopSyncAddressCandidate> { candidateScore(it) }.thenBy { it.address })

        val addresses = bestByAddress
            .filterNot { shouldHideWhenBetterAddressExists(it, bestByAddress) }
            .map { it.address }
            .distinct()

        if (addresses.isEmpty()) {
            return DesktopSyncAddressSelection(
                ipAddresses = emptyList(),
                localNetworkAvailable = false,
                hint = "电脑同步服务已启动，但没有找到可让电脑浏览器访问的 Wi-Fi / 热点局域网地址。"
            )
        }

        val hiddenLowConfidenceCount = valid
            .map { it.address }
            .distinct()
            .count { it !in addresses }
        val hasTrustedWifi = bestByAddress.any { isTrustedLanInterface(it.interfaceName.normalizedInterfaceName()) }
        val hasBridgeOnly = !hasTrustedWifi && bestByAddress.any { isBridgeInterface(it.interfaceName.normalizedInterfaceName()) }
        val hint = buildString {
            append(
                when {
                    hasTrustedWifi -> "已检测到当前 Wi-Fi / 热点局域网地址，优先显示最可能被电脑访问的地址。"
                    hasBridgeOnly -> "未检测到普通 Wi-Fi 地址，但发现疑似热点/以太网桥接地址；电脑连接手机热点时可尝试。"
                    else -> "已检测到可能可用的局域网地址；若电脑连不上，请确认手机和电脑处于同一 Wi-Fi / 热点。校园网还可能开启设备隔离，此时地址会显示，但电脑仍可能无法直连。"
                }
            )
            if (hiddenLowConfidenceCount > 0) {
                append(" 已隐藏 VPN、虚拟网卡、移动数据或低可信桥接地址，避免显示电脑无法 ping 通的链接。")
            }
        }

        return DesktopSyncAddressSelection(
            ipAddresses = addresses,
            localNetworkAvailable = true,
            hint = hint
        )
    }

    private fun candidateScore(candidate: DesktopSyncAddressCandidate): Int {
        val name = candidate.interfaceName.normalizedInterfaceName()
        val interfacePriority = when {
            isTrustedLanInterface(name) -> 0
            isBridgeInterface(name) -> 45
            candidate.source == DesktopSyncAddressSource.ACTIVE_WIFI && !isDefinitelyVirtualInterface(name) -> 55
            isDefinitelyVirtualInterface(name) -> 200
            else -> 90
        }
        val addressPriority = when {
            candidate.address.startsWith("192.168.") -> 0
            candidate.address.startsWith("10.") -> 20
            candidate.address.matches(Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\..*")) -> 40
            else -> 100
        }
        val activeBonus = if (
            candidate.source == DesktopSyncAddressSource.ACTIVE_WIFI &&
            (isTrustedLanInterface(name) || candidate.address.startsWith("192.168."))
        ) {
            -5
        } else {
            0
        }
        return interfacePriority + addressPriority + activeBonus
    }

    private fun shouldHideWhenBetterAddressExists(
        candidate: DesktopSyncAddressCandidate,
        sortedCandidates: List<DesktopSyncAddressCandidate>
    ): Boolean {
        if (sortedCandidates.size <= 1) return false
        val best = sortedCandidates.first()
        val name = candidate.interfaceName.normalizedInterfaceName()
        return candidate.address != best.address &&
            candidate.address.matches(Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\..*")) &&
            !isTrustedLanInterface(name) &&
            best.address.startsWith("192.168.")
    }

    private fun isShareableIpv4(candidate: DesktopSyncAddressCandidate): Boolean {
        val address = candidate.address
        val parts = address.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        if (parts[0] == 0 || parts[0] == 127 || parts[0] >= 224) return false
        if (parts[0] == 169 && parts[1] == 254) return false

        val name = candidate.interfaceName.normalizedInterfaceName()
        return isPrivateIpv4(parts) ||
            candidate.source == DesktopSyncAddressSource.ACTIVE_WIFI ||
            isTrustedLanInterface(name) ||
            isBridgeInterface(name)
    }

    private fun isPrivateIpv4(parts: List<Int>): Boolean {
        return parts[0] == 10 ||
            (parts[0] == 192 && parts[1] == 168) ||
            (parts[0] == 172 && parts[1] in 16..31)
    }

    private fun isTrustedLanInterface(name: String): Boolean {
        if (name.isBlank()) return false
        return name.startsWith("wlan") ||
            name.startsWith("swlan") ||
            name.startsWith("ap") ||
            name.startsWith("eth") ||
            name.startsWith("usb") ||
            name.startsWith("rndis")
    }

    private fun isBridgeInterface(name: String): Boolean {
        return name == "br" || name.startsWith("br")
    }

    private fun isDefinitelyVirtualInterface(name: String): Boolean {
        if (name.isBlank()) return false
        return name.startsWith("lo") ||
            name.startsWith("dummy") ||
            name.startsWith("tun") ||
            name.startsWith("tap") ||
            name.startsWith("ppp") ||
            name.startsWith("rmnet") ||
            name.startsWith("ccmni") ||
            name.startsWith("clat") ||
            name.startsWith("ipsec") ||
            name.startsWith("sit") ||
            name.startsWith("gre") ||
            name.startsWith("veth") ||
            name.startsWith("docker") ||
            name.startsWith("virbr") ||
            name.startsWith("wg") ||
            name.startsWith("tailscale")
    }

    private fun String?.normalizedInterfaceName(): String {
        return this?.lowercase(Locale.ROOT).orEmpty()
    }
}
