import {
  View,
  StyleSheet,
  Text,
  StatusBar,
  Button,
  TextInput,
} from "react-native";
import React, { useEffect, useState } from "react";

const HomePage = ({ navigation }) => {
  const [gyariSzam, setGyariSzam] = useState("440011700485475");
  const [heightRatio, setHeightRatio] = useState("1");
  const [widthRatio, setWidthRatio] = useState("1");
  const [ocrTimeOut, setOcrTimeOut] = useState("10");

  return (
    <View style={styles.container}>
      {/* <StatusBar barStyle={"dark-content"} backgroundColor={"white"} /> */}
      <View style={{ alignItems: "center" }}>
        <Text>Gyári szám:</Text>
        <TextInput
          style={styles.input}
          onChangeText={setGyariSzam}
          value={gyariSzam}
          keyboardType="numeric"
        />
        <Text>Magasság aránya</Text>
        <TextInput
          style={styles.input}
          onChangeText={setHeightRatio}
          value={heightRatio}
          keyboardType="numeric"
        />
        <Text>Szélesség aránya</Text>
        <TextInput
          style={styles.input}
          onChangeText={setWidthRatio}
          value={widthRatio}
          keyboardType="numeric"
        />
        <Text>Gyári szám leolvasás ideje (másodperc)</Text>
        <TextInput
          style={styles.input}
          onChangeText={setOcrTimeOut}
          value={ocrTimeOut}
          keyboardType="numeric"
        />
      </View>
      <StatusBar style="auto" />
      <Button
        title="Leolvasás"
        color="#0191ca"
        onPress={() => {
          if (gyariSzam != "") {
            navigation.navigate("Camera", {
              gyariSzam: gyariSzam,
              heightRatio: parseInt(heightRatio),
              widthRatio: parseInt(widthRatio),
              ocrTimeOut: parseInt(ocrTimeOut),
            });
          }
        }}
      />
    </View>
  );
};
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
    alignItems: "center",
    justifyContent: "space-evenly",
  },
  input: {
    height: 40,
    margin: 12,
    borderWidth: 1,
    padding: 10,
  },
});

export default HomePage;
