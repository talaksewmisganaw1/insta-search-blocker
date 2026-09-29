package expo.modules.instagramblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InstagramAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "InstaBlockerService"
        private const val TARGET_PACKAGE = "com.instagram.android"
        private const val COOLDOWN_MS = 500L
        private const val MAX_SEARCH_DEPTH = 60
    }

    private var lastBlockTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected")

        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            info.notificationTimeout = 50
            serviceInfo = info
            Log.d(TAG, "Accessibility service info configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring service info", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg.isNotEmpty() && pkg != TARGET_PACKAGE) {
            return
        }

        // 1. Direct event check: when a search button/tab/bar is clicked, focused, or typed in
        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            event.source?.let { source ->
                if (isDirectSearchInteraction(source, eventType)) {
                    triggerBlock("Direct interaction (${AccessibilityEvent.eventTypeToString(eventType)})")
                    return
                }
            }
        }

        // 2. Active window state check
        val rootNode = rootInActiveWindow ?: event.source ?: return
        if (findSearchInTree(rootNode, depth = 0)) {
            triggerBlock("Search UI detected in active window hierarchy")
        }
    }

    private fun triggerBlock(reason: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastBlockTime < COOLDOWN_MS) {
            return
        }
        lastBlockTime = now
        Log.d(TAG, "Search detected [$reason]. Triggering BACK...")
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Checks if a single node targeted by an event (click/focus/typing) is an intentional search interaction.
     */
    private fun isDirectSearchInteraction(node: AccessibilityNodeInfo, eventType: Int): Boolean {
        val text = node.text?.toString() ?: ""
        val hint = node.hintText?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""

        // Case A: User clicked the Search/Explore tab at the bottom navigation bar or clicked search/Meta AI
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (isSearchOrExploreTab(desc, viewId)) {
                return true
            }
            if (containsSearchOrAiKeyword(text) ||
                containsSearchOrAiKeyword(hint) ||
                containsSearchOrAiKeyword(desc) ||
                containsSearchOrAiKeyword(viewId)
            ) {
                return true
            }
        }

        // Case B: User focused on or typed into a search / Meta AI field
        if (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            node.isFocused
        ) {
            if (isSearchInputField(node, text, hint, desc, viewId, className)) {
                return true
            }
        }

        return false
    }

    /**
     * Recursively traverses the node hierarchy looking for:
     * 1. The Search/Explore tab being actively selected in the bottom navigation
     * 2. Active / focused search fields (Meta AI or standard search)
     * 3. Dedicated search screen / fragment components
     * 4. Explore search headers
     */
    private fun findSearchInTree(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > MAX_SEARCH_DEPTH) return false

        val text = node.text?.toString() ?: ""
        val hint = node.hintText?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""

        // Case 1: Search / Explore bottom tab is actively selected
        if (isTabSelected(node) && isSearchOrExploreTab(desc, viewId)) {
            return true
        }

        // Case 2: An input field is currently focused and relates to search or Meta AI
        if (node.isFocused && isSearchInputField(node, text, hint, desc, viewId, className)) {
            return true
        }

        // Case 3: Dedicated Search screen / Search fragment components
        if (isDedicatedSearchScreen(text, desc, viewId, className)) {
            return true
        }

        // Case 4: The top Explore search header in the updated Instagram
        if (isExploreSearchHeader(node, text, hint, desc, viewId, className)) {
            return true
        }

        // Check children
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            if (findSearchInTree(child, depth + 1)) {
                return true
            }
        }

        return false
    }

    private fun isTabSelected(node: AccessibilityNodeInfo): Boolean {
        if (node.isSelected) return true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val stateDesc = node.stateDescription?.toString()?.lowercase() ?: ""
                if (stateDesc.contains("selected")) return true
            } catch (_: Throwable) {}
        }
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        return contentDesc.contains("selected")
    }

    private fun isSearchOrExploreTab(desc: String, viewId: String): Boolean {
        val d = desc.lowercase()
        val v = viewId.lowercase()
        val isTab = v.contains("tab") || d.contains("tab") || d.contains("explore") || d.contains("search and explore")
        return isTab && (containsSearchOrAiKeyword(d) || containsSearchOrAiKeyword(v) || d.contains("explore") || v.contains("explore"))
    }

    private fun isSearchInputField(
        node: AccessibilityNodeInfo,
        text: String,
        hint: String,
        desc: String,
        viewId: String,
        className: String
    ): Boolean {
        val isEditableOrText = node.isEditable ||
                className.contains("EditText", ignoreCase = true) ||
                className.contains("AutoCompleteTextView", ignoreCase = true) ||
                className.contains("SearchEditText", ignoreCase = true) ||
                className.contains("TextView", ignoreCase = true) ||
                className.contains("View", ignoreCase = true)

        if (!isEditableOrText) return false

        return containsSearchOrAiKeyword(text) ||
                containsSearchOrAiKeyword(hint) ||
                containsSearchOrAiKeyword(desc) ||
                containsSearchOrAiKeyword(viewId) ||
                className.contains("SearchEditText", ignoreCase = true)
    }

    private fun isDedicatedSearchScreen(text: String, desc: String, viewId: String, className: String): Boolean {
        val t = text.lowercase()
        val d = desc.lowercase()
        val v = viewId.lowercase()

        // In Instagram search page: "Recent searches", "Recent", "Clear all"
        if (t == "recent searches" || d == "recent searches" || (t == "recent" && v.contains("search"))) {
            return true
        }

        // Specific search result tab view IDs or classes
        if (className.contains("SearchEditText", ignoreCase = true)) {
            return true
        }

        return false
    }

    private fun isExploreSearchHeader(
        node: AccessibilityNodeInfo,
        text: String,
        hint: String,
        desc: String,
        viewId: String,
        className: String
    ): Boolean {
        val v = viewId.lowercase()
        // Top search bar IDs in Explore
        if (v.contains("search_flat_bar") || v.contains("echo_search_bar") || v.contains("meta_ai_search")) {
            return true
        }

        // Header search button / container with search or Meta AI
        if (node.isClickable) {
            val d = desc.lowercase()
            val t = text.lowercase()
            val h = hint.lowercase()
            if ((d.contains("search") || t.contains("search") || h.contains("search") ||
                 d.contains("meta ai") || t.contains("meta ai") || h.contains("meta ai")) &&
                (v.contains("action_bar") || v.contains("header") || v.contains("search") || v.contains("title"))
            ) {
                return true
            }
        }

        return false
    }

    private fun containsSearchOrAiKeyword(value: String): Boolean {
        if (value.isEmpty()) return false
        val s = value.lowercase()
        return s.contains("search") ||
                s.contains("meta ai") ||
                s.contains("meta_ai") ||
                s.contains("metaai") ||
                s.contains("ask meta") ||
                s.contains("ask anything") ||
                s.contains("imagine") ||
                s.contains("find") ||
                s.contains("rechercher") ||
                s.contains("buscar") ||
                s.contains("suche") ||
                s.contains("cerca") ||
                s.contains("ፈልግ")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }
}
