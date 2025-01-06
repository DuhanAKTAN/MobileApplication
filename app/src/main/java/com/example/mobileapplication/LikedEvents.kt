package com.example.mobileapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapplication.EventDetail.Companion.likedEvents
import com.google.android.material.bottomnavigation.BottomNavigationView

class LikedEvents : AppCompatActivity() {
    private val eventList = mutableListOf<Event>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_liked_events)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navHome

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    startActivity(Intent(this, MainPage::class.java))
                    overridePendingTransition(0,0)
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
                    true
                }
                else -> false
                    }
            }

        //loadLikedEvents()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Beğenilen etkinlikleri al ve RecyclerView ile göster
        eventAdapter = EventAdapter(EventDetail.likedEvents) { event ->
            // Beğenilen etkinlik tıklanırsa, detay sayfasına git
            val intent = Intent(this, EventDetail::class.java)
            intent.putExtra("event", event)
            startActivity(intent)
        }
        recyclerView.adapter = eventAdapter
    }
    override fun onResume() {
        super.onResume()
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navFavourite // Ana sayfanın simgesini seç
        }

    /*
    // SharedPreferences'ten beğenilen etkinlikleri yükleyin
    fun loadLikedEvents() {
        val sharedPreferences = getSharedPreferences("liked_events", MODE_PRIVATE)
        val likedEventNames = sharedPreferences.getStringSet("liked_event_names", setOf()) ?: setOf()

        // Etkinlik adlarına göre likedEvents listesini güncelle
        likedEvents.clear()
        likedEventNames.forEach { eventName ->
            val event = getEventByName(eventName)  // Etkinlik adını kullanarak etkinliği al
            event?.let { likedEvents.add(it) }
        }
    }

    // Etkinliği adına göre bulma fonksiyonu
    fun getEventByName(eventName: String): Event? {
        return eventList.find { it.name == eventName }  // eventList, etkinliklerin tüm listesidir.
    }*/


}
