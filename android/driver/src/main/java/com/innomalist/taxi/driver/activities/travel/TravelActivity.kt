package com.innomalist.taxi.driver.activities.travel

import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.transition.TransitionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.maps.android.PolyUtil
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.innomalist.taxi.common.activities.chat.ChatActivity
import com.innomalist.taxi.common.interfaces.AlertDialogEvent
import com.innomalist.taxi.common.location.MapHelper.centerLatLngsInMap
import com.innomalist.taxi.common.models.Request
import com.innomalist.taxi.common.networking.socket.Cancel
import com.innomalist.taxi.common.networking.socket.CurrentRequestResult
import com.innomalist.taxi.common.networking.socket.GetCurrentRequestInfo
import com.innomalist.taxi.common.networking.socket.interfaces.EmptyClass
import com.innomalist.taxi.common.networking.socket.interfaces.SocketNetworkDispatcher
import com.innomalist.taxi.common.utils.AlertDialogBuilder
import com.innomalist.taxi.common.utils.AlertDialogBuilder.DialogResult
import com.innomalist.taxi.common.utils.AlertDialogBuilder.show
import com.innomalist.taxi.common.utils.AlerterHelper
import com.innomalist.taxi.common.utils.LoadingDialog
import com.innomalist.taxi.common.utils.LocationHelper.Companion.distFrom
import com.innomalist.taxi.driver.R
import com.innomalist.taxi.driver.databinding.ActivityTravelBinding
import com.innomalist.taxi.driver.networking.http.LocationUpdate
import com.innomalist.taxi.driver.networking.socket.Arrived
import com.innomalist.taxi.driver.networking.socket.Finish
import com.innomalist.taxi.driver.networking.socket.FinishResult
import com.innomalist.taxi.driver.networking.socket.Start
import com.innomalist.taxi.driver.ui.DriverBaseActivity
import ng.max.slideview.SlideView
import java.text.NumberFormat
import java.util.*

class TravelActivity : DriverBaseActivity(), OnMapReadyCallback, LocationListener {
    var gMap: GoogleMap? = null
    var currentLocation: LatLng? = null
    lateinit var binding: ActivityTravelBinding
    var pickupMarker: Marker? = null
    var driverMarker: Marker? = null
    var destinationMarker: Marker? = null
    var locationManager: LocationManager? = null
    var geoLog: MutableList<LatLng> = ArrayList()
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_travel)
        val mapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mapFragment.getMapAsync(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        SocketNetworkDispatcher.instance.onPaid = {
            LoadingDialog.hide()
            finish()
        }
        SocketNetworkDispatcher.instance.onCancel = {
            val req = travel!!
            req.status = Request.Status.RiderCanceled
            travel = req
            refreshPage()
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        binding.slideStart.setOnSlideCompleteListener { slideView: SlideView? -> startTravel() }
        binding.slideFinish.setOnSlideCompleteListener { slideView: SlideView? -> finishTravel() }
        binding.slideCancel.setOnSlideCompleteListener { slideView: SlideView? ->
            run {
                Cancel().execute<EmptyClass> {
                    when (it) {
                        is RemoteResponse.Success -> {
                            val request = travel
                            request!!.status = Request.Status.DriverCanceled
                            travel = request
                            refreshPage()
                        }
                    }
                }
            }
        }
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val request = travel
                    val timeStamp = if (request!!.startTimestamp == null) (request.etaPickup ?: 0) else (request.startTimestamp ?: 0) + (request.durationBest ?: 0) * 1000
                    val seconds = (timeStamp - Date().time) / 1000
                    if (seconds <= 0) binding.etaText.setText(R.string.eta_soon) else binding.etaText.text = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
                }
            }
        }, 0, 1000)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 20f, this)
        }
        GetCurrentRequestInfo().execute<CurrentRequestResult> {
            when(it) {
                is RemoteResponse.Success -> {
                    travel = it.body.request
                    val format: NumberFormat = NumberFormat.getCurrencyInstance()
                    format.currency = Currency.getInstance(it.body.request.currency)
                    binding.costText.text = format.format(it.body.request.costBest)
                    refreshPage()
                }

                is RemoteResponse.Error -> {
                    finish()
                }
            }
        }

    }

    override fun onPause() {
        locationManager!!.removeUpdates(this)
        super.onPause()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap!!.isTrafficEnabled = true
    }

    override fun onLocationChanged(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        LocationUpdate(preferences.token!!, latLng, true).execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {

                }

                is RemoteResponse.Error -> {

                }
            }

        }
        geoLog.add(LatLng(location.latitude, location.longitude))
        currentLocation = LatLng(location.latitude, location.longitude)
        refreshPage()
        val request = travel
        val destination: LatLng = if (request!!.startTimestamp == null) request.points[0] else request.points[1]
        val distance = distFrom(latLng, destination).toDouble()
        if (binding.root.resources.getBoolean(R.bool.use_miles)) binding.distanceText.text = binding.root.context.getString(R.string.unit_distance_miles, distance / 1609.344f) else binding.distanceText.text = getString(R.string.unit_distance, distance / 1000f)
    }

    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    fun startTravel() {
        Start().execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {
                    val req = travel
                    req!!.status = Request.Status.Started
                    travel = req
                    refreshPage()
                }

                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.status.rawValue)
                }
            }
        }
    }

    private fun refreshPage() {
        val request = travel
        when (request!!.status) {
            Request.Status.DriverAccepted -> {
                if (pickupMarker == null) pickupMarker = gMap!!.addMarker(MarkerOptions()
                        .position(request.points[0])
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_pickup))) else pickupMarker!!.setPosition(request.points!![0])
                if (driverMarker == null) {
                    if (currentLocation != null) driverMarker = gMap!!.addMarker(MarkerOptions().position(currentLocation!!).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_taxi)))
                } else {
                    if (currentLocation != null) driverMarker!!.setPosition(currentLocation!!) else {
                        driverMarker!!.remove()
                        driverMarker = null
                    }
                }
                if (destinationMarker != null) {
                    destinationMarker!!.remove()
                    destinationMarker = null
                }
                if (driverMarker == null) gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupMarker!!.position, 16f)) else {
                    val locations: MutableList<LatLng?> = ArrayList()
                    locations.add(pickupMarker!!.position)
                    locations.add(driverMarker!!.position)
                    centerLatLngsInMap(gMap!!, locations, true)
                }
            }
            Request.Status.DriverCanceled, Request.Status.RiderCanceled -> show(this@TravelActivity, getString(R.string.service_canceled), AlertDialogBuilder.DialogButton.OK, AlertDialogEvent { result: DialogResult? -> finish() })
            Request.Status.Started -> {
                if (pickupMarker != null) {
                    pickupMarker!!.remove()
                    pickupMarker = null
                }
                if (driverMarker == null) {
                    if (currentLocation != null) driverMarker = gMap!!.addMarker(MarkerOptions().position(currentLocation!!).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_taxi)))
                } else {
                    if (currentLocation != null) driverMarker!!.setPosition(currentLocation!!)
                }
                if (destinationMarker == null) destinationMarker = gMap!!.addMarker(MarkerOptions()
                        .position(request.points[1])
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_destination))) else destinationMarker!!.position = request.points[1]
                if (driverMarker == null) gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationMarker!!.position, 16f)) else {
                    val locations: MutableList<LatLng?> = ArrayList()
                    locations.add(destinationMarker!!.position)
                    locations.add(currentLocation)
                    centerLatLngsInMap(gMap!!, locations, true)
                }
                TransitionManager.beginDelayedTransition((binding.root as ViewGroup))
                binding.slideStart.visibility = View.GONE
                binding.slideCancel.visibility = View.GONE
                binding.slideFinish.visibility = View.VISIBLE
                binding.inLocationButton.visibility = View.GONE
                binding.chatButton.visibility = View.GONE
                binding.callButton.visibility = View.GONE
            }
            Request.Status.WaitingForPostPay -> {
                LoadingDialog.display(this)
            }
            Request.Status.WaitingForReview -> {
                LoadingDialog.hide()
                finish()
            }
            Request.Status.Finished -> {
                finish()
            }
        }
    }

    fun finishTravel() {
        var encodedPoly = ""
        if (geoLog.size > 0) encodedPoly = PolyUtil.encode(PolyUtil.simplify(geoLog, 10.0))
        if (travel!!.confirmationCode == null) {
            callFinish(travel!!.distanceReal!!.toInt(), encodedPoly)
        } else {
            showConfirmationDialog(encodedPoly)
        }
    }

    fun showConfirmationDialog(path: String) {
        val builder = MaterialAlertDialogBuilder(this)
                .setTitle("Verification")
                .setMessage("Finishing this service needs Confirmation code.")
                .setView(R.layout.dialog_input)
                .setPositiveButton("OK") { dialog: DialogInterface, which: Int ->
                    val dlg = dialog as AlertDialog
                    val txt = dlg.findViewById<TextInputEditText>(R.id.text1)
                    callFinish(travel!!.distanceReal!!.toInt(), path, txt!!.text.toString().toInt())

                }
        builder.show()
    }

    fun callFinish(distanceReal: Int, path: String, confirmationCode: Int? = null) {
        Finish(confirmationCode , distanceReal, path).execute<FinishResult> {
            when(it) {
                is RemoteResponse.Success -> {
                    runOnUiThread {
                        val req = travel!!
                        req.status = if (it.body.status) Request.Status.Finished else Request.Status.WaitingForPostPay
                        travel = req
                        refreshPage()
                    }
                }

                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.status.rawValue)
                }
            }
        }
    }

    fun onInLocationButtonClicked(v: View?) {
        Arrived().execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {
                    TransitionManager.beginDelayedTransition(binding.layoutActions)
                    binding.inLocationButton.visibility = View.GONE
                }

                is RemoteResponse.Error -> {

                }
            }

        }

    }

    var callPermissionListener: PermissionListener = object : PermissionListener {
        @SuppressLint("MissingPermission")
        override fun onPermissionGranted() {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:+" + travel!!.rider!!.mobileNumber)
            startActivity(intent)
        }

        override fun onPermissionDenied(deniedPermissions: List<String>) {}
    }

    fun onCallDriverClicked(view: View?) {
        TedPermission.with(this)
                .setPermissionListener(callPermissionListener)
                .setDeniedMessage(R.string.message_permission_denied)
                .setPermissions(Manifest.permission.CALL_PHONE)
                .check()
    }

    fun onChatButtonClicked(view: View?) {
        val intent = Intent(this@TravelActivity, ChatActivity::class.java)
        intent.putExtra("app", "driver")
        startActivity(intent)
    }
}