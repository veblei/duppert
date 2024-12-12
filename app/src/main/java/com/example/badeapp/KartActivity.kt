package com.example.badeapp


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import com.example.badeapp.viewmodel.KartActivityViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*
import java.lang.NullPointerException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.HashSet


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class KartActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var kart: GoogleMap
    private lateinit var gpsLokasjon: LatLng
    private lateinit var brukerMarker: MarkerOptions
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerListe = mutableListOf<MarkerOptions>()
    private var synligMarkerListe = mutableListOf<MarkerOptions>()
    private var luftTempListe = mutableListOf<String>()
    private var vannTempListe = mutableListOf<String>()
    private var navnListe = mutableListOf<String>()
    private val permissionID = 21 //Vilkårlig tall.
    private val viewModel by viewModels<KartActivityViewModel>()
    private val varselTidListe = mutableListOf<TextView>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kart)
        val kartFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        kartFragment.getMapAsync(this)
        //Lagrer tider for værvarsel i liste for å gjøre enklere å vise.
        varselTidListe.add(findViewById(R.id.R1C1))
        varselTidListe.add(findViewById(R.id.R1C2))
        varselTidListe.add(findViewById(R.id.R1C3))
        varselTidListe.add(findViewById(R.id.R1C4))
        varselTidListe.add(findViewById(R.id.R1C5))
        varselTidListe.add(findViewById(R.id.R1C6))
    }





    override fun onMapReady(googleMap: GoogleMap) {
        kart = googleMap
        val linearLayout = findViewById<LinearLayout>(R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(linearLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        /**Henter og setter markers på kartet.*/
        settMarkers()

        /**Bestemmer hvor kartet skal være/starte når det åpnes.*/
        settStartLokasjon()

        /**Marker klikk listener.*/
        kart.setOnMarkerClickListener { marker ->
            sjekkMarker(marker)
            true
        }

        /**Kart klikk listener.*/
        kart.setOnMapClickListener {
            lukkTastatur()
            findViewById<SearchView>(R.id.action_soke).isIconified = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            findViewById<CardView>(R.id.filterTab).visibility = View.INVISIBLE
            findViewById<CardView>(R.id.forslagCardView).visibility = View.INVISIBLE
        }

        /**Lar bruker filtrere markers etter luft- og vann temperatur.*/
        findViewById<Button>(R.id.aktiverFiltreKnapp).setOnClickListener {
            val luftTempInput = findViewById<EditText>(R.id.luftTempInput)
            val vannTempInput = findViewById<EditText>(R.id.vannTempInput)
            if (luftTempInput.text.toString().toDoubleOrNull() == null && vannTempInput.text.toString().toDoubleOrNull() == null) {
                Toast.makeText(applicationContext, "Ingen input gitt", Toast.LENGTH_SHORT).show()
                findViewById<CardView>(R.id.forslagCardView).visibility = View.INVISIBLE
            }
            else {
                aktiverFiltere(luftTempInput, vannTempInput)
            }
        }

        /**Listener for om bruker trykker på et item i søkeforslag.*/
        findViewById<ListView>(R.id.forslagsListe).setOnItemClickListener { parent, _, position, _ ->
            forslagTrykket(parent, position)
        }
    }





    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        /**Inflater og synliggjør menyen/toolbar øverst på kartet.*/
        menuInflater.inflate(R.menu.actions, menu)
        /**Søkefunksjon:*/
        val sokeItem = menu?.findItem(R.id.action_soke)
        val sokeView = sokeItem?.actionView as SearchView
        val forslagsListe = findViewById<ListView>(R.id.forslagsListe)
        val adapter:ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, navnListe)
        forslagsListe.adapter = adapter
        val linearLayout = findViewById<LinearLayout>(R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(linearLayout)
        sokeView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            /**Kjøres når brukeren trykker søk eller "check" på tastaturet.*/
            override fun onQueryTextSubmit(query: String?): Boolean {
                lukkTastatur()
                sokeView.clearFocus()
                sokeView.isIconified = true
                for (i in 0 until markerListe.size - 1) {
                    kart.addMarker(markerListe[i])
                }
                val input = query?.toLowerCase(Locale.ROOT)?.capitalize(Locale.ROOT)
                if (input == "") {
                    Toast.makeText(applicationContext, "Ingen input gitt", Toast.LENGTH_SHORT).show()
                } else {
                    var funnet = false
                    for (i in markerListe) {
                        if (input == i.title) {
                            kart.addMarker(i)
                            visBottomSheet(i)
                            funnet = true
                            break // Avbryter for-løkka når den finner riktig marker.
                        }
                    }
                    if (!funnet) {
                        Toast.makeText(applicationContext, "Fant ikke sted", Toast.LENGTH_SHORT).show()
                    }
                }
                sokeView.setQuery("", true)
                findViewById<CardView>(R.id.forslagCardView).visibility = View.INVISIBLE
                return false
            }
            /**Kjøres hver gang noe blir skrevet i søkefeltet, oppdaterer forslag.*/
            override fun onQueryTextChange(newText: String?): Boolean {
                findViewById<CardView>(R.id.forslagCardView).visibility = View.VISIBLE
                adapter.filter.filter(newText)
                return false
            }
        })
        /**Kjøres når bruker trykker på søke-ikonet i toolbar.*/
        sokeView.setOnSearchClickListener {
            findViewById<CardView>(R.id.forslagCardView).visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
        /**Kjøres når søkeviewet blir lukket, uavhengig av hvordan det blir lukket.*/
        sokeView.setOnCloseListener {
            lukkTastatur()
            findViewById<CardView>(R.id.forslagCardView).visibility = View.INVISIBLE
            false
        }
        return true
    }





    @SuppressLint("WrongViewCast")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /**Følger med på om bruker interagerer med toolbar øverst på kartet, og handler hvis det registreres et trykk.*/
        val linearLayout = findViewById<LinearLayout>(R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(linearLayout)
        val filterCardView = findViewById<CardView>(R.id.filterTab)
        val forslagCardView = findViewById<CardView>(R.id.forslagCardView)
        val sokeView = findViewById<SearchView>(R.id.action_soke)
        return when (item.itemId) {
            /**Synliggjør "filter-tab" som lar bruker filtrere markers etter luft- og vann temperatur.*/
            R.id.action_filtre -> {
                lukkTastatur()
                sokeView.isIconified = true
                if (filterCardView.visibility == View.VISIBLE) {
                    filterCardView.visibility = View.INVISIBLE
                } else {
                    forslagCardView.visibility = View.INVISIBLE
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    filterCardView.visibility = View.VISIBLE
                }
                true
            }
            /**Finner og viser hvilken marker som er nærmest bruker sin posisjon.*/
            R.id.action_naermeste -> {
                lukkTastatur()
                finnNaermeste()
                sokeView.isIconified = true
                forslagCardView.visibility = View.INVISIBLE
                true
            }
            /**Tilbakestiller filtere og kartets posisjon.*/
            R.id.action_tilbakestill -> {
                tilbakestill()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }





    private fun settMarkers() {
        /**Kommuniserer med viewmodel som henter markers, enten fra nettverkskall eller fra
         * lagret data, og sender dem til aktiviteten hvor markers legges på kartet.*/
        val sharedPref = getSharedPreferences("nettverkskall", MODE_PRIVATE)
        try {
            viewModel.hentKoordinater(sharedPref)
            viewModel.forecastModel.observe(this) { responsForecasts ->
                for (x in responsForecasts) {
                    val array = x.split("#").toTypedArray()
                    val marker = MarkerOptions()
                        .position(LatLng(array[1].toDouble(), array[2].toDouble()))
                        .title(array[0])
                        .snippet(x)
                    kart.addMarker(marker)
                    markerListe.add(marker)
                    synligMarkerListe.add(marker)
                    luftTempListe.add(array[4])
                    vannTempListe.add(array[7])
                    navnListe.add(array[0])
                }
                setSupportActionBar(findViewById(R.id.toolbar))
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                visStedFraFavoritter()
            }
        } catch (e : Exception) {
            Toast.makeText(applicationContext, "Nettverksfeil", Toast.LENGTH_SHORT).show()
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            visStedFraFavoritter()
        }
    }





    private fun settStartLokasjon() {
        /**Sjekker hva slags brukerlokasjon bruker har godtatt/skrudd på, og velger startposisjon
         * på kartet i henhold til dette.*/
        val sharedPreferences = getSharedPreferences("posisjon", MODE_PRIVATE)
        val switchState = sharedPreferences.getBoolean("switchState", true)
        val egenSwitchState = sharedPreferences.getBoolean("egenSwitchState", false)
        val streng = sharedPreferences.getString("streng", "")
        if (switchState) {
            hentLokasjon()
        } else if (egenSwitchState && streng != "") {
            val strengArray = streng?.split(",")?.toTypedArray()
            val posisjon = LatLng(strengArray?.get(0)?.toDouble()!!, strengArray[1].toDouble())
            brukerMarker = MarkerOptions().position(posisjon).title(getString(R.string.din_posisjon)).icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            kart.addMarker(brukerMarker)
            kart.moveCamera(CameraUpdateFactory.newLatLngZoom(posisjon, 10f))
        } else {
            val oslo = LatLng(59.911491, 10.757933)
            kart.moveCamera(CameraUpdateFactory.newLatLngZoom(oslo, 10f))
            Toast.makeText(applicationContext, "Fant ikke din posisjon", Toast.LENGTH_SHORT).show()
        }
    }





    private fun sjekkMarker(marker: Marker) {
        /**Kalles når en marker blir trykket. Sjekker hva slags marker det er.*/
        lukkTastatur()
        try {
            findViewById<SearchView>(R.id.action_soke).isIconified = true
        } catch (e: NullPointerException) {
            Log.d("NullPointerException", "action_soke is null")
        }
        findViewById<CardView>(R.id.filterTab).visibility = View.INVISIBLE
        findViewById<CardView>(R.id.forslagCardView).visibility = View.INVISIBLE
        /**Hvis marker som blir trykket er bruker sin posisjon.*/
        if (marker.title == getString(R.string.din_posisjon)) {
            kart.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 13f))
            findViewById<TextView>(R.id.bottomSheetNavn).text = marker.title
            findViewById<TextView>(R.id.luftTempInfo1).text = ""
            findViewById<TextView>(R.id.luftTempInfo2).text = ""
            findViewById<TextView>(R.id.vannTempInfo1).text = ""
            findViewById<TextView>(R.id.vannTempInfo2).text = ""
            findViewById<TextView>(R.id.tableInfo).text = ""
            findViewById<TableLayout>(R.id.table).isInvisible = true
            findViewById<Button>(R.id.leggTilFavoritter).visibility = View.INVISIBLE
            val linearLayout = findViewById<LinearLayout>(R.id.design_bottom_sheet)
            BottomSheetBehavior.from(linearLayout).state = BottomSheetBehavior.STATE_COLLAPSED
            findViewById<Button>(R.id.bottomSheetUrlKnapp).alpha = 0F
        }
        else {
            /**Oppdaterer og synliggjør informasjon på bottomsheet. Kalles når et sted er blitt valgt.*/
            for (x in synligMarkerListe) {
                if (x.title == marker.title) {
                    visBottomSheet(x)
                    break
                }
            }
        }
    }




    private fun aktiverFiltere(luftTempInput: TextView, vannTempInput: TextView) {
        /**Kalles når bruker trykker på "filtrer"-knappen inne i filter-tab.*/
        //Lukker alt og tømmer kartet først.
        lukkTastatur()
        val filterCardView = findViewById<CardView>(R.id.filterTab)
        filterCardView.visibility = View.INVISIBLE
        kart.clear()
        synligMarkerListe.clear()
        //Tilbakelegger bruker sin lokasjon som marker på kartet (hvis det er skrudd på).
        val sharedPreferences = getSharedPreferences("posisjon", MODE_PRIVATE)
        val switchState = sharedPreferences.getBoolean("switchState", true)
        val egenSwitchState = sharedPreferences.getBoolean("egenSwitchState", false)
        if (switchState || egenSwitchState) {
            kart.addMarker(brukerMarker)
        }
        //Itererer gjennom markerListe og legger til markers på kartet hvis de passer input.
        val luftInputGyldig = luftTempInput.text.toString().toDoubleOrNull() != null //True hvis gyldig input er gitt.
        val vannInputGyldig = vannTempInput.text.toString().toDoubleOrNull() != null //True hvis gyldig input er gitt.

        for (i in 0 until markerListe.size - 1) {
            if (sjekkFilterInput(luftInputGyldig, vannInputGyldig, luftTempInput.text.toString(), vannTempInput.text.toString(), luftTempListe[i], vannTempListe[i])) {
                kart.addMarker(markerListe[i])
                synligMarkerListe.add(markerListe[i])
            }
        }
        //Hvis ingen steder oppfyller filteret beholdes alle markere i stedet for å fjerne alle.
        if (synligMarkerListe.size==0) {
            Toast.makeText(applicationContext, "Ingen steder oppfyller filteret", Toast.LENGTH_SHORT).show()
            for (x in markerListe) {
                kart.addMarker(x)
                synligMarkerListe.add(x)
            }
        }
        luftTempInput.text = ""
        vannTempInput.text = ""
    }





    fun sjekkFilterInput(luftInputGyldig: Boolean, vannInputGyldig: Boolean, luftTempInput: String, vannTempInput: String, luftTemp: String, vannTemp: String): Boolean {
        /**Sjekker om inputet gitt i filter-tab er gyldig.*/
        if (luftInputGyldig && vannInputGyldig && luftTemp.toDoubleOrNull() != null && vannTemp.toDoubleOrNull() != null && luftTemp.toDouble() >= luftTempInput.toDouble() && vannTemp.toDouble() >= vannTempInput.toDouble()) {
            return true
        }
        if (!luftInputGyldig && vannInputGyldig && vannTemp.toDoubleOrNull() != null && vannTemp.toDouble() >= vannTempInput.toDouble()) {
            return true
        }
        if (luftInputGyldig && !vannInputGyldig && luftTemp.toDoubleOrNull() != null && luftTemp.toDouble() >= luftTempInput.toDouble()) {
            return true
        }
        return false
    }





    private fun forslagTrykket(parent: AdapterView<*>, position: Int) {
        /**Kjører når bruker trykker på et forslag i søkefunksjonen.*/
        lukkTastatur()
        findViewById<SearchView>(R.id.action_soke).isIconified = true
        findViewById<CardView>(R.id.forslagCardView).visibility = View.INVISIBLE
        val stedsnavn = parent.getItemAtPosition(position)
        val input = stedsnavn.toString().toLowerCase(Locale.ROOT).capitalize(Locale.ROOT)
        if (input == "") {
            Toast.makeText(applicationContext, "Fant ikke stedet", Toast.LENGTH_SHORT).show()
        } else {
            for (x in markerListe) {
                if (input == x.title) {
                    kart.addMarker(x)
                    visBottomSheet(x)
                    break //Avbryter for-løkka når den finner riktig marker.
                }
            }
        }
    }






    private fun tilbakestill() {
        /**Kalles hvis bruker trykker på tilbakestill i toolbar.*/
        //Lukker alt først.
        lukkTastatur()
        findViewById<SearchView>(R.id.action_soke).isIconified = true
        val linearLayout = findViewById<LinearLayout>(R.id.design_bottom_sheet)
        BottomSheetBehavior.from(linearLayout).state = BottomSheetBehavior.STATE_HIDDEN
        findViewById<CardView>(R.id.filterTab).visibility = View.INVISIBLE
        findViewById<CardView>(R.id.forslagCardView).visibility = View.INVISIBLE
        //Tilbakestiller markere.
        synligMarkerListe.clear()
        for (x in markerListe) {
            kart.addMarker(x)
            synligMarkerListe.add(x)
        }
        //Flytter kamera til bruker sin posisjon og zoomer ut.
        val switchState = getSharedPreferences("posisjon", MODE_PRIVATE).getBoolean("switchState", true)
        val egenSwitchState = getSharedPreferences("posisjon", MODE_PRIVATE).getBoolean("egenSwitchState", false)
        val oslo = LatLng(59.911491, 10.757933)
        if (switchState) {
            try {
                kart.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsLokasjon, 10f))
            } catch (e: UninitializedPropertyAccessException) {
                Toast.makeText(applicationContext, "Fant ikke GPS-posisjon", Toast.LENGTH_SHORT).show()
            }
        } else if (egenSwitchState) {
            val streng = getSharedPreferences("posisjon", MODE_PRIVATE).getString("streng", "")
            if (streng != "") {
                val strengArray = streng?.split(",")?.toTypedArray()
                val posisjon = LatLng(strengArray?.get(0)?.toDouble()!!, strengArray[1].toDouble())
                kart.animateCamera(CameraUpdateFactory.newLatLngZoom(posisjon, 10f))
            } else {
                kart.animateCamera(CameraUpdateFactory.newLatLngZoom(oslo, 10f))
            }
        } else {
            kart.animateCamera(CameraUpdateFactory.newLatLngZoom(oslo, 10f))
        }
        Toast.makeText(applicationContext, "Tilbakestilt", Toast.LENGTH_SHORT).show()
    }





    @SuppressLint("SetTextI18n")
    fun visBottomSheet(marker: MarkerOptions) {
        /**Oppdaterer og synliggjør informasjon på bottomsheet. Kalles når et sted er blitt valgt.*/
        kart.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 13f))
        val array = marker.snippet.split("#").toTypedArray()
        // Skjuler alt først.
        lukkTastatur()
        findViewById<CardView>(R.id.forslagCardView).visibility = View.INVISIBLE
        findViewById<CardView>(R.id.filterTab).visibility = View.INVISIBLE
        try {
            findViewById<SearchView>(R.id.action_soke).isIconified = true
        } catch (e: NullPointerException) {
            Log.d("NullPointerException", "action_soke is null")
        }
        //Setter stedsnavn:
        val stedsnavn = findViewById<TextView>(R.id.bottomSheetNavn)
        stedsnavn.text = marker.title
        //Setter temperaturer:
        findViewById<TextView>(R.id.luftTempInfo1).text = "Lufttemperatur ${array[4]} C°"
        findViewById<TextView>(R.id.luftTempInfo2).text = array[5]
        findViewById<TextView>(R.id.vannTempInfo1).text = "Vanntemperatur ${array[7]} C°"
        findViewById<TextView>(R.id.vannTempInfo2).text = array[8]
        //Setter værvarsel:
        findViewById<TableLayout>(R.id.table).isInvisible = false
        var tid = array[3]
        for (x in varselTidListe) {
            if (tid.toInt() >= 24) {
                tid = "00"
            }
            x.text = tid
            if (tid.toInt() < 10) {
                tid = "0${tid.toInt()+1}"
            } else {
                tid = "${tid.toInt()+1}"
            }
        }
        val temperaturer = array[6].split("¤").toTypedArray()
        findViewById<TextView>(R.id.R2C1).text = "${temperaturer[0]}°"
        findViewById<TextView>(R.id.R2C2).text = "${temperaturer[1]}°"
        findViewById<TextView>(R.id.R2C3).text = "${temperaturer[2]}°"
        findViewById<TextView>(R.id.R2C4).text = "${temperaturer[3]}°"
        findViewById<TextView>(R.id.R2C5).text = "${temperaturer[4]}°"
        findViewById<TextView>(R.id.R2C6).text = "${temperaturer[5]}°"
        // Setter opp og lytter for "mer info"-knappen:
        sjekkLink(stedsnavn.text.toString())
        //Listener for favoritt-knappen:
        favorittKnapp()
        //Gjør bottom sheet synlig med all informasjonen:
        val linearLayout = findViewById<LinearLayout>(R.id.design_bottom_sheet)
        BottomSheetBehavior.from(linearLayout).state = BottomSheetBehavior.STATE_EXPANDED
    }





    private fun favorittKnapp() {
        /**Lytter for favoritt-knappen om den blir trykket.*/
        val stedNavn = findViewById<TextView>(R.id.bottomSheetNavn).text.toString()
        sjekkFavoritt()
        findViewById<Button>(R.id.leggTilFavoritter).setOnClickListener {
            val sharedPreferences = getSharedPreferences("favoritter", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val hentetSet = sharedPreferences.getStringSet("favorittset", HashSet<String>())
            Log.d("FAV", hentetSet.toString())
            var erLik = false
            for (sted in hentetSet!!) {
                if (sted == stedNavn) {
                    Log.d("FAV", sted)
                    Log.d("FAV", stedNavn)
                    erLik = true
                }
            }
            if (erLik) {
                editor.clear()
                val set = HashSet<String>()
                set.addAll(hentetSet)
                set.remove(stedNavn)
                Log.d("FAV", set.toString())
                editor.putStringSet("favorittset", set)
                editor.apply()
            }
            else {
                editor.clear()
                val set = HashSet<String>()
                set.addAll(hentetSet)
                set.add(stedNavn)
                Log.d("FAV", set.toString())
                editor.putStringSet("favorittset", set)
                editor.apply()
            }
            sjekkFavoritt()
        }
    }





    private fun sjekkFavoritt() {
        /**Hvis et sted er lagt til som favoritt, vil hjerte-ikonet være fyllt inn når man trykker
         * på markeren. Hvis det ikke er fylt inn, er det ikke en favoritt.*/
        val favorittKnapp = findViewById<Button>(R.id.leggTilFavoritter)
        val stedNavn = findViewById<TextView>(R.id.bottomSheetNavn).text
        val sharedPreferences = getSharedPreferences("favoritter", MODE_PRIVATE)
        val hentetSet = sharedPreferences.getStringSet("favorittset", HashSet<String>())
        var erLik = false
        for (sted in hentetSet!!) {
            if (sted.toString() == stedNavn.toString()) {
                erLik = true
            }
        }
        if (erLik) {
            favorittKnapp.setBackgroundResource(R.drawable.ic_favorittikon1)
        }
        else {
            favorittKnapp.setBackgroundResource(R.drawable.ic_favorittikon2)
        }
    }





    private fun visStedFraFavoritter() {
        /**Viser sted fra FavoritterActivity hvis bruker kommer fra FavoritterActivity. Sjekker dette
         * ved å kalle på 'visBottomSheet(x)' bare hvis intenten ikke er lik null.*/
        //Henter stedsnavn fra intent.
        val stedsnavn = intent.getStringExtra("knappOnClick")
        var funnet = false
        //Sjekker om intenten er null eller ikke.
        if (stedsnavn != null) {
            for (x in markerListe) {
                if (x.title == stedsnavn) {
                    visBottomSheet(x)
                    funnet = true
                    break
                }
            }
            if (!funnet) {
                Toast.makeText(applicationContext, "Fant ikke sted", Toast.LENGTH_SHORT).show()
            }
        }
    }





    private fun sjekkLink(stedsnavn: String) {
        /**Sjekker om et sted har en link til Oslo kommune sin sider.*/
        val stedUrlKnapp = findViewById<Button>(R.id.bottomSheetUrlKnapp)
        var url = "https://www.oslo.kommune.no/natur-kultur-og-fritid/tur-og-friluftsliv/badeplasser-og-temperaturer/$stedsnavn"
        url = url.replace("ø", "o")
        url = url.replace("å", "a")
        viewModel.sjekkLink(url)
        viewModel.linkModel.observe(this) { gyldig ->
            if (gyldig) {
                stedUrlKnapp.alpha = 1F
                stedUrlKnapp.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = url.toUri()
                    startActivity(intent)
                }
            } else {
                stedUrlKnapp.alpha = 0.4F
                stedUrlKnapp.setOnClickListener {
                    Toast.makeText(applicationContext, "Mer informasjon ikke tilgjengelig", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }





    private fun finnNaermeste() {
        /**Sjekker hva slags posisjonsvalg bruker har valgt.*/
        val sharedPreferences = getSharedPreferences("posisjon", MODE_PRIVATE)
        val switchState = sharedPreferences.getBoolean("switchState", true)
        val egenSwitchState = sharedPreferences.getBoolean("egenSwitchState", false)
        if (switchState) {
            try {
                finnNaermesteMarker(gpsLokasjon)
            } catch (e: UninitializedPropertyAccessException) {
                Toast.makeText(applicationContext, "Fant ikke GPS-posisjon", Toast.LENGTH_SHORT).show()
            }
        } else if (egenSwitchState) {
            val streng = sharedPreferences.getString("streng", "")
            if (streng != "") {
                val strengArray = streng?.split(",")?.toTypedArray()
                val posisjon = LatLng(strengArray?.get(0)?.toDouble()!!, strengArray[1].toDouble())
                finnNaermesteMarker(MarkerOptions().position(posisjon).position)
            } else {
                Toast.makeText(applicationContext, "Du har ikke valgt en posisjon manuelt", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(applicationContext, "Fant ikke din posisjon", Toast.LENGTH_SHORT).show()
        }
    }





    private fun finnNaermesteMarker(brukerPos: LatLng) {
        /**Finner nærmeste marker i forhold til bruker sin lokasjon.*/
        var naermesteMarker = MarkerOptions()
        var forrigeDistanse = 0f
        for (i in synligMarkerListe) {
            val lat = i.position.latitude
            val long = i.position.longitude
            val brukerLat = brukerPos.latitude
            val brukerLong = brukerPos.longitude
            val resultat = FloatArray(1)
            Location.distanceBetween(lat, long, brukerLat, brukerLong, resultat)
            if (forrigeDistanse == 0f || resultat[0] < forrigeDistanse) {
                forrigeDistanse = resultat[0]
                naermesteMarker = i
            }
        }
        try {
            visBottomSheet(naermesteMarker)
        }
        catch (e: NullPointerException) {
            Toast.makeText(applicationContext, "Fant ingen steder", Toast.LENGTH_SHORT).show()
        }
    }





    private fun lukkTastatur() {
        /**Lukker/gjemmer tastaturet hvis det er synlig.*/
        val view = this.currentFocus
        if (view != null) {
            val inputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }





    /**Flere metoder som lar appen finne bruker sin GPS-lokasjon. Metodene sjekker også om appen har
     * tillatelse fra bruker til å hente ut lokasjon via stedstjenester, og hvis ikke ber appen
     * bruker om å skru på/godkjenne stedstjenester.*/
    @SuppressLint("MissingPermission")
    private fun hentLokasjon() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        gpsLokasjon = LatLng(location.latitude, location.longitude)
                        brukerMarker = MarkerOptions().position(gpsLokasjon).title(getString(R.string.din_posisjon)).icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        kart.addMarker(brukerMarker)
                        if (intent.getStringExtra("knappOnClick") == null) {
                            kart.moveCamera(CameraUpdateFactory.newLatLngZoom(gpsLokasjon, 10f))
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Skru på stedstjenester", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            val lastLocation: Location = p0.lastLocation
            gpsLokasjon = LatLng(lastLocation.latitude, lastLocation.longitude)
            brukerMarker = MarkerOptions().position(gpsLokasjon).title(getString(R.string.din_posisjon)).icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            kart.addMarker(brukerMarker)
            kart.moveCamera(CameraUpdateFactory.newLatLngZoom(gpsLokasjon, 10f))
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), permissionID)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionID && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            hentLokasjon()
        }
    }

}