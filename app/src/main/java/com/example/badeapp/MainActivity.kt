package com.example.badeapp


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)
    }


    override fun onResume() {
        super.onResume()
         /**Knapper som når trykket på sender bruker til en ny aktivitet/side.*/
        findViewById<Button>(R.id.kartKnapp).setOnClickListener {
            startKartActivity()
        }
        findViewById<Button>(R.id.innstillingerKnapp).setOnClickListener {
            startInnstillingerActivity()
        }
        findViewById<Button>(R.id.favKnapp).setOnClickListener {
            startFavoritterActivity()
        }
    }


    private fun startKartActivity() {
        val intent = Intent(this, KartActivity::class.java).apply {}
        startActivity(intent)
    }

    private fun startInnstillingerActivity() {
        val intent = Intent(this, InnstillingerActivity::class.java).apply {}
        startActivity(intent)
    }

    private fun startFavoritterActivity() {
        val intent = Intent(this, FavoritterActivity::class.java).apply {}
        startActivity(intent)
    }

}


