package com.innomalist.taxi.rider.ui

import android.os.Bundle
import com.innomalist.taxi.common.components.BaseActivity
import com.innomalist.taxi.common.models.Request
import com.innomalist.taxi.common.utils.MyPreferenceManager
import com.innomalist.taxi.common.utils.MyPreferenceManager.Companion.getInstance
import com.innomalist.taxi.common.utils.TravelRepository
import com.innomalist.taxi.common.utils.TravelRepository.get
import com.innomalist.taxi.common.utils.TravelRepository.set

abstract class RiderBaseActivity : BaseActivity() {
    var travel: Request?
        get() = get(this, TravelRepository.AppType.RIDER)
        protected set(request) {
            set(this, TravelRepository.AppType.RIDER, request!!)
        }
}