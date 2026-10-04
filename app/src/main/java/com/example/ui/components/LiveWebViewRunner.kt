package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ConsoleLogItem
import com.example.data.model.LogLevel

class WebAppBridge(private val onLog: (ConsoleLogItem) -> Unit) {
    @JavascriptInterface
    fun postConsoleLog(level: String, msg: String) {
        val l = when (level.uppercase()) {
            "ERROR" -> LogLevel.ERROR
            "WARN" -> LogLevel.WARNING
            "DEBUG" -> LogLevel.DEBUG
            else -> LogLevel.INFO
        }
        onLog(ConsoleLogItem(message = msg, level = l))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveWebViewRunner(
    htmlData: String,
    filesMap: Map<String, String>,
    reloadTrigger: Int,
    onConsoleLog: (ConsoleLogItem) -> Unit,
    onPageLoaded: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bridge = remember(onConsoleLog) { WebAppBridge(onConsoleLog) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadsImagesAutomatically = true
                        mediaPlaybackRequiresUserGesture = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }

                    addJavascriptInterface(bridge, "AndroidBridge")

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                val level = when (it.messageLevel()) {
                                    ConsoleMessage.MessageLevel.ERROR -> LogLevel.ERROR
                                    ConsoleMessage.MessageLevel.WARNING -> LogLevel.WARNING
                                    ConsoleMessage.MessageLevel.DEBUG -> LogLevel.DEBUG
                                    else -> LogLevel.INFO
                                }
                                onConsoleLog(
                                    ConsoleLogItem(
                                        message = it.message() ?: "",
                                        level = level,
                                        sourceId = it.sourceId(),
                                        lineNumber = it.lineNumber()
                                    )
                                )
                            }
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            onPageLoaded()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                onError(error?.description?.toString() ?: "WebView loading error")
                            }
                        }
                    }

                    loadDataWithBaseURL("https://appassets.local/", htmlData, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                // Reload when htmlData or reloadTrigger updates
                webView.loadDataWithBaseURL("https://appassets.local/", htmlData, "text/html", "UTF-8", null)
            }
        )
    }
}
