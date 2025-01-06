package com.example.mobileapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.os.Parcelable
import android.view.MenuItem
import android.widget.Filter
import android.widget.Filterable
import kotlinx.parcelize.Parcelize
import androidx.appcompat.widget.SearchView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private val eventList = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_page)

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navHome

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    true // Zaten MainPage'desin, bir işlem yapmana gerek yok
                }
                R.id.navMap -> {
                    val intent = Intent(this, Map::class.java)
                    intent.putExtra("events", ArrayList(eventList)) // eventList'i aktar
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navProfile -> {
                    val intent = Intent(this, ProfileSettings::class.java)
                    intent.putExtra("events", ArrayList(eventList)) // eventList'i aktar
                    startActivity(intent)
                    overridePendingTransition(0, 0) // Geçiş animasyonunu kaldır
                    true
                }
                R.id.navFavourite -> {
                    startActivity(Intent(this, LikedEvents::class.java))
                    overridePendingTransition(0,0)
                    true
                }
                else -> false
            }
        }


        val searchView: SearchView = findViewById(R.id.searchView)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        eventAdapter = EventAdapter(eventList) { event ->
            // Tıklanan etkinliğin detaylarını başka bir aktiviteye geçmek için intent oluştur
            val intent = Intent(this, EventDetail::class.java)
            intent.putExtra("event", event) // Parcelable olarak gönder
            startActivity(intent)
        }

        recyclerView.adapter = eventAdapter

        checkLocationPermission()

        val logoutButton = findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener { logoutUser() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // SearchView ile filtreleme
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                eventAdapter.filter.filter(query) // Filter işlemi burada yapılır
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                eventAdapter.filter.filter(newText) // Filter işlemi burada yapılır
                return true
            }
        })



    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    "Location permission is needed for this feature",
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        fetchEvents(it.latitude, it.longitude)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchEvents(latitude: Double, longitude: Double) {
        val apiKey = "TyhGz9TIvRxe8NXrdokLyqzNSxkEwpFu"
        //val url = "https://app.ticketmaster.com/discovery/v2/events.json?apikey=$apiKey&latlong=$latitude,$longitude"
        val url = "https://app.ticketmaster.com/discovery/v2/events.json?apikey=$apiKey"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainPage, "Failed to fetch events", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.use { responseBody ->
                    val json = JSONObject(responseBody.string())
                    if (json.has("_embedded")) {
                        val events = json.getJSONObject("_embedded").getJSONArray("events")
                        eventList.clear()
                        for (i in 0 until events.length()) {
                            val eventJson = events.getJSONObject(i)

                            val name = eventJson.getString("name")
                            val date = eventJson.getJSONObject("dates").getJSONObject("start").getString("localDate")
                            val venue = eventJson.getJSONObject("_embedded").getJSONArray("venues").getJSONObject(0)
                            val time = eventJson.getJSONObject("dates").getJSONObject("start").optString("localTime", "N/A")
                            val location = eventJson.getJSONObject("_embedded").getJSONArray("venues").getJSONObject(0).getString("name")
                            //val location = venue.getString("name")
                            val description = eventJson.optString("info", "No description available")

                            val imageUrl = eventJson.getJSONArray("images").getJSONObject(0).getString("url")

                            val latitude = venue.getJSONObject("location").getDouble("latitude")
                            val longitude = venue.getJSONObject("location").getDouble("longitude")
                            
                            val category = eventJson.getJSONArray("classifications").getJSONObject(0).getJSONObject("segment").getString("name")
                            // Yeni veri alanlarını alalım
                            val organizer = eventJson.optJSONObject("promoter")?.getString("name") ?: "Unknown"
                            val ticketPrices = eventJson.optJSONArray("priceRanges")?.let { range ->
                                if (range.length() > 0) {
                                    range.getJSONObject(0).optString("min", "Price not available")
                                } else {
                                    "Price not available"
                                }
                            } ?: "Price not available"

                            val event = Event(
                                name = name,
                                date = "$date $time",
                                latitude = latitude,
                                longitude=longitude,
                                location = location,
                                description = description,
                                imageUrl = imageUrl,
                                organizer = organizer, // Organizör bilgisi
                                ticketPrice = ticketPrices, // Bilet fiyatı
                                category = category
                            )
                            eventList.add(event)
                        }
                        runOnUiThread {
                            eventAdapter.notifyDataSetChanged()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainPage, "No events found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }


    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Uygulamayı kapat
        finishAffinity()
    }
    override fun onResume() {
        super.onResume()
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navHome // Ana sayfanın simgesini seç
    }
}



@Parcelize
data class Event(
    val name: String,
    val date: String,
    val latitude: Double,
    val longitude: Double,
    val location: String,
    val description: String,
    val imageUrl: String,
    val organizer: String, // Organizör bilgisi
    val ticketPrice: String, // Bilet fiyatı
    val category: String
) : Parcelable





class EventAdapter(
    private val eventList: List<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>(), Filterable {

    private var eventListFiltered: List<Event> = eventList // Filtered list

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventName: TextView = itemView.findViewById(R.id.eventNameTextView)
        val eventDate: TextView = itemView.findViewById(R.id.eventDateTextView)
        val eventLocation: TextView = itemView.findViewById(R.id.eventLocationTextView)
        val eventDesc: TextView = itemView.findViewById(R.id.eventDescTextView)
        val eventCategory: TextView = itemView.findViewById(R.id.eventCategoryTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventListFiltered[position]
        holder.eventName.text = event.name
        holder.eventDate.text = event.date
        holder.eventLocation.text = event.location
        holder.eventDesc.text = event.description
        holder.eventCategory.text = event.category

        // Handle item click
        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount() = eventListFiltered.size

    // Filter logic
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val query = constraint?.toString()?.lowercase()?.trim()

                filterResults.values = if (query.isNullOrEmpty()) {
                    eventList // No filter applied
                } else {
                    eventList.filter {
                        it.name.lowercase().contains(query) ||
                                it.description.lowercase().contains(query) ||
                                it.location.lowercase().contains(query)
                    }
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                eventListFiltered = results?.values as List<Event>
                notifyDataSetChanged()
            }
        }
    }


}


