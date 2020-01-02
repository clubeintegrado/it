package com.innomalist.taxi.common

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.google.firebase.FirebaseApp

class MyTaxiApplication : Application() {
    override fun onCreate() {
        FirebaseApp.initializeApp(applicationContext)
        val nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        AppCompatDelegate.setDefaultNightMode(nightMode)
        super.onCreate()
    }

    override fun attachBaseContext(base: Context) {
        /*LocaleHelper.setLocale(base,"en");*/
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}