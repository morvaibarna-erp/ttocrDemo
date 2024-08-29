import { StyleSheet, View } from "react-native";
import React, { useEffect } from "react";
import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import HomePage from "./HomePage";
import CameraPage from "./CameraPage";
import FinalPage from "./FinalPage";
import { StatusBar } from "expo-status-bar";

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <StatusBar />

      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen
          name="Home"
          component={HomePage}
          options={{ title: "Mérőóra leolvasó Android Demo" }}
        />
        <Stack.Screen
          name="Camera"
          component={CameraPage}
          options={{ headerShown: false }}
        />
        <Stack.Screen
          name="Result"
          component={FinalPage}
          options={{ title: "Eredmény" }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
