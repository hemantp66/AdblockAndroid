package io.github.edsuns.adblockclient.sample.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.github.edsuns.adblockclient.sample.R
import io.github.edsuns.adblockclient.sample.databinding.ActivityMainBinding
import io.github.edsuns.adblockclient.sample.hideKeyboard
import io.github.edsuns.adblockclient.sample.main.blocking.BlockingInfoDialogFragment
import io.github.edsuns.adblockclient.sample.settings.SettingsActivity
import io.github.edsuns.adblockclient.sample.smartUrlFilter
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.FilterViewModel
import io.github.edsuns.adfilter.MatchedRule
import io.github.edsuns.smoothprogress.SmoothProgressAnimator

class MainActivity : AppCompatActivity(), WebViewClientListener {

    private lateinit var filterViewModel: FilterViewModel

    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var progressAnimator: SmoothProgressAnimator

    private lateinit var blockingInfoDialogFragment: BlockingInfoDialogFragment

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filterViewModel = AdFilter.get().viewModel

        val popupMenu = PopupMenu(
            this,
            binding.menuButton,
            Gravity.NO_GRAVITY,
            R.attr.actionOverflowMenuStyle,
            0
        )
        popupMenu.inflate(R.menu.menu_main)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuRefresh -> webView.reload()
                R.id.menuForward -> webView.goForward()
                R.id.menuSettings ->
                    startActivity(Intent(this, SettingsActivity::class.java))
                else -> finish()
            }
            true
        }
        val menuForward = popupMenu.menu.findItem(R.id.menuForward)

        binding.menuButton.setOnClickListener {
            menuForward.isVisible = webView.canGoForward()
            popupMenu.show()
        }

        blockingInfoDialogFragment = BlockingInfoDialogFragment.newInstance()

        binding.countText.setOnClickListener {
            if (filterViewModel.isEnabled.value == true) {
                blockingInfoDialogFragment.show(supportFragmentManager, null)
            } else {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        webView = binding.webView
        webView.webViewClient = WebClient(this)
        webView.webChromeClient = ChromeClient(this)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Zooms out the content to fit on screen by width. For example, showing images.
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        AdFilter.get().setupWebView(webView)

        progressAnimator = SmoothProgressAnimator(binding.loadProgress)

        val urlText = binding.urlEditText
        urlText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO
                || event.keyCode == KeyEvent.KEYCODE_ENTER
                && event.action == KeyEvent.ACTION_DOWN
            ) {
                val urlIn = urlText.text.toString()
                webView.loadUrl(
                    urlIn.smartUrlFilter() ?: URLUtil.composeSearchUrl(
                        urlIn,
                        "https://www.bing.com/search?q={}",
                        "{}"
                    )
                )
                webView.requestFocus()
                urlText.hideKeyboard()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        filterViewModel.isEnabled.observe(this, {
            binding.countText.text =
                if (it) getString(R.string.count_none) else getString(R.string.off)
        })

        viewModel.blockingInfoMap.observe(this, { updateBlockedCount() })
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {
        runOnUiThread {
            url?.let { viewModel.currentPageUrl = it }
            updateBlockedCount()
        }
    }

    override fun progressChanged(newProgress: Int) {
        runOnUiThread {
            webView.url?.let { viewModel.currentPageUrl = it }
            progressAnimator.progress = newProgress
            binding.urlEditText.setText(webView.url)
            if (newProgress == 10) {
                updateBlockedCount()
            }
        }
    }

    private fun updateBlockedCount() {
        if (filterViewModel.isEnabled.value == true) {
            val blockedUrlMap =
                viewModel.blockingInfoMap.value?.get(viewModel.currentPageUrl)?.blockedUrlMap
            binding.countText.text = (blockedUrlMap?.size ?: 0).toString()
        }
    }

    override fun onShouldInterceptRequest(rule: MatchedRule) {
        viewModel.logRequest(rule)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        super.onBackPressed()
    }
}