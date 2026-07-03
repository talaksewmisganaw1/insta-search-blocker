package expo.modules.instagramblocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InstagramAccessibilityService : AccessibilityService() {

    private val TAG = "InstaBlockerService"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.packageName?.toString() == "com.instagram.android") {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                if (findSearchNodes(rootNode)) {
                    Log.d(TAG, "Search UI detected. Blocking...")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
        }
    }

    private fun findSearchNodes(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // Look for EditText (search bar) or nodes that indicate a search input
        if (node.className == "android.widget.EditText") {
            val text = node.text?.toString()?.lowercase() ?: ""
            val hint = node.hintText?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName ?: ""

            // Simple heuristic to identify search bar in Instagram
            if (text.contains("search") || hint.contains("search") || contentDesc.contains("search") || viewId.contains("search")) {
                return true
            }
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            if (findSearchNodes(node.getChild(i))) {
                return true
            }
        }

        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }
}
