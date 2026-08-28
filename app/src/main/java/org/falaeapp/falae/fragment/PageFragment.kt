package org.falaeapp.falae.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import org.falaeapp.falae.R
import org.falaeapp.falae.adapter.ItemPagerAdapter
import org.falaeapp.falae.model.Page

class PageFragment : Fragment(), ViewPagerItemFragment.PageInteractionListener {

    private lateinit var mPageFragmentListener: PageFragmentListener
    private var mPager: ViewPager? = null
    private var leftNav: ImageView? = null
    private var rightNav: ImageView? = null
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_page, container, false)
        // A back-stack entry owns its page. The Activity's current page may be a
        // different page by the time this fragment's view is restored.
        val page = requireNotNull(requireArguments().getParcelable<Page>(PAGE_ARGUMENT))
        val leftNav = view.findViewById<ImageView>(R.id.left_nav)
        val rightNav = view.findViewById<ImageView>(R.id.right_nav)
        val leftNavHolder = view.findViewById<FrameLayout>(R.id.left_nav_holder)
        val rightNavHolder = view.findViewById<FrameLayout>(R.id.right_nav_holder)
        val pager = view.findViewById<ViewPager>(R.id.pager)
        this.leftNav = leftNav
        this.rightNav = rightNav
        mPager = pager

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                layoutListener = null
                // A restored Activity can replace this fragment before its first layout.
                if (this@PageFragment.view !== view) return
                val navHoldersSize = java.lang.Double.valueOf(pager.measuredWidth * 0.065).toInt()
                leftNav.layoutParams.width = navHoldersSize
                leftNav.layoutParams.height = navHoldersSize
                rightNav.layoutParams.width = navHoldersSize
                rightNav.layoutParams.height = navHoldersSize
                leftNavHolder.layoutParams.width = navHoldersSize
                rightNavHolder.layoutParams.width = navHoldersSize
                val pagerLayoutParams = pager.layoutParams as ViewGroup.MarginLayoutParams
                pagerLayoutParams.leftMargin += navHoldersSize
                pagerLayoutParams.rightMargin += navHoldersSize
                // Install once per view. ViewPager restores the adapter's state
                // against the child fragments retained by childFragmentManager.
                pager.adapter = ItemPagerAdapter(childFragmentManager, page, navHoldersSize * 2)
                pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                    override fun onPageSelected(position: Int) {
                        handleNavButtons()
                    }
                })
                handleNavButtons()
                leftNavHolder.setOnClickListener {
                    var tab = pager.currentItem
                    if (tab > 0) {
                        tab--
                        speak(getString(R.string.previous))
                        pager.currentItem = tab
                    } else if (tab == 0) {
                        pager.currentItem = tab
                    }
                }
                rightNavHolder.setOnClickListener {
                    var tab = pager.currentItem
                    val count = pager.adapter?.count ?: 0
                    if (count > 1 && tab != count - 1) {
                        speak(getString(R.string.next))
                    }
                    tab++
                    pager.currentItem = tab
                }
            }
        }
        layoutListener = listener
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        return view
    }

    override fun nextPage() {
        val pager = mPager ?: return
        val count = pager.adapter?.count ?: 0
        if (count > 1) {
            var currentItem = pager.currentItem
            if (currentItem == count - 1) {
                pager.currentItem = 0
            } else {
                pager.currentItem = ++currentItem
            }
        }
    }

    private fun handleNavButtons() {
        val pager = mPager ?: return
        val count = pager.adapter?.count ?: 0
        leftNav?.visibility = if (pager.currentItem > 0) View.VISIBLE else View.INVISIBLE
        rightNav?.visibility = if (pager.currentItem < count - 1) View.VISIBLE else View.INVISIBLE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PageFragmentListener) {
            mPageFragmentListener = context
        } else {
            throw RuntimeException("$context must implement PageFragmentListener")
        }
    }

    override fun onDestroyView() {
        layoutListener?.let { listener ->
            view?.viewTreeObserver?.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(listener)
        }
        layoutListener = null
        mPager?.clearOnPageChangeListeners()
        // Do not set adapter=null here: FragmentStatePagerAdapter would remove
        // children still referenced by the saved ViewPager state (keys f0, f1...).
        // FragmentManager owns their lifecycle; drop only our view references.
        mPager = null
        leftNav = null
        rightNav = null
        super.onDestroyView()
    }

    fun speak(msg: String) {
        mPageFragmentListener.speak(msg)
    }

    interface PageFragmentListener {
        fun speak(msg: String)
    }

    companion object {
        private const val PAGE_ARGUMENT = "page"

        fun newInstance(page: Page): PageFragment {
            return PageFragment().apply {
                arguments = Bundle().apply { putParcelable(PAGE_ARGUMENT, page) }
            }
        }
    }
}
