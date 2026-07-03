import { NativeModule, requireNativeModule } from 'expo';

declare class InstagramBlockerModule extends NativeModule<{}> {
  openAccessibilitySettings(): void;
}

export default requireNativeModule<InstagramBlockerModule>('InstagramBlocker');
