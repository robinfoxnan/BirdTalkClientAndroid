package com.bird2fish.birdtalkclient

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.bird2fish.birdtalksdk.net.WebSocketClient

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class FragmentArticles : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var webView:WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_articles, container, false)
        webView = root.findViewById<WebView>(R.id.wv_web)
        initWebView()

        return root
    }

    fun initWebView(){
        // 2. 核心配置 WebSettings（必加，保证页面正常加载）



        // 关键3：补充WebSettings配置，允许非安全连接（必须加，否则私有证书仍会加载失败）
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        // 允许WebView加载使用私有证书的HTTPS连接
        webSettings.allowFileAccess = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webSettings.loadWithOverviewMode = true // 自适应屏幕
        webSettings.useWideViewPort = true // 支持宽视图
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT // 缓存模式（默认：有缓存用缓存，无则联网）
        // 适配Android 9.0+ 明文HTTP（若你的私有证书地址是http，需加此配置，https则可选）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //webSettings.setAllowLoadingLocalResourcesFromRemoteUrls(true)
        }
        // 若运行在Android 7.0+，需在AndroidManifest.xml中添加配置（下文补充）

        // 3. 设置 WebViewClient（关键：避免跳转到系统浏览器）
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // 原有加载进度条逻辑保留
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 原有隐藏进度条逻辑保留
            }

            // 原有URL拦截逻辑：保留不变（解决net:Err_unknown_url_schema）
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    false
                } else {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        // Fragment中启动Activity需添加FLAG，避免上下文异常
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "暂无应用可打开此链接", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }

            // 关键1：重写Android 7.0+ 证书验证方法（主流版本适配）
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // 跳过私有证书认证：直接继续加载，不终止请求
                handler?.proceed()
                // 注意：不要调用 super.onReceivedSslError(view, handler, error)，否则会默认拒绝
            }

            // 关键2：重写低版本（Android 6.0及以下）证书验证方法（兼容旧设备）
//            @Suppress("DEPRECATION")
//            fun onReceivedSslError(
//                view: WebView?,
//                handler: SslErrorHandler?,
//                error: SslError?,
//                clientCertificateRequest: ClientCertificateRequest?
//            ) {
//                handler?.proceed()
//            }
        }



        // 4. 加载网络页面（核心方法，传入完整URL）
        var targetUrl = "https://www.baidu.com" // 示例：加载百度
        targetUrl = WebSocketClient.instance!!.getWelcomeUrl()
        webView.loadUrl(targetUrl)
    }

    // ========== 关键：重写生命周期方法，避免内存泄漏 ==========
    // 页面暂停时，暂停 WebView 加载
    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers() // 暂停所有WebView定时器（含JS定时器）
    }

    // 页面恢复时，恢复 WebView 加载
    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    // 页面销毁时，彻底释放 WebView 资源（必加，解决内存泄漏）
    override fun onDestroy() {
        // 1. 停止加载
        webView.stopLoading()
        // 2. 移除所有JS接口
        webView.removeJavascriptInterface("Android")
        // 3. 清空历史记录
        webView.clearHistory()
        // 4. 将 WebView 从父布局移除（关键）
        (webView.parent as android.view.ViewGroup).removeView(webView)
        // 5. 销毁 WebView
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentArticles().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}