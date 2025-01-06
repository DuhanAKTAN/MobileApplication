package com.example.mobileapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.collections.Map

class ProfileSettings : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val eventList = mutableListOf<Event>()
    private lateinit var joinedEventsRecyclerView: RecyclerView
    private val joinedEventsList = mutableListOf<Event>()
    private lateinit var joinedEventsAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_settings)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navProfile

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    startActivity(Intent(this, MainPage::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navMap -> {
                    val intent = Intent(this, Map::class.java)
                    intent.putExtra("events", ArrayList(eventList)) // eventList'i aktarıyoruz
                    startActivity(intent)
                    overridePendingTransition(0,0) // Geçiş animasyonunu kaldır
                    true
                }
                R.id.navFavourite -> {
                    startActivity(Intent(this, LikedEvents::class.java))
                    overridePendingTransition(0,0)
                    true
                }
                R.id.navProfile -> true // Zaten ProfileSettings'deyiz
                else -> false
            }
        }

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        val etUserName = findViewById<EditText>(R.id.etUserName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val cbNotifications = findViewById<CheckBox>(R.id.cbNotifications)
        val btnSaveChanges = findViewById<Button>(R.id.btnSaveChanges) // Değişiklikleri kaydet butonu
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Kullanıcı bilgilerini doldur
        currentUser?.let {
            etEmail.setText(it.email)
            etUserName.setText(it.displayName)
            // Firebase Realtime Database'den kullanıcı adı ve bildirim ayarlarını al
            val userId = it.uid
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Eğer veri varsa
                    if (snapshot.exists()) {
                        val username = snapshot.child("username").value?.toString() ?: "Username not found"
                        val notificationsEnabled = snapshot.child("notificationsEnabled").value as? Boolean ?: false
                        etUserName.setText(username)
                        cbNotifications.isChecked = notificationsEnabled
                    } else {
                        // Veri bulunamadığında kullanıcıyı bilgilendir
                        Toast.makeText(this@ProfileSettings, "No user data found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Hata durumunda hata mesajını göster
                    Toast.makeText(this@ProfileSettings, "Error loading user data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

            val eventsRef = FirebaseDatabase.getInstance().getReference("Events")
            eventsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    eventList.clear()  // Mevcut veriyi temizle
                    for (eventSnapshot in snapshot.children) {
                        val event = eventSnapshot.getValue(Event::class.java)
                        event?.let { eventList.add(it) }
                    }
                    Log.d("ProfileSettings", "Event list after onDataChange: ${eventList.size} events")
                    joinedEventsAdapter.notifyDataSetChanged()  // Adapter'a veri değiştiğini bildir
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileSettings, "Failed to load events: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })



        }

        btnSaveChanges.setOnClickListener {
            val username = etUserName.text.toString()
            val notificationsEnabled = cbNotifications.isChecked
            updateUserProfile(currentUser, username, notificationsEnabled)
        }

        // Kullanıcı bilgilerini güncelle
        etUserName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateUserProfile(currentUser, etUserName.text.toString(), cbNotifications.isChecked)
            }
        }
        cbNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateUserProfile(currentUser, etUserName.text.toString(), isChecked)
        }

        joinedEventsAdapter = EventAdapter(joinedEventsList) { event ->
            // Tıklama durumunda etkinliği kaydet
            //joinEvent(event.name)
            Toast.makeText(this, "Joined event: ${event.name}", Toast.LENGTH_SHORT).show()
        }


        joinedEventsRecyclerView = findViewById(R.id.rvJoinedEvents)
        joinedEventsRecyclerView.layoutManager = LinearLayoutManager(this)
        joinedEventsRecyclerView.adapter = joinedEventsAdapter



        // Çıkış yap butonu
        btnLogout.setOnClickListener {
            logoutUser()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun onResume() {
        super.onResume()
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navProfile // Profile settings simgesini seç
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }




    private fun updateUserProfile(user: FirebaseUser?, username: String, notificationsEnabled: Boolean) {
        user?.let {
            if (user == null) return

            val userId = it.uid
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

            val updates = mapOf(
                "username" to username,
                "notificationsEnabled" to notificationsEnabled
            )

            databaseRef.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
            }
        }
}