package com.wakewakeup.session

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.testing.AppEnv
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** TEC-01: sem Direct Boot, o alarme não toca se o celular reiniciar e não for desbloqueado. */
@RunWith(AndroidJUnit4::class)
class DirectBootBugTest {

    @get:Rule val env = AppEnv()

    private val pm get() = env.app.packageManager

    @Test
    fun theRingingComponentsAreDirectBootAware() {
        val pkg = env.app.packageName
        fun name(cls: Class<*>) = ComponentName(pkg, cls.name)
        assertTrue("AlarmReceiver", pm.getReceiverInfo(name(AlarmReceiver::class.java), 0).directBootAware)
        assertTrue("BootReceiver", pm.getReceiverInfo(name(BootReceiver::class.java), 0).directBootAware)
        assertTrue("AlarmRingService", pm.getServiceInfo(name(AlarmRingService::class.java), 0).directBootAware)
        assertTrue("RingActivity", pm.getActivityInfo(name(RingActivity::class.java), 0).directBootAware)
    }

    @Test
    fun theBootReceiverListensToLockedBootCompleted() {
        val intent = Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED).setPackage(env.app.packageName)
        val flags = PackageManager.MATCH_ALL or PackageManager.MATCH_DIRECT_BOOT_AWARE or PackageManager.MATCH_DIRECT_BOOT_UNAWARE
        val found = pm.queryBroadcastReceivers(intent, flags)
        assertTrue("ninguém trata LOCKED_BOOT_COMPLETED", found.any { it.activityInfo.name == BootReceiver::class.java.name })
    }
}
