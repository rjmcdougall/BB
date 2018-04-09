import RNFS from "react-native-fs";

exports.setDefaultPeripheral = async function (peripheral) {
	// create a path you want to write to
	var path = RNFS.DocumentDirectoryPath + "/BBMobile.json";

	try {
		var fileExists = (await RNFS.readDir(RNFS.DocumentDirectoryPath)).filter((item) => {
			return item.name === "BBMobile.json";
		}).length > 0;

		if (!fileExists) {
			console.log("File Not Found. Creating Config File.");
			await RNFS.writeFile(path, JSON.stringify(peripheral), "utf8");
		}
	}
	catch (error) {
		console.log("Error: " + error);
	}
}


exports.getDefaultPeripheral = async function () {
	// create a path you want to write to
	var path = RNFS.DocumentDirectoryPath + "/BBMobile.json";

	try {
		var fileExists = (await RNFS.readDir(RNFS.DocumentDirectoryPath)).filter((item) => {
			return item.name === "BBMobile.json";
		}).length > 0;

		if (!fileExists) {
			console.log("File Not Found.");
			return null;
		}

		var peripheral = JSON.parse(await RNFS.readFile(path, "utf8"));
		console.log("Successfuly loaded configuration file: " + JSON.stringify(peripheral));
		return peripheral;
	}
	catch (error) {
		console.log("Error: " + error);
	}
}