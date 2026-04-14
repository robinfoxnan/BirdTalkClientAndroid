package com.bird2fish.birdtalksdk.ui
import androidx.fragment.app.Fragment

open class EmbedFrame : Fragment() {

    // ==================== 自定义显示/隐藏回调 ====================
    /**
     * 当 Fragment 对用户可见时调用
     */
    open fun onShow() {
        // 子类重写实现逻辑
    }

    /**
     * 当 Fragment 对用户不可见时调用
     */
    open fun onHide() {
        // 子类重写实现逻辑
    }

    // ==================== 系统生命周期监听 ====================
    override fun onResume() {
        super.onResume()
        // 页面显示 → 触发 onShow
        onShow()
    }

    override fun onPause() {
        super.onPause()
        // 页面隐藏 → 触发 onHide
        onHide()
    }

    // 可选：如果你是用 replace / add 切换 Fragment，用这个更精准
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            onHide()
        } else {
            onShow()
        }
    }
}