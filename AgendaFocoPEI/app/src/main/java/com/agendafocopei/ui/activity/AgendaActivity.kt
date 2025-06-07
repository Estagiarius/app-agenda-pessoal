package com.agendafocopei.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.agendafocopei.databinding.ActivityAgendaBinding
import com.agendafocopei.ui.agenda.AgendaPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class AgendaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgendaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar a Toolbar
        setSupportActionBar(binding.toolbarAgenda)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // O título já está definido no XML como "Agenda"

        // Configurar ViewPager2 e TabLayout
        val agendaPagerAdapter = AgendaPagerAdapter(this)
        binding.viewPagerAgenda.adapter = agendaPagerAdapter

        TabLayoutMediator(binding.tabLayoutAgenda, binding.viewPagerAgenda) { tab, position ->
            tab.text = when (position) {
                0 -> "Dia"
                1 -> "Semana"
                2 -> "Mês"
                else -> null
            }
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
