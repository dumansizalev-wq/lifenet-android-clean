package net.lifenet.core.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.lifenet.core.ui.fragments.HomeFragment
import net.lifenet.core.ui.fragments.ConnectionsFragment
import net.lifenet.core.ui.fragments.SecureChatFragment
import net.lifenet.core.ui.fragments.NearbyDevicesFragment
import net.lifenet.core.ui.fragments.SosFragment

class DashboardPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()          // 1. Ana Sayfa
            1 -> ConnectionsFragment()    // 2. Güvenlik (Temp: Connections)
            2 -> SecureChatFragment()     // 3. Mesajlar
            3 -> NearbyDevicesFragment()  // 4. Otonom (Temp: Nearby)
            4 -> SosFragment()           // 5. Ayarlar (Temp: SOS)
            else -> HomeFragment()
        }
    }
}
