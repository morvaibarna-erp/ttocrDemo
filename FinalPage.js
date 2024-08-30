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
var RNFS = require("react-native-fs");

const deleteImage = (path) => {
  const filepath = path;

  RNFS.exists(filepath)
    .then((result) => {
      console.log("file exists: ", result);

      if (result) {
        return (
          RNFS.unlink(filepath)
            .then(() => {
              console.log("FILE DELETED");
            })
            // `unlink` will throw an error, if the item to unlink does not exist
            .catch((err) => {
              console.log(err.message);
            })
        );
      }
    })
    .catch((err) => {
      console.log(err.message);
    });
};

const FinalPage = ({ route, navigation }) => {
  const { allas, gyariSzam, savedImagePath } = route.params;
  const [kwh, setKwh] = useState(allas);

  return (
    <View style={styles.container}>
      <ScrollView>
        <View style={styles.container}>
          <Image
            source={{ uri: "file://" + savedImagePath }} // Update the path to your local image
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
                deleteImage.bind(this, savedImagePath);
                navigation.reset({
                  index: 0,
                  routes: [{ name: "Home" }],
                });
              }}
            />
            <Button
              style={styles.buttonConfirm}
              title="Elfogadás (kép törlése)"
              color={"#04c01d"}
              onPress={() => {
                deleteImage.bind(this, savedImagePath);
                navigation.reset({
                  index: 0,
                  routes: [{ name: "Home" }],
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
