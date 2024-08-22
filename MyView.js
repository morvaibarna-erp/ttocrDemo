import React, { useEffect, useRef } from "react";
import {
  PixelRatio,
  UIManager,
  findNodeHandle,
  Dimensions,
  SafeAreaView,
  StyleSheet,
  View,
} from "react-native";
import PropTypes from "prop-types";
import { requireNativeComponent } from "react-native";
var viewProps = {
  name: "OCRViewManager",
  propTypes: {
    expectedBarCode: PropTypes.string,
    heightRatio: PropTypes.int,
    widthRatio: PropTypes.int,
  },
};
export const CameraView = requireNativeComponent("OCRViewManager");

// const createFragment = (viewId) =>
//   UIManager.dispatchViewManagerCommand(
//     viewId,
//     // we are calling the 'create' command
//     UIManager.OCRViewManager.Commands.create.toString(),
//     [viewId]
//   );

export const MyView = () => {
  // const ref = useRef(null);

  // useEffect(() => {
  //   const viewId = findNodeHandle(ref.current);
  //   createFragment(viewId);
  // }, []);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.cameraContainer}>
        <CameraView
          style={styles.camera}
          expectedBarCode={"640011314609186"}
          heightRatio={1}
          widthRatio={1}
        />
      </View>
    </SafeAreaView>
  );
};

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
    width: Dimensions.get("window").width,
    height: Dimensions.get("window").height,
  },
});
