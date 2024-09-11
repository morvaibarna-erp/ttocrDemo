import React from "react";
import { requireNativeComponent, View, Text, StyleSheet } from "react-native";

const RCTCustomView = requireNativeComponent("OCRView");

export default function CustomView({
  gyariSzam,
  onCancelPress,
  onSuccess,
  navigation,
}) {
  return (
    <View style={styles.container}>
      <RCTCustomView
        style={styles.wrapper}
        gyariSzam={gyariSzam}
        onCancelPress={(e) => {
          onCancelPress();
        }}
        onSuccess={(e) => {
          onSuccess(e);
        }}
      />
    </View>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: "stretch",
  },
  wrapper: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
});
