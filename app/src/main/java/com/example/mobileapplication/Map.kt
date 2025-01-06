package com.example.mobileapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView

class Map : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val eventList = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)

        //NAVIGATION BAR EKLEYECEKSEN YORUM SATIRINI KALDIR!!!!
        //navigation menu content
        /*val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navMap

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    startActivity(Intent(this, MainPage::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navMap -> true // Zaten MapActivity'deyiz
                R.id.navProfile -> {
                    startActivity(Intent(this, ProfileSettings::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }*/

        // SupportMapFragment'ı programatik olarak ekleyelim
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction().add(R.id.map, mapFragment).commit()
        mapFragment.getMapAsync(this)

        val events = intent.getSerializableExtra("events") as? ArrayList<Event> ?: emptyList()
        eventList.addAll(events)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Log.d("MapActivity", "Map Fragment added and getMapAsync called")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        val sanFrancisco = LatLng(37.7749, -122.4194)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sanFrancisco, 12f))

        val locationEventMap = mutableMapOf<LatLng, MutableList<Event>>()
        // Konumları biraz kaydırmak için kullanılan küçük bir ofset
        val offset = 0.0001

        for (i in eventList.indices) {
            val event = eventList[i]
            val location = getLocationFromEvent(event)
            if (location != null) {
                val adjustedLocation = LatLng(location.latitude + i * offset, location.longitude + i * offset)
                val markerOptions = MarkerOptions()
                    .position(adjustedLocation)
                    .title("${event.name} - ${event.date}")
                    .snippet(event.description) // Display the event description as a snippet

                // Her etkinlik için ayrı ayrı pin ekle
                mMap.addMarker(markerOptions)?.tag = event
                Log.d("MapActivity", "Marker added for event: ${event.name}")
            } else {
                Log.d("MapActivity", "Event location not found for: ${event.name}")
            }
        }
        mMap.setOnMarkerClickListener { marker ->
            onMarkerClick(marker)
        }

    }

    fun onMarkerClick(marker: Marker): Boolean {
        val event = marker.tag as? Event
        event?.let {
            Toast.makeText(this, "${it.name} on ${it.date}\n${it.description}", Toast.LENGTH_LONG).show()
        }
        return false
    }

    private fun getLocationFromEvent(event: Event): LatLng {
        return LatLng(event.latitude, event.longitude)
        }


}