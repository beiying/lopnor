package com.beiying.lopnor.base.recylerview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.io.Serializable

class NormalFragmentPagerAdapter(fragmentManager: FragmentManager, private var fragmentList: MutableList<NormalBean>, behavior: Int)
    : FragmentStatePagerAdapter(fragmentManager, behavior) {
    /**
     * AndroidX方式的懒加载
     * behavior需要设置BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT 则Fragment从不可见到可见将执行resume方法，Fragment不可见时，resume方法不执行
     *
     * @param fragmentManager
     * @param fragmentList
     * @param behavior      FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT 则Fragment从不可见到可见将执行resume方法，Fragment不可见时，resume方法不执行
     */
    constructor (fragmentManager: FragmentManager, fragmentList: MutableList<NormalBean>):this(fragmentManager, fragmentList, 0) {
    }

    override fun getItem(i: Int): Fragment {
        return fragmentList[i].getFragment()
    }

    override fun getCount(): Int {
        return fragmentList.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentList[position].title
    }

    class NormalBean(fragment: Fragment, title: String) : Serializable {
        private var fragment //Fragment
                : Fragment
        var title //标题
                : String

        fun getFragment(): Fragment {
            return fragment
        }

        fun setFragment(fragment: Fragment) {
            this.fragment = fragment
        }

        init {
            this.fragment = fragment
            this.title = title
        }
    }
}