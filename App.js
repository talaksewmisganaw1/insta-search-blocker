import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View, Button, Alert } from 'react-native';
import InstagramBlockerModule from './modules/instagram-blocker/src/InstagramBlockerModule';

export default function App() {
  const openSettings = () => {
    try {
      InstagramBlockerModule.openAccessibilitySettings();
    } catch (e) {
      Alert.alert("Error", "Could not open settings. Are you on Android?");
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Instagram Search Blocker</Text>
      <Text style={styles.description}>
        This app uses an Android Accessibility Service to detect when you are trying to use the search bar in Instagram and blocks it.
      </Text>
      <Text style={styles.instruction}>
        1. Tap the button below to open Accessibility Settings.
        {'\n'}2. Find "Instagram Search Blocker" (or your app name) in the downloaded services list.
        {'\n'}3. Enable the service.
      </Text>
      <Button title="Open Accessibility Settings" onPress={openSettings} />
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  description: {
    textAlign: 'center',
    marginBottom: 20,
    fontSize: 16,
  },
  instruction: {
    textAlign: 'left',
    marginBottom: 30,
    fontSize: 14,
    lineHeight: 22,
    color: '#333'
  }
});
