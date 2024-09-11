import {
  View,
  Text,
  NativeModules,
  PermissionsAndroid,
  NativeEventEmitter,
  StatusBar,
} from "react-native";
import { React, useEffect } from "react";
import { CameraView } from "./CameraView";

const nativeEventEmitter = new NativeEventEmitter(NativeModules.OCRViewManager);

async function requestPermission() {
  try {
    await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.CAMERA);
  } catch (err) {
    console.warn(err);
  }
}

const CameraPage = ({ navigation, route }) => {
  const { gyariSzam, heightRatio, widthRatio, ocrTimeOut } = route.params;

  useEffect(() => {
    const subscription = nativeEventEmitter.addListener(
      "displayHit",
      (event) => {
        navigation.navigate("Result", {
          allas: event.allas,
          savedImagePath: event.savedImagePath,
          gyariSzam: gyariSzam,
        });
      }
    );
    const subscription2 = nativeEventEmitter.addListener(
      "NoBarCode",
      (event) => {
        navigation.goBack();
      }
    );

    return () => {
      subscription.remove();
      subscription2.remove();
    };
  }, []);

  useEffect(() => {
    requestPermission();
  }, []);

  return (
    <View
      style={{
        flex: 1,
        justifyContent: "center",
        alignItems: "center",
        backgroundColor: "black",
      }}
    >
      {/* <StatusBar barStyle={"light-content"} style="dark" /> */}

      <CameraView
        gyariSzam={gyariSzam}
        heightRatio={heightRatio}
        widthRatio={widthRatio}
        ocrTimeOut={ocrTimeOut}
      />
    </View>
  );
};

export default CameraPage;
