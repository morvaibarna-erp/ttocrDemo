import { StatusBar } from "expo-status-bar";
import { StyleSheet, Text, View, NativeModules, Button } from "react-native";
import { MyView } from "./MyView";
import { PermissionsAndroid } from "react-native";
import React, { useEffect } from "react";

async function requestCameraPermission() {
  try {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.CAMERA,
      {
        title: "Camera Permission",
        message: "App needs camera access",
        buttonNeutral: "Ask Me Later",
        buttonNegative: "Cancel",
        buttonPositive: "OK",
      }
    );
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log("Camera permission granted");
    } else {
      console.log("Camera permission denied");
    }
  } catch (err) {
    console.warn(err);
  }
}

export default function App() {
  const onPress = () => {
    OCRModule.createOCREvent("testName");
  };
  useEffect(() => {
    requestCameraPermission();
  }, []);
  return (
    <View style={styles.container}>
      {/* <Text>Open up App.js to start working on your app!</Text>
      <StatusBar style="auto" />
      <Button
        title="Click to invoke your native module!"
        color="#841584"
        onPress={onPress}
      /> */}
      <MyView />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
    alignItems: "center",
    justifyContent: "center",
  },
});
