package com.example.womensafety

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Vibrator
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var shakeThreshold = 12f
    private var lastShakeTime = 0L

    private lateinit var phoneEdit: EditText
    private lateinit var saveBtn: Button
    private lateinit var statusTxt: TextView

    private val PREFS = "sos_prefs"
    private val KEY_PHONE = "sos_phone"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phoneEdit = findViewById(R.id.phoneEdit)
        saveBtn = findViewById(R.id.saveBtn)
        statusTxt = findViewById(R.id.statusTxt)

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        phoneEdit.setText(prefs.getString(KEY_PHONE, ""))

        saveBtn.setOnClickListener {
            val num = phoneEdit.text.toString().trim()
            prefs.edit().putString(KEY_PHONE, num).apply()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }

    override fun onSensorChanged(event: android.hardware.SensorEvent?) {
        if (event == null) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
        if (gForce > shakeThreshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > 1000) {
                lastShakeTime = now
                sendSOS()
            }
        }
    }

    private fun sendSOS() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val phone = prefs.getString(KEY_PHONE, "") ?: ""
        if (phone.isEmpty()) {
            Toast.makeText(this, "No number saved", Toast.LENGTH_SHORT).show()
            return
        }

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(200)

        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation.addOnSuccessListener { loc ->
            val message = if (loc != null) {
                "SOS! Need help. Location: https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
            } else {
                "SOS! Need help. Location not available."
            }

            try {
                val sms = SmsManager.getDefault()
                sms.sendTextMessage(phone, null, message, null, null)
                statusTxt.text = "SOS sent!"
            } catch (e: Exception) {
                statusTxt.text = "Failed to send SOS"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
