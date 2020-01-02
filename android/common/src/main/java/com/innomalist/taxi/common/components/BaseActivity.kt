package com.innomalist.taxi.common.components

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.innomalist.taxi.common.R
import com.innomalist.taxi.common.utils.LocaleHelper
import com.innomalist.taxi.common.utils.MyPreferenceManager

open class BaseActivity : AppCompatActivity() {
    @JvmField
    var toolbar: ActionBar? = null
    var screenDensity = 0f
    private var isFullscreen = false
    var isInForeground = false
    var showConnectionDialog = true
    val preferences: MyPreferenceManager
        get() {
            return MyPreferenceManager.getInstance(applicationContext)
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowAnimations()
        screenDensity = applicationContext.resources.displayMetrics.density
        setActivityTheme(this@BaseActivity)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    fun initializeToolbar(title: String?) {
        val toolbarView = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbarView)
        toolbar = supportActionBar
        if (toolbar != null) {
            toolbar!!.setDisplayHomeAsUpEnabled(true)
            toolbar!!.title = title
            toolbarView.setNavigationOnClickListener { v: View? -> onBackPressed() }
        }
    }

    val primaryColor: Int
        get() {
            val typedValue = TypedValue()
            val a = this.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
            val color = a.getColor(0, 0)
            a.recycle()
            return color
        }

    fun setupWindowAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.allowEnterTransitionOverlap = false
            window.allowReturnTransitionOverlap = false
            window.enterTransition = Fade()
            window.exitTransition = Fade()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus and isFullscreen) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    val currentTheme: Int
        get() = R.style.Theme_Default

    private fun setActivityTheme(activity: AppCompatActivity) {
        activity.setTheme(currentTheme)
    }

    /*@Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionEventReceived(event: SocketConnectionEvent) {
        if (!showConnectionDialog) return
        val eventResourceId = this.resources.getIdentifier("event_" + event.event, "string", this.packageName)
        var message = event.event
        if (eventResourceId > 0) message = this.getString(eventResourceId)
        if (event.event == Socket.EVENT_CONNECT) { /*if(connectionProgressDialog != null)
                connectionProgressDialog.dismiss();*/
            return
        }
        /*if(connectionProgressDialog == null) {
        } else {
            connectionProgressDialog.setContent(message);
        }*/
    }*/

    override fun onResume() {
        super.onResume()
        isInForeground = true
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
    }

    fun convertDPToPixel(dp: Int): Int {
        return (dp * screenDensity).toInt()
    }

    override fun setImmersive(immersive: Boolean) {
        isFullscreen = immersive
    }
}