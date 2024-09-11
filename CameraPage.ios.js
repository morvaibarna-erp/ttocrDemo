import { View, Text, requireNativeComponent, StyleSheet } from "react-native";
import { React, useEffect } from "react";
import { request, check, PERMISSIONS, RESULTS } from "react-native-permissions";
import MyCustomView from "./CustomView.js";

async function requestPermission() {
  try {
    check(PERMISSIONS.IOS.CAMERA)
      .then((result) => {
        switch (result) {
          case RESULTS.UNAVAILABLE:
            console.log(
              "This feature is not available (on this device / in this context)"
            );
            break;
          case RESULTS.DENIED:
            console.log(
              "The permission has not been requested / is denied but requestable"
            );
            request(PERMISSIONS.IOS.CAMERA).then((result) => {
              console.log(result);
            });
            break;
          case RESULTS.LIMITED:
            console.log("The permission is limited: some actions are possible");
            break;
          case RESULTS.GRANTED:
            console.log("The permission is granted");
            break;
          case RESULTS.BLOCKED:
            console.log("The permission is denied and not requestable anymore");
            break;
        }
      })
      .catch((error) => {
        console.log(error);
      });
  } catch (err) {
    console.warn(err);
  }
}

const CameraPage = ({ navigation, route }) => {
  const { gyariSzam, heightRatio, widthRatio, ocrTimeOut } = route.params;
  const onCancelPress = () => {
    navigation.goBack();
  };
  const onSuccess = (event) => {
    navigation.navigate("Result", {
      allas: event.nativeEvent.meroErtek,
      savedImagePath: event.nativeEvent.filePath,
      gyariSzam: gyariSzam,
    });
  };
  useEffect(() => {
    requestPermission();
  }, []);

  return (
    <MyCustomView
      gyariSzam={gyariSzam}
      onCancelPress={onCancelPress}
      onSuccess={onSuccess}
    />
  );
};
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#F5FCFF",
  },
});
export default CameraPage;
