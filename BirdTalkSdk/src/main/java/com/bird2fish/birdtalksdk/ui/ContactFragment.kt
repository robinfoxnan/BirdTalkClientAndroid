package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bird2fish.birdtalksdk.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

class ContactFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_contact, container, false)

        val view = inflater.inflate(R.layout.fragment_contact_sdk, container, false)

        val tabLayout1: TabLayout? = view.findViewById(R.id.contact_tab)
        if (tabLayout1 == null) {
            Log.e("ContactFragment", "TabLayout is null")
        } else {
            Log.d("ContactFragment", "TabLayout found")
        }

        this.tabLayout = tabLayout1!!
        viewPager = view.findViewById(R.id.contact_main)

        // 设置 ViewPager2 的适配器
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // 使用 TabLayoutMediator 关联 TabLayout 和 ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.contact_followed) //"互相关注"
                1 -> getString(R.string.contact_following) //"关注"
                2 -> getString(R.string.contact_fans) //"粉丝"
                3-> getString(R.string.contact_recommend) //"推荐"
                else -> getString(R.string.contact_search) //"推荐"
            }
        }.attach()

        return view
    }

    class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 5  // Tab 数量

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FollowedFragment()      // 自定义 Fragment
                1 -> FollowingFragment()     // 自定义 Fragment
                2 -> FansFragment()          // 自定义 Fragment
                3-> RecommendedFragment()  // 自定义 Fragment
                else -> SearchFriendFragment()
            }
        }
    }


}