package com.example.todoalarm.update

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.BuildConfig
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ApkUpdateIdentityTest {
    @Test fun realSignedArchiveAndInstalledPackageEnforceIdentityAndVersion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val apk = File(context.filesDir, "update-fixture-release.apk")
        assertTrue("Stage the current signed release APK in the isolated test user", apk.isFile)
        val manager = context.packageManager
        val signed = manager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)!!
        val installed = manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val olderSigned = manager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)!!.apply { longVersionCode = signed.longVersionCode - 1 }
        ApkUpdateIdentity.validate(olderSigned, signed, context.packageName, BuildConfig.VERSION_NAME)
        assertTrue(runCatching { ApkUpdateIdentity.validate(signed, signed, context.packageName, BuildConfig.VERSION_NAME) }.isFailure)
        assertTrue(runCatching { ApkUpdateIdentity.validate(olderSigned, signed, "com.other.app", BuildConfig.VERSION_NAME) }.isFailure)
        installed.longVersionCode = signed.longVersionCode - 1
        val error = runCatching { ApkUpdateIdentity.validate(installed, signed, context.packageName, BuildConfig.VERSION_NAME) }.exceptionOrNull()
        assertTrue(error?.message?.contains("签名不兼容") == true)
    }
}
