import RNFS from "react-native-fs";

exports.setDefaultPeripheral = async function (peripheral) {
	// create a path you want to write to
	var path = RNFS.DocumentDirectoryPath + "/BBMobile.json";

	try {
		var fileExists = (await RNFS.readDir(RNFS.DocumentDirectoryPath)).filter((item) => {
			return item.name === "BBMobile.json";
		}).length > 0;

		if(fileExists) {
			RNFS.unlink(path);
			console.log("FileSystemConfig: File Found, Deleting");
		}

		await RNFS.writeFile(path, JSON.stringify(peripheral), "utf8");
	}
	catch (error) {
		console.log("FileSystemConfig: Error: " + error);
	}
};


exports.getDefaultPeripheral = async function () {
	// create a path you want to write to
	var path = RNFS.DocumentDirectoryPath + "/BBMobile.json";

	try {
		var fileExists = (await RNFS.readDir(RNFS.DocumentDirectoryPath)).filter((item) => {
			return item.name === "BBMobile.json";
		}).length > 0;

		if (!fileExists) {
			console.log("FileSystemConfig: File Not Found. Returning Null.");
			return null;
		}

		var peripheral = JSON.parse(await RNFS.readFile(path, "utf8"));
//		console.log("FileSystemConfig: Successfuly loaded configuration file: " + JSON.stringify(peripheral));
		return peripheral;
	}
	catch (error) {
		console.log("FileSystemConfig: Error: " + error);
	}
};