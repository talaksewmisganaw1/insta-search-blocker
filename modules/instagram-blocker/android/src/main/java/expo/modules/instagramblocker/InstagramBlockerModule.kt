package expo.modules.instagramblocker

import android.content.Intent
import android.provider.Settings
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class InstagramBlockerModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("InstagramBlocker")

    Function("openAccessibilitySettings") {
      val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      appContext.reactContext?.startActivity(intent)
    }
  }
}
