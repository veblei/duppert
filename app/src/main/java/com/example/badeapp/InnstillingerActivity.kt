package com.example.badeapp


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class InnstillingerActivity : AppCompatActivity(){



    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_innstillinger)

        val adresseSwitch = findViewById<Switch>(R.id.adresseSwitch)
        val egenPosisjonSwitch = findViewById<Switch>(R.id.egenPosisjonSwitch)
        val sharedPreferences = getSharedPreferences("posisjon", MODE_PRIVATE)

        /**Henter verdien til switchen i SharedPreferences og setter switchen lik den verdien */
        if (!sharedPreferences.getBoolean("switchState",true)) {
            adresseSwitch.isChecked = false
        }
        if (sharedPreferences.getBoolean("switchState",true)) {
            adresseSwitch.isChecked = true
        }
        if (!sharedPreferences.getBoolean("egenSwitchState",false)) {
            egenPosisjonSwitch.isChecked = false
        }
        if (sharedPreferences.getBoolean("egenSwitchState",false)) {
            egenPosisjonSwitch.isChecked = true
        }
    }



    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onResume() {
        super.onResume()

        val adresseSwitch = findViewById<Switch>(R.id.adresseSwitch)
        val egenPosisjonSwitch = findViewById<Switch>(R.id.egenPosisjonSwitch)
        val sharedPreferences = getSharedPreferences("posisjon", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        /** Starter VelgStedActivity når knappen blir trykket. */
        findViewById<Button>(R.id.adresseKnapp).setOnClickListener {
            startVelgStedActivity()
        }

        /** Sletter all brukerdata når knappen blir trykket. */
        findViewById<Button>(R.id.slettDataKnapp).setOnClickListener {
            editor.clear()
            editor.apply()
            adresseSwitch.isChecked = true
            editor.putBoolean("switchState", true)
            editor.apply()
            Toast.makeText(applicationContext, "Innsillinger tilbakestilt", Toast.LENGTH_SHORT).show()
        }

        /** Lagrer verdien til den trykte switchen i SharedPreferences når den blir trykket */
        adresseSwitch.setOnCheckedChangeListener { _: CompoundButton, b: Boolean ->
            editor.putBoolean("switchState", b)
            editor.apply()
            if (b) {
                egenPosisjonSwitch.isChecked = false
            }
        }
        egenPosisjonSwitch.setOnCheckedChangeListener { _: CompoundButton, b: Boolean ->
            editor.putBoolean("egenSwitchState", b)
            editor.apply()
            if (b) {
                adresseSwitch.isChecked = false
            }
        }
    }



    private fun startVelgStedActivity() {
        val intent = Intent(this, VelgStedActivity::class.java).apply {}
        startActivity(intent)
    }

}