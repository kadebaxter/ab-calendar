package com.example.calendarnotes.google

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes

object GoogleCalendarAuth {
    private val calendarReadonlyScope = Scope(CalendarScopes.CALENDAR_READONLY)

    fun signInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(calendarReadonlyScope)
            .build()
    }

    fun getClient(activity: Activity): GoogleSignInClient {
        return GoogleSignIn.getClient(activity, signInOptions())
    }

    fun lastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun hasCalendarAccess(account: GoogleSignInAccount?): Boolean {
        if (account == null) return false
        return GoogleSignIn.hasPermissions(account, calendarReadonlyScope)
    }
}
