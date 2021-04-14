package com.nnt.mapapidemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.ArrayMap
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.nnt.mapapidemo.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), OnMapReadyCallback, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    private var mMap: GoogleMap?=null
    private var cameraPosition: CameraPosition? = null

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null
    private var currentLatLng: LatLng? =null
    private var likelyPlaceNames: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAddresses: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAttributions: Array<List<*>?> = arrayOfNulls(0)
    private var likelyPlaceLatLngs: Array<LatLng?> = arrayOfNulls(0)

    private lateinit var mapFragment: SupportMapFragment

    private var stores: List<Store> = Store.getStoreExampleList()
    private var storeMarkers = ArrayMap<Marker, Store>()
    private var reverseStoreMarkers = ArrayMap<Int, Marker>()
    private var currentMarker: Marker? = null

    private lateinit var binding: ActivityMainBinding
    private var adapter: StoreAdapter? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
            currentLatLng = LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
        }

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val btnConfirm = binding.btnConfirm
        btnConfirm.setOnClickListener {
            if(currentLatLng==null){
                Toast.makeText(this, "Vui lòng chọn địa chỉ", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(
                    this,
                    "Vĩ độ: ${currentLatLng!!.latitude}, Kinh độ: ${currentLatLng!!.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "" + currentLatLng!!.latitude + ", " + currentLatLng!!.longitude)
            }
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap?) {
        this.mMap = googleMap

        //setup google map padding
        val marginRight = resources.getDimensionPixelSize(R.dimen._4dp)
        val marginTop = resources.getDimensionPixelSize(R.dimen._48dp)
        mMap?.setPadding(marginRight, marginTop, marginRight, 0)

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.mMap?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            // Return null here, so that getInfoContents() is called next.
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the layouts for the info window, title and snippet.
                val infoWindow = layoutInflater.inflate(
                    R.layout.custom_info_contents,
                    findViewById<FrameLayout>(R.id.map), false
                )
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker.title
//                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
//                snippet.text = marker.snippet
                return infoWindow
            }
        })

        //setupRV
        setupSearchFeature()

        // Prompt the user for permission.
        getLocationPermission()

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()

        setupStoreMarker()

        mMap?.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStart(p0: Marker?) {

            }

            override fun onMarkerDrag(p0: Marker?) {
            }

            override fun onMarkerDragEnd(p0: Marker?) {
                currentLatLng = p0?.position
                currentMarker = p0
                mMap?.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
            }

        })

        mMap?.setOnMarkerClickListener {
                val store = storeMarkers[it]
                store?.let {
                    showStoreInfo(store)
                }
            return@setOnMarkerClickListener true
        }
        mMap?.setOnMyLocationButtonClickListener {
            currentMarker?.remove()
            getDeviceLocation()
            return@setOnMyLocationButtonClickListener true
        }

    }

    private fun setupSearchFeature(){
        setupKeyboardEvent()
        adapter = StoreAdapter(stores, ::onStoreSuggestionClick)
        binding.rvStore.apply {
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = this@MainActivity.adapter
        }

        val watcher = object : TextWatcher {
            private var searchFor = ""

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim()
                if (searchText == searchFor)
                    return

                searchFor = searchText

                launch {
                    delay(300)  //debounce timeOut
                    if (searchText != searchFor)
                        return@launch

                    // do our magic here
                    val shouldShowRv = adapter?.filter(searchFor)
                    if(shouldShowRv==true){
                        binding.rvStore.visibility = View.VISIBLE
                    }
                    else {
                        binding.rvStore.visibility = View.GONE
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        }
        binding.edtSearch.addTextChangedListener(watcher)
        binding.edtSearch.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) { // Perform action on key press
                    hideKeyboard()
                    return true
                }
                return false
            }
        })
    }
    private fun showStoreInfo(store: Store){
        StoreInfoDialog.newInstance(store).show(supportFragmentManager, StoreInfoDialog.TAG)
    }

    private fun setupStoreMarker(){
        for (store in stores){
            val marker = mMap?.addMarker(
                MarkerOptions()
                    .title(store.name)
                    .icon(getBitmapDescriptor())
                    .position(LatLng(store.latitude, store.longitude))
            )
            marker?.let {
                storeMarkers.put(marker, store)
                reverseStoreMarkers.put(store.id, marker)
            }
        }
    }

    private fun getBitmapDescriptor(): BitmapDescriptor{
        val width = resources.getDimensionPixelSize(R.dimen.marker_size)
        val height = width
        val bitmap = AppCompatResources.getDrawable(this, R.drawable.ic_store)?.toBitmap(
            width,
            height
        )
        if(bitmap!=null){
            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }else{
            return BitmapDescriptorFactory.defaultMarker()
        }
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }

    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                currentLatLng = null
                //getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            currentLatLng = LatLng(
                                lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude
                            )
                            mMap?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    currentLatLng, DEFAULT_ZOOM.toFloat()
                                )
                            )
                            currentMarker = mMap?.addMarker(
                                MarkerOptions()
                                    .draggable(true)
                                    .title("Drag and drop")
                                    .snippet("Drag and drop")
                                    .position(currentLatLng!!)
                            )
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)

                        mMap?.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        mMap?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

//    @SuppressLint("MissingPermission")
//    private fun showCurrentPlace() {
//        if ( mMap == null) {
//            return
//        }
//        if (locationPermissionGranted) {
//            // Use fields to define the data types to return.
//            val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
//
//            // Use the builder to create a FindCurrentPlaceRequest.
//            val request = FindCurrentPlaceRequest.newInstance(placeFields)
//
//            // Get the likely places - that is, the businesses and other points of interest that
//            // are the best match for the device's current location.
//            val placeResult = placesClient.findCurrentPlace(request)
//            placeResult.addOnCompleteListener { task ->
//                if (task.isSuccessful && task.result != null) {
//                    val likelyPlaces = task.result
//
//                    // Set the count, handling cases where less than 5 entries are returned.
//                    val count = if (likelyPlaces != null && likelyPlaces.placeLikelihoods.size < M_MAX_ENTRIES) {
//                        likelyPlaces.placeLikelihoods.size
//                    } else {
//                        M_MAX_ENTRIES
//                    }
//                    var i = 0
//                    likelyPlaceNames = arrayOfNulls(count)
//                    likelyPlaceAddresses = arrayOfNulls(count)
//                    likelyPlaceAttributions = arrayOfNulls<List<*>?>(count)
//                    likelyPlaceLatLngs = arrayOfNulls(count)
//                    for (placeLikelihood in likelyPlaces?.placeLikelihoods ?: emptyList()) {
//                        // Build a list of likely places to show the user.
//                        likelyPlaceNames[i] = placeLikelihood.place.name
//                        likelyPlaceAddresses[i] = placeLikelihood.place.address
//                        likelyPlaceAttributions[i] = placeLikelihood.place.attributions
//                        likelyPlaceLatLngs[i] = placeLikelihood.place.latLng
//                        i++
//                        if (i > count - 1) {
//                            break
//                        }
//                    }
//
//                    // Show a dialog offering the user the list of likely places, and add a
//                    // marker at the selected place.
//                    openPlacesDialog()
//                } else {
//                    Log.e(TAG, "Exception: %s", task.exception)
//                }
//                getDeviceLocation()
//            }
//        } else {
//            // The user has not granted permission.
//            Log.i(TAG, "The user did not grant location permission.")
//            // Add a default marker, because the user hasn't selected a place.
//            mMap?.addMarker(
//                MarkerOptions()
//                    .title(getString(R.string.default_info_title))
//                    .position(defaultLocation)
//                    .snippet(getString(R.string.default_info_snippet))
//            )
//
//            // Prompt the user for permission.
//            getLocationPermission()
//        }
//    }
//
//    private fun openPlacesDialog() {
//        // Ask the user to choose the place where they are now.
//        val listener = DialogInterface.OnClickListener { _, which -> // The "which" argument contains the position of the selected item.
//            val markerLatLng = likelyPlaceLatLngs[which]
//            var markerSnippet = likelyPlaceAddresses[which]
//            if (likelyPlaceAttributions[which] != null) {
//                markerSnippet = """
//                $markerSnippet
//                ${likelyPlaceAttributions[which]}
//                """.trimIndent()
//            }
//
//            // Add a marker for the selected place, with an info window
//            // showing information about that place.
//            mMap?.addMarker(
//                MarkerOptions()
//                    .title(likelyPlaceNames[which])
//                    .position(markerLatLng!!)
//                    .snippet(markerSnippet)
//            )
//
//            // Position the map's camera at the location of the marker.
//            mMap?.moveCamera(
//                CameraUpdateFactory.newLatLngZoom(
//                    markerLatLng,
//                    DEFAULT_ZOOM.toFloat()
//                )
//            )
//        }
//
//        // Display the dialog.
//        AlertDialog.Builder(this)
//            .setTitle(R.string.pick_place)
//            .setItems(likelyPlaceNames, listener)
//            .show()
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == R.id.option_get_place) {
//            showCurrentPlace()
//        }
//        return true
//    }
//    /**
//     * Sets up the options menu.
//     * @param menu The options menu.
//     * @return Boolean.
//     */
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.current_place_menu, menu)
//        return true
//    }

    override fun onSaveInstanceState(outState: Bundle) {
        mMap?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }

    private fun setupMyLocationButtonPosition(){
        val locationButton= (findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
            Integer.parseInt(
                "2"
            )
        )
        val rlp=locationButton.layoutParams as (RelativeLayout.LayoutParams)
        // position on right bottom
        val marginRight = resources.getDimensionPixelSize(R.dimen._16dp)
        val marginTop = resources.getDimensionPixelSize(R.dimen._48dp)

        rlp.setMargins(0, marginTop, marginRight, 0);
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val view: View? = currentFocus
        val ret = super.dispatchTouchEvent(event)
        if (view is EditText) {
            currentFocus?.let {
                val w: View = it
                val scrcoords = IntArray(2)
                w.getLocationOnScreen(scrcoords)

                val x: Float = event.rawX + w.left - scrcoords[0]
                val y: Float = event.rawY + w.top - scrcoords[1]
                if (event.action == MotionEvent.ACTION_UP
                    && (x < w.left || x >= w.right || y < w.top || y > w.bottom)
                ) {
                    hideKeyboard()
                }
            }
        }
        return ret
    }

    private fun hideKeyboard(){
        val view = this.currentFocus
        view?.let {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun setupKeyboardEvent() {
        KeyboardVisibilityEvent.setEventListener(this, object : KeyboardVisibilityEventListener {
            override fun onVisibilityChanged(isOpen: Boolean) {
                binding.btnConfirm.isVisible = !isOpen
                binding.rvStore.isVisible = isOpen && binding.edtSearch.text.isNotEmpty()
            }
        })
    }

    private fun onStoreSuggestionClick(store: Store){
        val marker = reverseStoreMarkers[store.id]
        marker?.showInfoWindow()
        mMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    store.latitude,
                    store.longitude
                ), DEFAULT_ZOOM.toFloat()
            )
        )
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5
    }
}