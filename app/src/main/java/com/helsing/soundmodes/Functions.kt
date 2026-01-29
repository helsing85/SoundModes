package com.helsing.soundmodes

import android.app.NotificationManager
import android.content.Context

fun isNotificationPolicyAccessPermissionEnabled(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted()

}