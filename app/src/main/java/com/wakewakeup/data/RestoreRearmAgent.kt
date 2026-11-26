package com.wakewakeup.data

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import com.wakewakeup.session.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Restoring a backup brings the alarms back into the database, but AlarmManager knows nothing
 * about them. The files themselves are restored by the system (full backup only, see the
 * manifest); this agent only arms every alarm once that is done, so they ring before the app
 * is ever opened. It deliberately does not go through the Application object: the restore can
 * run in a process where the app's container was never built.
 */
class RestoreRearmAgent : BackupAgent() {

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    override fun onRestoreFinished() {
        runCatching {
            runBlocking(Dispatchers.IO) {
                AlarmRepository(AppDatabase.get(this@RestoreRearmAgent).alarmDao(), AlarmScheduler(this@RestoreRearmAgent)).rescheduleAll()
            }
        }
    }
}
