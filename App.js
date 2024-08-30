import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { StatusBar } from "expo-status-bar";
import React from "react";
import CameraPage from "./CameraPage";
import FinalPage from "./FinalPage";
import HomePage from "./HomePage";

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <StatusBar style="auto" />
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen
          name="Home"
          component={HomePage}
          options={{
            title: "Mérőóra leolvasó Android Demo",
            statusBarStyle: "dark",
          }}
        />
        <Stack.Screen
          name="Camera"
          component={CameraPage}
          options={{
            headerShown: false,
          }}
        />
        <Stack.Screen
          name="Result"
          component={FinalPage}
          options={{
            title: "Eredmény",
            statusBarStyle: "dark",
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
