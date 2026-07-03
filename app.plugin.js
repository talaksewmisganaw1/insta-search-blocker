const { withAndroidManifest, withStringsXml } = require('@expo/config-plugins');

const withAccessibilityService = (config) => {
  config = withAndroidManifest(config, (config) => {
    const androidManifest = config.modResults;
    const application = androidManifest.manifest.application[0];

    // Add string resource for description (using a simple workaround for the description)
    // Actually better to use withStringsXml but for simplicity we will just put a hardcoded description in string XML later or use android:description="Instagram Search Blocker" (Note: it requires a reference to @string, so we need withStringsXml)

    if (!application.service) {
      application.service = [];
    }

    // Check if it already exists to avoid duplicates
    const serviceExists = application.service.some(
      (s) => s.$['android:name'] === 'expo.modules.instagramblocker.InstagramAccessibilityService'
    );

    if (!serviceExists) {
      application.service.push({
        $: {
          'android:name': 'expo.modules.instagramblocker.InstagramAccessibilityService',
          'android:permission': 'android.permission.BIND_ACCESSIBILITY_SERVICE',
          'android:exported': 'true'
        },
        'intent-filter': [
          {
            action: [
              {
                $: {
                  'android:name': 'android.accessibilityservice.AccessibilityService',
                },
              },
            ],
          },
        ],
        'meta-data': [
          {
            $: {
              'android:name': 'android.accessibilityservice',
              'android:resource': '@xml/accessibility_service_config',
            },
          },
        ],
      });
    }

    return config;
  });

  config = withStringsXml(config, (config) => {
    const strings = config.modResults.resources.string || [];
    
    const hasDescription = strings.some((s) => s.$.name === 'accessibility_service_description');
    if (!hasDescription) {
      strings.push({
        $: { name: 'accessibility_service_description' },
        _: 'Blocks search inside the Instagram app.',
      });
    }
    config.modResults.resources.string = strings;
    
    return config;
  });

  return config;
};

module.exports = withAccessibilityService;
