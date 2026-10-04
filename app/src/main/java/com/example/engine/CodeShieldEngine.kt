package com.example.engine

import android.util.Base64
import java.util.regex.Pattern

object CodeShieldEngine {

    /**
     * Sanitizes and wraps HTML files to ensure they execute safely on Android WebView
     * without blank-screen crashes, missing viewports, or uncaught JS errors.
     */
    fun shieldHtmlContent(
        rawHtml: String,
        appName: String,
        files: Map<String, String> = emptyMap(),
        injectConsoleBridge: Boolean = true
    ): String {
        var content = rawHtml.trim()

        // 1. If content is completely empty or just plain text / script / style
        if (content.isEmpty()) {
            content = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>$appName</title>
                    <style>
                        body { font-family: sans-serif; background: #0f172a; color: #fff; padding: 24px; text-align: center; }
                        h2 { color: #38bdf8; margin-top: 40px; }
                        p { color: #94a3b8; }
                    </style>
                </head>
                <body>
                    <h2>$appName</h2>
                    <p>App running smoothly. Add your content to index.html!</p>
                </body>
                </html>
            """.trimIndent()
        }

        // 2. Ensure DOCTYPE
        if (!content.contains("<!DOCTYPE", ignoreCase = true) && !content.contains("<html", ignoreCase = true)) {
            content = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <title>$appName</title>
                </head>
                <body>
                    $content
                </body>
                </html>
            """.trimIndent()
        }

        // 3. Ensure Viewport Meta tag
        if (!content.contains("name=\"viewport\"", ignoreCase = true) && !content.contains("name='viewport'", ignoreCase = true)) {
            val viewportTag = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n"
            content = if (content.contains("<head>", ignoreCase = true)) {
                content.replaceFirst("(?i)<head>".toRegex(), "<head>\n$viewportTag")
            } else if (content.contains("<html>", ignoreCase = true)) {
                content.replaceFirst("(?i)<html>".toRegex(), "<html><head>$viewportTag</head>")
            } else {
                viewportTag + content
            }
        }

        // 4. Inject Error Shield & Console Bridge
        val shieldScript = buildShieldScript(injectConsoleBridge)
        content = if (content.contains("<head>", ignoreCase = true)) {
            content.replaceFirst("(?i)<head>".toRegex(), "<head>\n$shieldScript")
        } else if (content.contains("<body>", ignoreCase = true)) {
            content.replaceFirst("(?i)<body>".toRegex(), "<body>\n$shieldScript")
        } else {
            shieldScript + content
        }

        return content
    }

    private fun buildShieldScript(enableBridge: Boolean): String {
        return """
        <script id="__apk_maker_shield__">
        (function() {
            // Error Shield & Safe Catcher
            window.addEventListener('error', function(e) {
                console.error("[Safe Shield Catch] " + (e.message || "Unknown error") + " at " + (e.filename || "") + ":" + (e.lineno || ""));
                showVisualErrorBanner(e.message, e.filename, e.lineno);
            });

            window.addEventListener('unhandledrejection', function(e) {
                console.error("[Safe Shield Promise Rejection] " + (e.reason ? (e.reason.message || e.reason) : "Rejected"));
            });

            function showVisualErrorBanner(msg, file, line) {
                try {
                    let banner = document.getElementById('__apk_shield_banner__');
                    if (!banner) {
                        banner = document.createElement('div');
                        banner.id = '__apk_shield_banner__';
                        banner.style.cssText = 'position:fixed;bottom:10px;left:10px;right:10px;background:#ef4444;color:white;padding:10px 14px;border-radius:10px;font-size:12px;font-family:sans-serif;z-index:999999;box-shadow:0 4px 14px rgba(0,0,0,0.5);display:flex;justify-content:space-between;align-items:center;';
                        const text = document.createElement('span');
                        text.innerText = '⚠️ Error Handled: ' + (msg || 'Script issue');
                        const close = document.createElement('button');
                        close.innerText = '✕';
                        close.style.cssText = 'background:none;border:none;color:white;font-weight:bold;margin-left:8px;font-size:14px;cursor:pointer;';
                        close.onclick = function() { banner.remove(); };
                        banner.appendChild(text);
                        banner.appendChild(close);
                        if (document.body) document.body.appendChild(banner);
                    }
                } catch(err) {}
            }

            // Bridge console messages to Android if available
            ${if (enableBridge) """
            const origLog = console.log;
            const origWarn = console.warn;
            const origError = console.error;

            console.log = function() {
                origLog.apply(console, arguments);
                try {
                    if (window.AndroidBridge && window.AndroidBridge.postConsoleLog) {
                        const msg = Array.from(arguments).map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
                        window.AndroidBridge.postConsoleLog('INFO', msg);
                    }
                } catch(e) {}
            };

            console.warn = function() {
                origWarn.apply(console, arguments);
                try {
                    if (window.AndroidBridge && window.AndroidBridge.postConsoleLog) {
                        const msg = Array.from(arguments).map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
                        window.AndroidBridge.postConsoleLog('WARN', msg);
                    }
                } catch(e) {}
            };

            console.error = function() {
                origError.apply(console, arguments);
                try {
                    if (window.AndroidBridge && window.AndroidBridge.postConsoleLog) {
                        const msg = Array.from(arguments).map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
                        window.AndroidBridge.postConsoleLog('ERROR', msg);
                    }
                } catch(e) {}
            };
            """ else ""}
        })();
        </script>
        """.trimIndent()
    }

    /**
     * Bundle all referenced CSS and JS files into a single offline self-contained HTML file.
     * "creat app to html file"
     */
    fun bundleToSingleHtml(mainHtml: String, files: Map<String, String>): String {
        var bundled = mainHtml

        // Replace <link rel="stylesheet" href="..."> with inline <style>...</style>
        val cssLinkPattern = Pattern.compile("<link[^>]+rel=[\"']stylesheet[\"'][^>]+href=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE)
        val cssMatcher = cssLinkPattern.matcher(bundled)
        val cssSb = StringBuffer()
        while (cssMatcher.find()) {
            val href = cssMatcher.group(1) ?: ""
            val cleanHref = href.removePrefix("./").removePrefix("/")
            val cssContent = files[cleanHref] ?: files[cleanHref.substringAfterLast("/")]
            if (cssContent != null) {
                cssMatcher.appendReplacement(cssSb, "<style>/* Inlined $cleanHref */\n" + MatcherReplacementFix(cssContent) + "\n</style>")
            } else {
                cssMatcher.appendReplacement(cssSb, cssMatcher.group(0) ?: "")
            }
        }
        cssMatcher.appendTail(cssSb)
        bundled = cssSb.toString()

        // Replace <script src="..."></script> with inline <script>...</script>
        val scriptPattern = Pattern.compile("<script[^>]+src=[\"']([^\"']+)[\"'][^>]*>\\s*</script>", Pattern.CASE_INSENSITIVE)
        val scriptMatcher = scriptPattern.matcher(bundled)
        val scriptSb = StringBuffer()
        while (scriptMatcher.find()) {
            val src = scriptMatcher.group(1) ?: ""
            val cleanSrc = src.removePrefix("./").removePrefix("/")
            val jsContent = files[cleanSrc] ?: files[cleanSrc.substringAfterLast("/")]
            if (jsContent != null) {
                scriptMatcher.appendReplacement(scriptSb, "<script>/* Inlined $cleanSrc */\n" + MatcherReplacementFix(jsContent) + "\n</script>")
            } else {
                scriptMatcher.appendReplacement(scriptSb, scriptMatcher.group(0) ?: "")
            }
        }
        scriptMatcher.appendTail(scriptSb)
        bundled = scriptSb.toString()

        return bundled
    }

    private fun MatcherReplacementFix(str: String): String {
        return str.replace("\\", "\\\\").replace("$", "\\$")
    }
}
