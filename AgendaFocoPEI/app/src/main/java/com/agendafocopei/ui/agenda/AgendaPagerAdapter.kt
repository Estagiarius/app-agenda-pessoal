package com.agendafocopei.ui.agenda

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class AgendaPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3 // Dia, Semana, Mês

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DiaFragment()
            1 -> SemanaFragment()
            2 -> MesFragment()
            else -> throw IllegalStateException("Posição inválida no ViewPager: $position")
        }
    }
}
