package com.innomalist.taxi.rider.activities.splash

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.crashlytics.android.Crashlytics
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.PhoneBuilder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.innomalist.taxi.common.components.BaseActivity
import com.innomalist.taxi.common.interfaces.AlertDialogEvent
import com.innomalist.taxi.common.networking.socket.interfaces.Namespace
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import com.innomalist.taxi.common.networking.socket.interfaces.SocketNetworkDispatcher
import com.innomalist.taxi.common.utils.AlertDialogBuilder
import com.innomalist.taxi.common.utils.AlertDialogBuilder.DialogResult
import com.innomalist.taxi.common.utils.AlertDialogBuilder.show
import com.innomalist.taxi.common.utils.AlerterHelper.showError
import com.innomalist.taxi.common.utils.CommonUtils.isInternetDisabled
import com.innomalist.taxi.common.utils.LocationHelper.Companion.LatLngToDoubleArray
import com.innomalist.taxi.rider.R
import com.innomalist.taxi.rider.activities.main.MainActivity
import com.innomalist.taxi.rider.databinding.ActivitySplashBinding
import com.innomalist.taxi.rider.networking.http.Login
import com.innomalist.taxi.rider.networking.http.LoginResult
import io.fabric.sdk.android.Fabric

class SplashActivity : BaseActivity(), LocationListener {
    lateinit var binding: ActivitySplashBinding
    private var RC_SIGN_IN = 123
    private var locationTimeoutHandler: Handler? = null
    private var locationManager: LocationManager? = null
    var currentLocation: LatLng? = null
    private var isErrored = false
    private var goingToOpen = false
    private val permissionListener: PermissionListener = object : PermissionListener {
        @SuppressLint("MissingPermission")
        override fun onPermissionGranted() {
            val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@SplashActivity)
            mFusedLocationClient.lastLocation
                    .addOnSuccessListener(this@SplashActivity) { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currentLocation = LatLng(location.latitude, location.longitude)
                        }
                    }
            tryConnect()
        }

        override fun onPermissionDenied(deniedPermissions: List<String>) {
            tryConnect()
        }
    }
    private val onLoginButtonClicked = View.OnClickListener {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(listOf(PhoneBuilder().build()))
                        .setTheme(currentTheme)
                        .build(),
                RC_SIGN_IN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isImmersive = true
        showConnectionDialog = false
        super.onCreate(savedInstanceState)
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        Places.createClient(this)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (getString(R.string.fabric_key) != "") {
            Fabric.with(this, Crashlytics())
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding = DataBindingUtil.setContentView(this@SplashActivity, R.layout.activity_splash)
        binding.loginButton.setOnClickListener(onLoginButtonClicked)
        checkPermissions()
    }

    private fun checkPermissions() {
        if (isInternetDisabled(this)) {
            show(this, getString(R.string.message_enable_wifi), AlertDialogBuilder.DialogButton.CANCEL_RETRY, AlertDialogEvent { result: DialogResult ->
                if (result === DialogResult.RETRY) {
                    checkPermissions()
                } else {
                    finishAffinity()
                }
            })
            return
        }
        TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setDeniedMessage(getString(R.string.message_permission_denied))
                .setPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .check()
    }

    @SuppressLint("MissingPermission")
    private fun searchCurrentLocation() {
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        assert(locationManager != null)
        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1f, this)
    }

    private fun startMainActivity(latLng: LatLng) {
        if (goingToOpen) return
        goingToOpen = true
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        val array = LatLngToDoubleArray(latLng)
        intent.putExtra("currentLocation", array)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun tryConnect() {
        val token = preferences.token
        if (token != null && token.isNotEmpty()) {
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {fb ->
                SocketNetworkDispatcher.instance.connect(Namespace.Rider, token, fb.result?.token ?: "") {
                    when (it) {
                        is RemoteResponse.Success -> {
                            runOnUiThread {
                                locationTimeoutHandler = Handler()
                                locationTimeoutHandler!!.postDelayed({
                                    locationManager!!.removeUpdates(this@SplashActivity)
                                    if (currentLocation == null) {
                                        val location = getString(R.string.defaultLocation).split(",").toTypedArray()
                                        val lat = location[0].toDouble()
                                        val lng = location[1].toDouble()
                                        currentLocation = LatLng(lat, lng)
                                    }
                                    if (isErrored) return@postDelayed
                                    startMainActivity(currentLocation!!)
                                }, 5000)
                                searchCurrentLocation()
                            }

                        }

                        is RemoteResponse.Error -> {
                            runOnUiThread {
                                isErrored = true
                                binding.progressBar.visibility = View.GONE
                                showError(this, it.error.rawValue)
                            }

                        }
                    }

                }
            }
            goToLoadingMode()
        } else {
            goToLoginMode()
        }
    }


    override fun onResume() {
        super.onResume()
        tryConnect()
    }

    private fun tryLogin(firebaseToken: String) {
        isErrored = false
        goToLoadingMode()
        Login(firebaseToken).execute<LoginResult> {
            when(it) {
                is RemoteResponse.Success -> {
                    preferences.rider = it.body.user
                    preferences.token = it.body.token
                    tryConnect()
                }

                is RemoteResponse.Error -> {
                    showError(this, it.error.localizedDescription)
                }
            }

        }
        //eventBus.post(LoginEvent(java.lang.Long.valueOf(phone), BuildConfig.VERSION_CODE))
    }

    private fun goToLoadingMode() {
        binding.loginButton.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun goToLoginMode() {
        binding.loginButton.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                FirebaseAuth.getInstance().currentUser!!.getIdToken(false).addOnCompleteListener {
                    tryLogin(it.result!!.token!!)
                }
                return
            }
        }
        showError(this@SplashActivity, getString(R.string.login_failed))
        goToLoginMode()
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}
}