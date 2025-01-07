package com.example.mobileapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.squareup.picasso.Picasso
import android.graphics.Color
import com.google.android.material.bottomnavigation.BottomNavigationView

class EventDetail : AppCompatActivity() {

    companion object {
        // Beğenilen etkinliklerin listesi (Uygulama çalıştığı sürece)
        val likedEvents = mutableListOf<Event>()
        val jointEvents = mutableListOf<Event>()
    }
    private val eventList = mutableListOf<Event>()
    private lateinit var currentEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_event_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Event nesnesini al
        val event = intent.getParcelableExtra<Event>("event")

        if (event == null) {
            Toast.makeText(this, "Event data is missing!", Toast.LENGTH_SHORT).show()
            finish() // Veriler eksikse etkinlik detay sayfasını kapat
            return
        }

        currentEvent = event // Gelen etkinliği `currentEvent` değişkenine ata

        // UI elemanlarını tanımla ve doldur
        val eventName = findViewById<TextView>(R.id.eventName)
        val eventDate = findViewById<TextView>(R.id.eventDate)
        val eventLocation = findViewById<TextView>(R.id.eventLocation)
        val eventDescription = findViewById<TextView>(R.id.eventDescription)
        val eventImage = findViewById<ImageView>(R.id.imageView)
        val eventOrganizator = findViewById<TextView>(R.id.eventOrganizer)
        val eventPrice = findViewById<TextView>(R.id.eventPrice)

        eventName.text = event.name
        eventDate.text = event.date
        eventLocation.text = event.location
        eventDescription.text = event.description
        eventOrganizator.text = event.organizer
        eventPrice.text = event.ticketPrice

        Picasso.get()
            .load(event.imageUrl) // Resmin URL'sini buraya ekliyoruz
            .placeholder(R.drawable.ic_launcher_foreground) // Resim yüklenene kadar gösterilecek resim
            .into(eventImage) // Resmin yükleneceği ImageView

        // Butonlar için Click Listener ekle
        val join = findViewById<Button>(R.id.joinBtn)
        join.setOnClickListener { joinOnClick() }

        val remember = findViewById<Button>(R.id.rememberBtn)
        remember.setOnClickListener { rememberOnClick() }

        val like = findViewById<Button>(R.id.likeBtn)
        like.setOnClickListener { likeOnClick() }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navHome

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    startActivity(Intent(this, MainPage::class.java))
                    overridePendingTransition(0, 0)
                    true
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

    }

    fun joinOnClick() {
        // Katıl butonu için işlevsellik buraya yazılacak
        currentEvent?.let { event ->
            if (jointEvents.contains(event)) {
                jointEvents.remove(event) // Etkinliği listeden çıkar
                Toast.makeText(this, "Event removed from joint list!", Toast.LENGTH_SHORT).show()
            } else {
                jointEvents.add(event) // Etkinliği listeye ekle
                Toast.makeText(this, "Event added to joint list!", Toast.LENGTH_SHORT).show()
            }
            // SharedPreferences'e kaydet
            saveLikedEvents()

        } ?: run {
            Toast.makeText(this, "No event to like/unlike!", Toast.LENGTH_SHORT).show()
        }
    }

    fun rememberOnClick() {
        // Hatırla butonu için işlevsellik buraya yazılacak
    }

    fun likeOnClick() {
        currentEvent?.let { event ->
            if (likedEvents.contains(event)) {
                likedEvents.remove(event) // Etkinliği listeden çıkar
                Toast.makeText(this, "Event removed from liked list!", Toast.LENGTH_SHORT).show()
            } else {
                likedEvents.add(event) // Etkinliği listeye ekle
                Toast.makeText(this, "Event added to liked list!", Toast.LENGTH_SHORT).show()
            }
            // SharedPreferences'e kaydet
            saveLikedEvents()
            updateLikeButtonUI()
        } ?: run {
            Toast.makeText(this, "No event to like/unlike!", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveLikedEvents() {
        val sharedPreferences = getSharedPreferences("liked_events", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Etkinliklerin isimlerini alıyoruz
        val likedEventNames = likedEvents.map { it.name }
        editor.putStringSet("liked_event_names", likedEventNames.toSet()) // Etkinlik isimlerini SharedPreferences'a kaydediyoruz
        editor.apply()
    }

    fun savejointEvents() {
        val sharedPreferences = getSharedPreferences("joint_events", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Etkinliklerin isimlerini alıyoruz
        val jointEventNames = jointEvents.map { it.name }
        editor.putStringSet("liked_event_names", jointEventNames.toSet()) // Etkinlik isimlerini SharedPreferences'a kaydediyoruz
        editor.apply()
    }



    fun updateLikeButtonUI() {
        val likeButton = findViewById<Button>(R.id.likeBtn)
        if (likedEvents.contains(currentEvent)) {
            likeButton.text = "Unlike" // Buton yazısını değiştir
            likeButton.setBackgroundColor(Color.GRAY)
        } else {
            likeButton.text = "Like"
            // Arka planı kırmızı yapmak için
            likeButton.setBackgroundColor(Color.RED)
        }
    }

}
