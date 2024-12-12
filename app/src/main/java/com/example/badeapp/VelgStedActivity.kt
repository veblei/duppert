package com.example.badeapp


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class VelgStedActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var kart: GoogleMap
    private lateinit var pos: LatLng


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_velg_sted)
        val kartFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        kartFragment.getMapAsync(this)
    }



    override fun onMapReady(googleMap: GoogleMap) {
        kart = googleMap

        /** Flytter kameraet til marker når kartet starter.*/
        flyttKamera()

        /** Setter marker der brukeren klikker og fjerner tidligere markers. */
        kart.setOnMapClickListener{ marker ->
            kart.clear()
            pos = LatLng(marker.latitude, marker.longitude)
            kart.addMarker(MarkerOptions().position(pos).title("Din posisjon").icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        }

        /** Lagrer posisjonen til markeren i SharedPreferences og går tilbake når brukeren trykker på bekreftKnapp. */
        findViewById<Button>(R.id.bekreftKnapp).setOnClickListener {
            val sharedPreferences = getSharedPreferences("posisjon", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            try {
                //Lager sharedPreferences of lagrer posisjonen brukeren har valgt.
                val streng = pos.latitude.toString() + "," + pos.longitude.toString()
                editor.putString("streng", streng)
                editor.putBoolean("switchState", false)
                editor.putBoolean("egenSwitchState", true)
                editor.apply()
                Toast.makeText(applicationContext, "Sted lagret!", Toast.LENGTH_SHORT).show()
                finish()
            }
            catch(e: Exception) {
                Toast.makeText(applicationContext, "Fant ikke sted", Toast.LENGTH_SHORT).show()
            }
        }
    }



    /** Flytter kameraet til marker. */
    private fun flyttKamera() {
        val sharedPreferences = getSharedPreferences("posisjon", MODE_PRIVATE)
        val streng = sharedPreferences.getString("streng", "")
        if (streng != "") {
            val strengArray = streng?.split(",")?.toTypedArray()
            pos = LatLng(strengArray?.get(0)?.toDouble()!!, strengArray[1].toDouble())
            kart.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 10f))
            kart.addMarker(MarkerOptions().position(pos).title("Din posisjon").icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        } else {
            val oslo = LatLng(59.911491, 10.757933)
            kart.moveCamera(CameraUpdateFactory.newLatLngZoom(oslo, 10f))
        }
    }

}
