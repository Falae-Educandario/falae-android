package org.falaeapp.falae.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet

class DisplayViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var currentSpreadSheet: SpreadSheet

    private val linkToPage: MutableLiveData<String> = MutableLiveData()
    val pageToOpen: LiveData<PageNavigation> = linkToPage.switchMap { linkTo ->
        liveData(Dispatchers.Default) {
            val page = currentSpreadSheet.pages.find { it.name == linkTo }
            page?.apply {
                initialPage = isInitialPage(this)
                emit(PageNavigation(this))
            }
        }
    }

    private val newPage: MutableLiveData<Page> = MutableLiveData()
    val currentPage: LiveData<Page> = newPage.switchMap { page ->
        liveData {
            emit(page)
        }
    }

    fun init(spreadSheet: SpreadSheet, openInitialPage: Boolean = true) {
        currentSpreadSheet = spreadSheet
        if (openInitialPage) {
            currentSpreadSheet.initialPage?.let { openPage(it) }
        }
    }

    fun openPage(linkTo: String) {
        linkToPage.value = linkTo
    }

    private fun isInitialPage(page: Page) = page.name == currentSpreadSheet.initialPage

    fun setCurrentPage(page: Page) {
        newPage.value = page
    }

    // LiveData replays its last value when an Activity is recreated. A handled
    // navigation must not replace the fragments/back stack Android just restored.
    class PageNavigation(private val page: Page) {
        private var handled = false

        fun consume(): Page? {
            if (handled) return null
            handled = true
            return page
        }
    }
}
