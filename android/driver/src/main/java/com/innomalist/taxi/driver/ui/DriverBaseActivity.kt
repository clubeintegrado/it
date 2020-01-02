package com.innomalist.taxi.driver.ui

import android.annotation.SuppressLint
import android.os.Bundle
import com.innomalist.taxi.common.components.BaseActivity
import com.innomalist.taxi.common.models.Request
import com.innomalist.taxi.common.utils.MyPreferenceManager
import com.innomalist.taxi.common.utils.MyPreferenceManager.Companion.getInstance
import com.innomalist.taxi.common.utils.TravelRepository
import com.innomalist.taxi.common.utils.TravelRepository.get
import com.innomalist.taxi.common.utils.TravelRepository.set

@SuppressLint("Registered")
open class DriverBaseActivity : BaseActivity() {
    lateinit var SP: MyPreferenceManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SP = getInstance(applicationContext)
    }

    protected var travel: Request?
        get() = get(this, TravelRepository.AppType.DRIVER)
        protected set(request) {
            set(this, TravelRepository.AppType.DRIVER, request!!)
        }
}