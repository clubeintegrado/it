package com.innomalist.taxi.rider.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.*
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.arlib.floatingsearchview.FloatingSearchView.OnLeftMenuClickListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.maps.android.SphericalUtil
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.innomalist.taxi.common.activities.chargeAccount.ChargeAccountActivity
import com.innomalist.taxi.common.activities.transactions.TransactionsActivity
import com.innomalist.taxi.common.activities.travels.TravelsActivity
import com.innomalist.taxi.common.interfaces.AlertDialogEvent
import com.innomalist.taxi.common.location.MapHelper.centerLatLngsInMap
import com.innomalist.taxi.common.models.Request
import com.innomalist.taxi.common.models.Service
import com.innomalist.taxi.common.networking.socket.CurrentRequestResult
import com.innomalist.taxi.common.networking.socket.GetCurrentRequestInfo
import com.innomalist.taxi.common.networking.socket.interfaces.EmptyClass
import com.innomalist.taxi.common.networking.socket.interfaces.RemoteResponse
import com.innomalist.taxi.common.utils.AlertDialogBuilder
import com.innomalist.taxi.common.utils.AlertDialogBuilder.DialogResult
import com.innomalist.taxi.common.utils.AlertDialogBuilder.show
import com.innomalist.taxi.common.utils.AlerterHelper
import com.innomalist.taxi.common.utils.AlerterHelper.showInfo
import com.innomalist.taxi.common.utils.AlerterHelper.showWarning
import com.innomalist.taxi.common.utils.DataBinder.setMedia
import com.innomalist.taxi.common.utils.LocationHelper.Companion.DoubleArrayToLatLng
import com.innomalist.taxi.common.utils.LocationHelper.Companion.LatLngToDoubleArray
import com.innomalist.taxi.common.utils.MyPreferenceManager.Companion.getInstance
import com.innomalist.taxi.rider.R
import com.innomalist.taxi.rider.activities.about.AboutActivity
import com.innomalist.taxi.rider.activities.addresses.AddressesActivity
import com.innomalist.taxi.rider.activities.coupon.CouponActivity
import com.innomalist.taxi.rider.activities.looking.LookingActivity
import com.innomalist.taxi.rider.activities.main.adapters.ServiceCategoryViewPagerAdapter
import com.innomalist.taxi.rider.activities.main.fragments.ServiceCarousalFragment.OnServicesCarousalFragmentListener
import com.innomalist.taxi.rider.activities.profile.ProfileActivity
import com.innomalist.taxi.rider.activities.promotions.PromotionsActivity
import com.innomalist.taxi.rider.activities.travel.TravelActivity
import com.innomalist.taxi.rider.databinding.ActivityMainBinding
import com.innomalist.taxi.rider.models.LocationWithName
import com.innomalist.taxi.rider.models.OrderedService
import com.innomalist.taxi.rider.models.RequestDTO
import com.innomalist.taxi.rider.networking.socket.*
import com.innomalist.taxi.rider.ui.RiderBaseActivity
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : RiderBaseActivity(), OnMapReadyCallback, OnServicesCarousalFragmentListener, LocationListener {
    lateinit var binding: ActivityMainBinding
    var mMap: GoogleMap? = null
    var markers: ArrayList<Marker> = ArrayList()
    var minutesFromNow = 0
    var driverMarkers: ArrayList<Marker>? = null
    var selectedService: Service? = null
    var serviceCategoryViewPagerAdapter: ServiceCategoryViewPagerAdapter? = null
    lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    var currentLocation: LatLng? = null
    var polylineOriginDestination: Polyline? = null
    var request = Request()
    override fun onServiceSelected(service: Service?) {
        selectedService = service
        binding.buttonRequest.isEnabled = (service != null)
        if (minutesFromNow == 0) binding.buttonRequest.setText(R.string.confirm_service) else {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, minutesFromNow)
            binding.buttonRequest.text = getString(R.string.book_later_button, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE])
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}
    fun onSelectTimeClicked(view: View?) {
        val timePickerDialog = TimePickerDialog(this@MainActivity, OnTimeSetListener { view1: TimePicker?, hourOfDay: Int, minute: Int ->
            val _minutesFromNow = hourOfDay * 60 + minute - (Calendar.getInstance()[Calendar.HOUR_OF_DAY] * 60 + Calendar.getInstance()[Calendar.MINUTE])
            binding.buttonRequest.text = getString(R.string.book_later_button, hourOfDay, minute)
            if (_minutesFromNow < 0) {
                show(this@MainActivity, getString(R.string.time_is_past_now), AlertDialogBuilder.DialogButton.OK, null)
            } else {
                minutesFromNow = _minutesFromNow
            }
        }, Calendar.getInstance()[Calendar.HOUR_OF_DAY], Calendar.getInstance()[Calendar.MINUTE], true)
        timePickerDialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.setImmersive(true)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)
        currentLocation = DoubleArrayToLatLng(intent.getDoubleArrayExtra("currentLocation")!!)
        binding.buttonConfirmPickup.isEnabled = false
        binding.buttonConfirmPickup.setOnClickListener { onButtonConfirmPickupClicked() }
        binding.buttonConfirmDestination.setOnClickListener { onButtonFinalDestinationClicked() }
        binding.buttonAddDestination.setOnClickListener { onButtonAddDestinationClicked() }
        binding.buttonRequest.setOnClickListener { onButtonConfirmServiceClicked() }
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
        binding.searchText.isSelected = true
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                binding.searchPlace.closeMenu(true)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
        binding.searchPlace.setOnLeftMenuClickListener(object : OnLeftMenuClickListener {
            override fun onMenuOpened() {
                if(binding.buttonConfirmPickup.visibility == View.VISIBLE) binding.drawerLayout.openDrawer(GravityCompat.START)
            }

            override fun onMenuClosed() {
                if (binding.buttonConfirmPickup.visibility == View.GONE) {
                    goBackFromServiceSelection()
                }
            }
        })
        binding.searchText.setOnClickListener { view: View? -> findPlace("") }
        binding.searchPlace.setSearchFocusable(false)
        binding.searchPlace.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_favorites -> {
                    GetAddresses().executeArray<com.innomalist.taxi.common.models.Address> {
                        when(it) {
                            is RemoteResponse.Success -> {
                                if (it.body.isEmpty()) {
                                    showWarning(this@MainActivity, getString(R.string.warning_no_favorite_place))
                                    return@executeArray
                                }
                                val addressStrings: Array<String> = it.body.map { add -> add.address!! }.toTypedArray()
                                val builder = MaterialAlertDialogBuilder(this)
                                        .setTitle("Locations")
                                        .setSingleChoiceItems(addressStrings, -1) { _: DialogInterface?, which: Int ->
                                            if (it.body[which].location != null) {
                                                mMap!!.animateCamera(CameraUpdateFactory.newLatLng(it.body[which].location))
                                            }
                                            binding.searchText.text = it.body[which].address
                                        }
                                builder.show()
                            }

                            is RemoteResponse.Error -> {
                                AlerterHelper.showError(this, it.error.status.name)
                            }
                        }

                    }
                }
                R.id.action_voice_rec -> displaySpeechRecognizer()
                R.id.action_location -> mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
            }
        }
        driverMarkers = ArrayList()
        val mapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mapFragment.getMapAsync(this)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.menu)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(getString(R.string.app_name))
        }
        refreshRequest()
        binding.navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            binding.drawerLayout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_item_favorites -> {
                    val intent = Intent(this@MainActivity, AddressesActivity::class.java)
                    val array = LatLngToDoubleArray(currentLocation!!)
                    intent.putExtra("currentLocation", array)
                    startActivity(intent)
                }
                R.id.nav_item_travels -> startActivity(Intent(this@MainActivity, TravelsActivity::class.java))
                R.id.nav_item_promotions -> startActivity(Intent(this@MainActivity, PromotionsActivity::class.java))
                R.id.nav_item_profile -> startActivityForResult(Intent(this@MainActivity, ProfileActivity::class.java), ACTIVITY_PROFILE)
                R.id.nav_item_charge_account -> startActivityForResult(Intent(this@MainActivity, ChargeAccountActivity::class.java), ACTIVITY_WALLET)
                R.id.nav_item_transactions -> startActivity(Intent(this@MainActivity, TransactionsActivity::class.java))
                R.id.nav_item_coupons -> startActivity(Intent(this@MainActivity, CouponActivity::class.java))
                R.id.nav_item_about -> startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                /*R.id.nav_item_exit -> show(this@MainActivity, getString(R.string.message_logout), AlertDialogBuilder.DialogButton.OK_CANCEL) { result: DialogResult ->
                    {
                        if (result == DialogResult.OK) {
                            logout()
                        }
                    }}*/
                R.id.nav_item_exit -> show(this@MainActivity, getString(R.string.message_logout), AlertDialogBuilder.DialogButton.OK_CANCEL, AlertDialogEvent { result: DialogResult -> if(result == DialogResult.OK) logout() })
                else -> Toast.makeText(this@MainActivity, menuItem.title, Toast.LENGTH_SHORT).show()
            }
            true
        }
        fillInfo()
    }

    private fun refreshRequest() {
        GetCurrentRequestInfo().execute<CurrentRequestResult> {
            when(it) {
                is RemoteResponse.Success -> {
                    travel = it.body.request
                    val notStartedStatusesArray: Array<Request.Status> = arrayOf(Request.Status.Booked, Request.Status.Found, Request.Status.Requested)
                    if (notStartedStatusesArray.contains(it.body.request.status)) {
                        startActivityForResult(Intent(this@MainActivity, LookingActivity::class.java), ACTIVITY_LOOKING)
                    } else {
                        val intent = Intent(this@MainActivity, TravelActivity::class.java)
                        startActivityForResult(intent, ACTIVITY_TRAVEL)
                    }
                }
            }
        }
    }
    private fun logout() {
        preferences.clearPreferences()
        finish()
    }

    private fun showCurvedPolyline(p1: LatLng, p2: LatLng, k: Double) {
        val d = SphericalUtil.computeDistanceBetween(p1, p2)
        val h = SphericalUtil.computeHeading(p1, p2)
        val p = SphericalUtil.computeOffset(p1, d * 0.5, h)
        val x = (1 - k * k) * d * 0.5 / (2 * k)
        val r = (1 + k * k) * d * 0.5 / (2 * k)
        val c = SphericalUtil.computeOffset(p, x, h + 90.0)
        val options = PolylineOptions()
        val pattern = Arrays.asList(Dash(30.0f), Gap(20.0f))
        val h1 = SphericalUtil.computeHeading(c, p1)
        val h2 = SphericalUtil.computeHeading(c, p2)
        val numpoints = 100
        val step = (h2 - h1) / numpoints
        for (i in 0 until numpoints) {
            val pi = SphericalUtil.computeOffset(c, r, h1 + i * step)
            options.add(pi)
        }
        polylineOriginDestination = mMap!!.addPolyline(options.width(10f).zIndex(100f).color(primaryColor).geodesic(true).pattern(pattern))
    }

    fun findPlace(preText: String?) {
        try { // Set the fields to specify which types of place data to return.
            val fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME)
            // Start the autocomplete intent.
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
            startActivityForResult(intent, ACTIVITY_PLACES)
            /*AutocompleteFilter autocompleteFilter = (new AutocompleteFilter.Builder()).setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS).build();
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .setFilter(autocompleteFilter)
                            .build(this);
            if (!TextUtils.isEmpty(preText)) {
                intent.putExtra("initial_query", preText);
            }
            startActivityForResult(intent, ACTIVITY_PLACES);*/
        } catch (ignored: Exception) {
        }
    }

    // Create an intent that can start the Speech Recognizer activity
    private fun displaySpeechRecognizer() {
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage(getString(R.string.message_permission_denied))
                .setPermissions(Manifest.permission.RECORD_AUDIO)
                .check()
    }

    var permissionlistener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getString(R.string.default_language))
                this@MainActivity.startActivityForResult(intent, ACTIVITY_VOICE_RECOGNITION)
            } catch (e: ActivityNotFoundException) {
                show(this@MainActivity, getString(R.string.question_install_speech), getString(R.string.error), AlertDialogBuilder.DialogButton.OK_CANCEL, AlertDialogEvent { result: DialogResult ->
                    if (result === DialogResult.OK) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/details?id=com.google.android.voicesearch"))
                        startActivity(browserIntent)
                    }
                })
            }
        }

        override fun onPermissionDenied(deniedPermissions: List<String>) {}
    }


    private fun onButtonConfirmPickupClicked() {
        binding.buttonConfirmDestination.isEnabled = false
        binding.buttonAddDestination.isEnabled = false
        showDestinationMarker()
        addDestination()
        TransitionManager.beginDelayedTransition((binding.root as ViewGroup), TransitionSet().addTransition(Slide()).addTransition(Fade()))
        binding.buttonConfirmPickup.visibility = View.GONE
        binding.buttonConfirmDestination.visibility = View.VISIBLE
        binding.buttonAddDestination.visibility = View.VISIBLE
        binding.searchPlace.openMenu(true)

    }

    private fun onButtonAddDestinationClicked() {
        addDestination()
    }

    private fun onButtonFinalDestinationClicked() {
        addDestination()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        binding.buttonRequest.isEnabled = false
        binding.buttonRequest.text = getString(R.string.confirm_service)
        binding.imageDestination.visibility = View.GONE
        mMap!!.uiSettings.setAllGesturesEnabled(false)
        TransitionManager.beginDelayedTransition((binding.root as ViewGroup), TransitionSet().addTransition(Fade()))
        binding.buttonConfirmDestination.visibility = View.GONE
        binding.buttonAddDestination.visibility = View.GONE
        binding.searchPlace.visibility = View.GONE
        CalculateFare(request.points).execute<CalculateFareResult> {
            when(it) {
                is RemoteResponse.Success -> {
                    minutesFromNow = 0
                    serviceCategoryViewPagerAdapter = ServiceCategoryViewPagerAdapter(supportFragmentManager, it.body.categories, it.body.distance, it.body.duration, it.body.currency)
                    binding.serviceTypesViewPager.adapter = serviceCategoryViewPagerAdapter
                    binding.serviceTypesViewPager.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    mMap!!.setPadding(0, binding.bottomSheet.height / 10, 0, binding.bottomSheet.height)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    centerLatLngsInMap(mMap!!, request.points, true)
                    binding.mapLayout.postDelayed({
                        for(x in 1 until markers.size) {
                            showCurvedPolyline(request.points[x - 1], request.points[x], 0.2)
                        }
                    }, 1500)
                    binding.tabCategories.setupWithViewPager(binding.serviceTypesViewPager)
                }

                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.status.name)
                    goBackFromServiceSelection()
                }
            }
        }
    }

    private fun addDestination() {
        request.points = request.points.plus(mMap!!.cameraPosition.target)
        request.addresses = request.addresses.plus(binding.searchText.text.toString())
        markers.add(mMap!!.addMarker(MarkerOptions()
                .position(mMap!!.cameraPosition.target)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_pickup))))
        mMap!!.animateCamera(CameraUpdateFactory.newLatLng(LatLng(mMap!!.cameraPosition.target.latitude + 0.001, mMap!!.cameraPosition.target.longitude)))
        if(!resources.getBoolean(R.bool.singlePointMode)) {
            binding.buttonConfirmPickup.visibility = View.GONE
            binding.buttonConfirmDestination.visibility = View.VISIBLE
            binding.buttonAddDestination.visibility = if(request.points.size > (resources.getInteger(R.integer.maximumDestinations) - 1)) View.GONE else View.VISIBLE
        }
    }

    private fun goBackFromServiceSelection() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        request.points = ArrayList()
        request.addresses = ArrayList()
        polylineOriginDestination?.remove()
        showPickupMarker()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        mMap!!.setPadding(0, 0, 0, 0)
        mMap!!.uiSettings.setAllGesturesEnabled(true)
        binding.buttonConfirmPickup.isEnabled = false
        TransitionManager.beginDelayedTransition((binding.root as ViewGroup), TransitionSet().addTransition(Fade()))
        binding.buttonConfirmPickup.visibility = View.VISIBLE
        binding.searchPlace.visibility = View.VISIBLE
        binding.searchPlace.closeMenu(false)
        for(marker in markers) {
            marker.remove()
        }
        markers = ArrayList()
    }

    private fun onButtonConfirmServiceClicked() {
        val locs = ArrayList<LocationWithName>()
        for(x in request.points.indices) {
            locs.add(LocationWithName(request.points[x], request.addresses[x]))
        }
        RequestService(RequestDTO(locs.toTypedArray(), arrayOf(OrderedService(selectedService!!.id, 1)), minutesFromNow)).execute<EmptyClass> {
            when(it) {
                is RemoteResponse.Success -> {
                    startActivityForResult(Intent(this@MainActivity, LookingActivity::class.java), ACTIVITY_LOOKING)
                }

                is RemoteResponse.Error -> {
                    AlerterHelper.showError(this, it.error.status.name)
                }
            }
        }
    }


    private fun showPickupMarker() {
        TransitionManager.beginDelayedTransition((binding.root as ViewGroup), Fade())
        if (binding.imageDestination.visibility == View.VISIBLE) binding.imageDestination.visibility = View.GONE
        binding.imagePickup.visibility = View.VISIBLE
    }

    private fun showDestinationMarker() {
        TransitionManager.beginDelayedTransition((binding.root as ViewGroup), Fade())
        if (binding.imagePickup.visibility == View.VISIBLE) binding.imagePickup.visibility = View.GONE
        binding.imageDestination.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
        mMap!!.isTrafficEnabled = true
        val locationManager = (this.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        mMap!!.setOnCameraIdleListener {
            val parentJob = Job()
            val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)
            coroutineScope.launch {
                getAddress(googleMap.cameraPosition.target)

            }

            GetDriversLocations(googleMap.cameraPosition.target).executeArray<LatLng> {
                when(it) {
                    is RemoteResponse.Success -> {
                        for (marker in driverMarkers!!) {
                            marker.remove()
                            driverMarkers!!.remove(marker)
                        }
                        for (driverLocation in it.body) driverMarkers!!.add(mMap!!.addMarker(MarkerOptions()
                                .position(driverLocation)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_taxi))))
                    }

                    is RemoteResponse.Error -> {
                        AlerterHelper.showError(this, it.error.status.name)
                    }
                }

            }
        }
        if (resources.getBoolean(R.bool.isNightMode)) {
            mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_night))
        }
    }

    suspend fun getAddress(location: LatLng) {
        val s = getAddressNetwork(location)
        binding.searchText.text = s
        if (binding.buttonConfirmPickup.visibility == View.VISIBLE) {
            binding.buttonConfirmPickup.isEnabled = true
        } else {
            binding.buttonConfirmDestination.isEnabled = true
            binding.buttonAddDestination.isEnabled = true
        }
    }

    suspend fun getAddressNetwork(location: LatLng): String = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
        var addresses: List<Address>? = null
        try {
            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (addresses != null && addresses.size > 0) {
            var address = ""
            if (addresses[0].thoroughfare != null) address = addresses[0].thoroughfare
            if (addresses[0].featureName != null) {
                if (address == "") address = addresses[0].featureName else address += ", " + addresses[0].featureName
            }
            return@withContext address
        } else return@withContext getString(R.string.unknown_location)
    }

    override fun onBackPressed() {
        if(binding.buttonConfirmPickup.visibility == View.VISIBLE) {
            show(this@MainActivity, getString(R.string.message_exit), AlertDialogBuilder.DialogButton.OK_CANCEL, AlertDialogEvent { result: DialogResult -> if (result === DialogResult.OK) finishAffinity() })
        } else {
            goBackFromServiceSelection()
        }
    }

    private fun fillInfo() {
        try {
            val name: String
            if (preferences.rider!!.status != null && preferences.rider!!.status == "blocked") {
                logout()
                return
            }
            name = if ((preferences.rider!!.firstName == null || preferences.rider!!.firstName!!.isEmpty()) && (preferences.rider!!.lastName == null || preferences.rider!!.lastName!!.isEmpty())) preferences.rider!!.mobileNumber.toString() else "${preferences.rider!!.firstName} ${preferences.rider!!.lastName}"
            val header = binding.navigationView.getHeaderView(0)
            (header.findViewById<View>(R.id.navigation_header_name) as TextView).text = name
            (header.findViewById<View>(R.id.navigation_header_charge) as TextView).text = preferences.rider!!.mobileNumber.toString()
            val imageView = header.findViewById<ImageView>(R.id.navigation_header_image)
            setMedia(imageView, preferences.rider!!.media)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*@SuppressLint("StaticFieldLeak")
    private inner class GetMarkerAddress : AsyncTask<Double, Void, String>() {
        protected override fun doInBackground(vararg floats: Double): String {

        }

        override fun onPostExecute(s: String) {
            super.onPostExecute(s)

        }
    }*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ACTIVITY_PROFILE -> {
                if (resultCode == Activity.RESULT_OK) showInfo(this@MainActivity, getString(R.string.info_edit_profile_success))
                fillInfo()
            }
            ACTIVITY_WALLET -> if (resultCode == Activity.RESULT_OK) showInfo(this@MainActivity, getString(R.string.account_charge_success))
            ACTIVITY_PLACES -> {
                binding.searchPlace.clearSearchFocus()
                if (resultCode == Activity.RESULT_OK) {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLng(place.latLng))
                } else if (resultCode == AutocompleteActivity.RESULT_ERROR) { // TODO: Handle the error.
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Log.i("PLACES", status.statusMessage)
                }
            }
            ACTIVITY_LOOKING -> if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(this@MainActivity, TravelActivity::class.java)
                startActivityForResult(intent, ACTIVITY_TRAVEL)
            } else {
                goBackFromServiceSelection()
            }
            ACTIVITY_VOICE_RECOGNITION -> if (resultCode == Activity.RESULT_OK) {
                val results: List<String> = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results.size > 0) findPlace(results[0]) else showWarning(this, getString(R.string.warning_voice_recognizer_failed))
            }
            ACTIVITY_TRAVEL -> goBackFromServiceSelection()
        }
    }

    companion object {
        private const val ACTIVITY_PROFILE = 11
        private const val ACTIVITY_WALLET = 12
        private const val ACTIVITY_PLACES = 13
        private const val ACTIVITY_TRAVEL = 14
        private const val ACTIVITY_VOICE_RECOGNITION = 15
        private const val ACTIVITY_LOOKING = 16
    }

}