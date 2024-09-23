import {
  View,
  Text,
  StyleSheet,
  Image,
  TextInput,
  Button,
  ScrollView,
} from "react-native";
import { React, useState } from "react";
// var RNFS = require("react-native-fs");

// const deleteImage = (path) => {
//   const filepath = path;

//   RNFS.exists(filepath)
//     .then((result) => {
//       if (result) {
//         return RNFS.unlink(filepath).catch((err) => {
//           console.log(err.message);
//         });
//       }
//     })
//     .catch((err) => {
//       console.log(err.message);
//     });
// };

const FinalPage = ({ route, navigation }) => {
  const { allas, gyariSzam, savedImagePath } = route.params;
  const [kwh, setKwh] = useState(allas);
  console.log(savedImagePath);
  return (
    <View style={styles.container}>
      <ScrollView>
        <View style={styles.container}>
          <Image
            source={{ uri: "file://" + savedImagePath }}
            style={styles.image}
          />
          <View style={styles.text}>
            <Text>Gyári szám:</Text>
            <Text>{gyariSzam}</Text>
          </View>
          <View style={styles.text}>
            <Text>Mérőóra leolvasott állása (kWh):</Text>
            <TextInput
              style={styles.textInput}
              value={kwh}
              onChangeText={setKwh}
            />
          </View>
          <View style={styles.buttonView}>
            <Button
              style={styles.buttonRetry}
              title="Újra"
              color={"#ff804e"}
              onPress={() => {
                deleteImage(savedImagePath);
                navigation.reset({
                  index: 0,
                  routes: [
                    {
                      name: "Home",
                      params: {
                        gyariSzamRoute: gyariSzam,
                      },
                    },
                  ],
                });
              }}
            />
            <Button
              style={styles.buttonConfirm}
              title="Elfogadás (kép törlése)"
              color={"#04c01d"}
              onPress={() => {
                deleteImage(savedImagePath);
                navigation.reset({
                  index: 0,
                  routes: [
                    {
                      name: "Home",
                      params: {
                        gyariSzamRoute: gyariSzam,
                      },
                    },
                  ],
                });
              }}
            />
          </View>
        </View>
      </ScrollView>
    </View>
  );
};
const styles = StyleSheet.create({
  container: {
    flex: 1,
    height: 600,
    backgroundColor: "#fff",
    justifyContent: "space-evenly",
    alignItems: "center",
  },
  image: {
    width: 300,
    height: 300,
    paddingBottom: 10,
  },
  buttonView: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
    alignSelf: "stretch",
    marginHorizontal: 10,
  },
  buttonRetry: {},
  buttonConfirm: {},
  textInput: {
    height: 40,
    margin: 12,
    borderWidth: 1,
    padding: 10,
    fontSize: 18,
  },
  text: {
    alignItems: "center",
  },
});

export default FinalPage;
