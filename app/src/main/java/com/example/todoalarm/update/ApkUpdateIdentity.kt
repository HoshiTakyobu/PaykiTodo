package com.example.todoalarm.update

import android.content.pm.PackageInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.security.MessageDigest

object ApkUpdateIdentity {
    @Suppress("DEPRECATION")
    fun validate(installed: PackageInfo, archive: PackageInfo, expectedPackage: String, expectedVersion: String) {
        require(archive.packageName == expectedPackage) { "包名不匹配" }
        require(archive.versionName == expectedVersion && PackageInfoCompat.getLongVersionCode(archive) > PackageInfoCompat.getLongVersionCode(installed)) { "安装包版本不正确" }
        fun signers(info: PackageInfo, history: Boolean = false): Set<String> = (if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.let { if (history && !it.hasMultipleSigners()) it.signingCertificateHistory else it.apkContentsSigners }
        } else info.signatures)?.map { signature -> MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) } }?.toSet() ?: emptySet()
        require(GithubRelease.compatibleSigners(signers(installed), signers(archive), signers(archive, true))) { "签名不兼容，不能覆盖当前应用" }
    }
}
