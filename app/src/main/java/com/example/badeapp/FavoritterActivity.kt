package com.example.badeapp


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.collection.ArraySet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class FavoritterActivity : AppCompatActivity() {


    private lateinit var favoritter : Set<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favoritter)
        setSupportActionBar(findViewById(R.id.toolbar))
        val sharedPreferences = getSharedPreferences("favoritter", MODE_PRIVATE)
        favoritter = sharedPreferences.getStringSet("favorittset",  ArraySet())!!

        /**Lager recyclerView, layoytManager og adapteren.*/
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        val adapter = RecyclerViewAdapter(this)
        recyclerView.adapter = adapter

        /**Forteller brukeren at ingen favoritter er valgt hvis lista er tom.*/
        val textView = findViewById<TextView>(R.id.ingenFavoritter)
        if (favoritter.isEmpty()) {
            textView.text = "Ingen favoritter valgt"
        }
        else {
            textView.text = ""
        }
    }



    /**LAGER ADAPTER KLASSE*/
    private class RecyclerViewAdapter(val activity : FavoritterActivity) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(activity).inflate(R.layout.favoritt_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.item.findViewById<TextView>(R.id.favorittSted).text = activity.favoritter.elementAt(position)
            val sharedPreferences = activity.getSharedPreferences("favoritter", MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            /**Clearer sharedPreferences og loader favoritter på nytt utifra sharedPreferences med
             * stedet slettet. Oppdaterer til slutt recyclerView slik at endringen synes for brukeren.*/
            holder.slettFavorittKnapp.setOnClickListener {
                val hentetSet = sharedPreferences.getStringSet("favorittset",  ArraySet())!!
                editor.clear()
                val set = HashSet<String>()
                set.addAll(hentetSet)
                set.remove(holder.stedsnavn.text)
                editor.putStringSet("favorittset", set)
                editor.apply()
                activity.favoritter = sharedPreferences.getStringSet("favorittset",  ArraySet())!!
                notifyDataSetChanged()
            }

            /**Lagrer stedsnavnet som intent sender den til KartActivity samtidig som KartActivity åpnes.*/
            holder.visPaKartKnapp.setOnClickListener {
                val intent = Intent(activity, KartActivity::class.java).apply {}
                val stedsnavn = holder.stedsnavn.text
                intent.putExtra("knappOnClick", stedsnavn.toString())
                activity.startActivity(intent)
            }

            /**Clearer sharedPreferences og loader RecyclerView på nytt med en tom liste.*/
            val slettAlleFavoritterKnapp = activity.findViewById<Button>(R.id.slettAlleFavoritterKnapp)
            slettAlleFavoritterKnapp.setOnClickListener {
                editor.clear()
                editor.apply()
                activity.favoritter = sharedPreferences.getStringSet("favorittset",  ArraySet())!!
                notifyDataSetChanged()
            }
        }

        /**Returnerer antall elementer i favoritter.*/
        override fun getItemCount(): Int {
            return activity.favoritter.size
        }

        /**Lager viewsene i activity_favoritter.*/
        private class ViewHolder(v : View) : RecyclerView.ViewHolder(v) {
            val item = v.findViewById<CardView>(R.id.item)!!
            val slettFavorittKnapp = v.findViewById<Button>(R.id.favorittKnapp)!!
            val stedsnavn = v.findViewById<TextView>(R.id.favorittSted)!!
            val visPaKartKnapp = v.findViewById<Button>(R.id.visPaKart)!!
        }
    }
}