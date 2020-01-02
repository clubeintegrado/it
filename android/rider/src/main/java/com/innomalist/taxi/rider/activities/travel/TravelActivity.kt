package com.innomalist.taxi.rider.activities.travel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.transition.TransitionManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.innomalist.taxi.common.activities.chargeAccount.ChargeAccountActivity
import com.innomalist.taxi.common.activities.chat.ChatActivity
import com.innomalist.taxi.common.interfaces.AlertDialogEvent
import com.innomalist.taxi.common.location.MapHelper.centerLatLngsInMap
import com.innomalist.taxi.common.models.Coupon
import com.innomalist.taxi.common.models.Request
import com.innomalist.taxi.common.models.Review
import com.innomalist.taxi.common.networking.socket.Cancel
import com.innomalist.taxi.common.networking.socket.CurrentRequestResult
import com.innomalist.taxi.common.networking.socket.GetCurrentRequestInfo
import com.innomalist.taxi.common.networking.socket.interfaces.EmptyClass
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import com.innomalist.taxi.common.networking.socket.interfaces.SocketNetworkDispatcher
import com.innomalist.taxi.common.utils.AlertDialogBuilder
import com.innomalist.taxi.common.utils.AlertDialogBuilder.show
import com.innomalist.taxi.common.utils.AlerterHelper
import com.innomalist.taxi.common.utils.AlerterHelper.showInfo
import com.innomalist.taxi.rider.R
import com.innomalist.taxi.rider.activities.coupon.CouponActivity
import com.innomalist.taxi.rider.activities.travel.adapters.TravelTabsViewPagerAdapter
import com.innomalist.taxi.rider.activities.travel.fragments.ReviewDialog
import com.innomalist.taxi.rider.activities.travel.fragments.ReviewDialog.onReviewFragmentInteractionListener
import com.innomalist.taxi.rider.activities.travel.fragments.TabStatisticsFragment.onTravelInfoReceived
import com.innomalist.taxi.rider.databinding.ActivityTravelBinding
import com.innomalist.taxi.rider.networking.socket.EnableVerification
import com.innomalist.taxi.rider.networking.socket.ReviewDriver
import com.innomalist.taxi.rider.ui.RiderBaseActivity
import ng.max.slideview.SlideView
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

class TravelActivity : RiderBaseActivity(), OnMapReadyCallback, onReviewFragmentInteractionListener, onTravelInfoReceived {
    lateinit var binding: ActivityTravelBinding
    private var pointMarkers: MutableList<Marker> = ArrayList()
    private var driverMarker: Marker? = null
    private var driverLocation: LatLng? = null
    private var gMap: GoogleMap? = null
    private var travelTabsViewPagerAdapter: TravelTabsViewPagerAdapter? = null
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_travel)
        binding.slideCancel.setOnSlideCompleteListener {
            Cancel().execute<EmptyClass> {
                when(it) {
                    is RemoteResponse.Success -> {
                        updateTravelStatus(Request.Status.RiderCanceled)
                        refreshPage()
                    }

                    is RemoteResponse.Error -> {
                        AlerterHelper.showError(this, it.error.status.name)
                    }
                }

            }
        }
        binding.slideCall.setOnSlideCompleteListener { slideView: SlideView? -> onCallDriverClicked(null) }
        binding.chatButton.setOnClickListener { v: View? ->
            val intent = Intent(this@TravelActivity, ChatActivity::class.java)
            intent.putExtra("app", "rider")
            startActivity(intent)
        }
        SocketNetworkDispatcher.instance.started.subscribe {
            travel = it
            showInfo(this@TravelActivity, getString(R.string.message_travel_started))
            refreshPage()
        }
        SocketNetworkDispatcher.instance.onFinished = {
            travel!!.costAfterCoupon = it.remainingAmount
            val req = travel!!
            req.status = if (it.paid) (if(travelTabsViewPagerAdapter!!.count == 2) Request.Status.Finished else Request.Status.WaitingForReview) else Request.Status.WaitingForPostPay
            travel = req
            refreshPage()
        }
        SocketNetworkDispatcher.instance.onCancel = {
            updateTravelStatus(Request.Status.DriverCanceled)
            refreshPage()
        }
        val mapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mapFragment.getMapAsync(this)
        travelTabsViewPagerAdapter = TravelTabsViewPagerAdapter(supportFragmentManager, this@TravelActivity, travel!!)
        binding.viewpager.adapter = travelTabsViewPagerAdapter
        binding.tabLayout.setupWithViewPager(binding.viewpager)
        val request = travel
        if (request!!.rating != null) {
            travelTabsViewPagerAdapter!!.deletePage(2)
            val tab = binding.tabLayout.getTabAt(0)
            tab?.select()
        }
    }

    override fun onResume() {
        super.onResume()
        if (gMap != null) {
            requestRefresh()
        }
    }

    fun requestRefresh() {
        GetCurrentRequestInfo().execute<CurrentRequestResult> {
            when(it) {
                is RemoteResponse.Success -> {
                    travel = it.body.request
                    driverLocation = it.body.driverLocation
                    refreshPage()
                }

                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.status.name)
                }
            }


        }
    }

    private fun refreshPage() {
        val request = travel
        if (request!!.service != null && request.service!!.canUseConfirmationCode == 1) {
            binding.enableVerificationButton.visibility = View.VISIBLE
        } else {
            binding.enableVerificationButton.visibility = View.GONE
        }
        when (request.status) {
            Request.Status.DriverAccepted, Request.Status.Started -> {
                for(marker in pointMarkers) {
                    marker.remove()
                }
                pointMarkers = ArrayList()
                for(point in request.points.iterator()) {
                    val marker =  gMap!!.addMarker(MarkerOptions()
                            .position(point)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_destination)))
                    pointMarkers.add(marker)
                }
                var points = request.points
                if(driverLocation != null) {
                    points = points!!.plus(driverLocation!!)
                    if(driverMarker == null) {
                        driverMarker = gMap!!.addMarker(MarkerOptions().position(driverLocation!!).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_taxi)))
                    } else {
                        driverMarker!!.position = driverLocation
                    }
                }
                centerLatLngsInMap(gMap!!, points, true)
                if(request.status == Request.Status.Started) {
                    TransitionManager.beginDelayedTransition((binding.root as ViewGroup))
                    binding.slideCall.visibility = View.GONE
                    binding.chatButton.visibility = View.GONE
                    binding.slideCancel.visibility = View.GONE
                }
            }
            Request.Status.DriverCanceled, Request.Status.RiderCanceled -> show(this@TravelActivity, getString(R.string.service_canceled), AlertDialogBuilder.DialogButton.OK, AlertDialogEvent { finish() })
            Request.Status.WaitingForPostPay -> {
                val intent = Intent(this, ChargeAccountActivity::class.java)
                intent.putExtra("defaultAmount", travel!!.costAfterCoupon)
                intent.putExtra("currency", travel!!.currency)
                startActivity(intent)
            }
            Request.Status.WaitingForReview -> {
                val fm = supportFragmentManager
                val reviewDialog: ReviewDialog = ReviewDialog.newInstance()
                reviewDialog.show(fm, "fragment_review_travel")
            }
            Request.Status.Finished -> {
                showInfo(this, "Finished!")
                finish()
            }

            else -> {
                show(this, "Unknown event found: ${travel!!.status!!.value}", AlertDialogBuilder.DialogButton.OK, null)
            }
        }
    }

    private fun updateTravelStatus(status: Request.Status) {
        val request = travel
        request!!.status = status
        travel = request
    }


    fun onChargeAccountClicked(view: View?) {
        val intent = Intent(this@TravelActivity, ChargeAccountActivity::class.java)
        intent.putExtra("defaultAmount", travel!!.costAfterCoupon)
        intent.putExtra("currency", travel!!.currency)
        startActivity(intent)
    }

    fun onApplyCouponClicked(view: View?) {
        val intent = Intent(this@TravelActivity, CouponActivity::class.java)
        intent.putExtra("select_mode", true)
        startActivityForResult(intent, ACTIVITY_COUPON)
    }

    override fun onBackPressed() {}
    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap!!.isTrafficEnabled = true
        gMap!!.setMaxZoomPreference(17f)
        refreshPage()
    }

    var callPermissionListener: PermissionListener = object : PermissionListener {
        @SuppressLint("MissingPermission")
        override fun onPermissionGranted() {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:+" + travel!!.driver!!.mobileNumber)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        override fun onPermissionDenied(deniedPermissions: List<String>) {}
    }

    override fun onReviewTravelClicked(review: Review) {
        ReviewDriver(review).execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {
                    if (travel!!.status == Request.Status.WaitingForReview) {
                        updateTravelStatus(Request.Status.Finished)
                        refreshPage()
                        return@execute
                    }
                    showInfo(this@TravelActivity, getString(R.string.message_review_sent))
                    travelTabsViewPagerAdapter!!.deletePage(2)
                    val tab = binding.tabLayout.getTabAt(0)
                    tab?.select()
                }

                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.status.name)
                }
            }

        }
    }

    fun onCallDriverClicked(view: View?) {
        TedPermission.with(this@TravelActivity)
                .setPermissionListener(callPermissionListener)
                .setDeniedMessage(R.string.message_permission_denied)
                .setPermissions(Manifest.permission.CALL_PHONE)
                .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_COUPON) {
            if (resultCode == Activity.RESULT_OK) {
                val coupon = data!!.getSerializableExtra("coupon") as Coupon
                var message = ""
                val format: NumberFormat = NumberFormat.getCurrencyInstance()
                format.currency = Currency.getInstance(travel!!.currency)
                if (coupon.flatDiscount == 0.0 && coupon.discountPercent != 0) message = "Coupon with discount of ${coupon.discountPercent}% has been applied."
                if (coupon.flatDiscount != 0.0 && coupon.discountPercent == 0) message = "Coupon with discount of ${format.format(coupon.flatDiscount)} has been applied."
                if (coupon.flatDiscount != 0.0 && coupon.discountPercent != 0) message = "Coupon with discount of ${format.format(coupon.flatDiscount)} & ${coupon.discountPercent}% has been applied."
                if (message == "") return
                showInfo(this@TravelActivity, message)
                travelTabsViewPagerAdapter!!.statisticsFragment!!.onUpdatePrice(data.getDoubleExtra("costAfterCoupon", travel!!.costBest!!))
            }
        }
    }

    override fun onReceived(driverLocation: LatLng?) {
        this.driverLocation = driverLocation
        refreshPage()
    }

    fun onEnableVerification(view: View?) {
        EnableVerification().execute<Int> {
            when(it) {
                is RemoteResponse.Success -> {
                    show(this@TravelActivity, getString(R.string.confirmation_code_message, it.body), AlertDialogBuilder.DialogButton.OK,null)
                }

                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.status.name)
                }
            }

        }
    }


    companion object {
        private const val ACTIVITY_COUPON = 700
    }
}