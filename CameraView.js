import React, { useEffect, useRef, useCallback } from "react";
import {
  PixelRatio,
  UIManager,
  findNodeHandle,
  Dimensions,
  SafeAreaView,
  StyleSheet,
  View,
} from "react-native";

import {
  requireNativeComponent,
  NativeEventEmitter,
  NativeModules,
} from "react-native";

export const OCRView = requireNativeComponent("OCRViewManager");

export function CameraView(props) {
  return (
    <OCRView
      style={styles.camera}
      expectedBarCode={props.gyariSzam}
      heightRatio={props.heightRatio}
      widthRatio={props.widthRatio}
    />
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  cameraContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    width: Dimensions.get("window").width,
    height: Dimensions.get("window").height,
  },
  camera: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    width: Dimensions.get("window").width,
    height: Dimensions.get("window").height,
  },
});
