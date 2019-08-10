import RNFS from "react-native-fs";
import Constants from "./Constants";

exports.get = async function (key) {
	// create a path you want to write to

	key = Constants.FS_CACHE_HEADER + key;

	var path = RNFS.DocumentDirectoryPath + "/" + key;

	try {
		var dir = await RNFS.readDir(RNFS.DocumentDirectoryPath);
		var fileExists = dir.filter((item) => {
			return item.name === key;
		}).length > 0;

		if (!fileExists) {
			console.log(key + " Not Found in Cache");
			return null;
		}

		console.log("Get " + key + " From Cache");
		var value = JSON.parse(await RNFS.readFile(path, "utf8"));
		return value;
	}
	catch (error) {
		console.log("Error: " + error);
	}
};

exports.set = async function (key, value) {
	// create a path you want to write to
	key = Constants.FS_CACHE_HEADER + key;

	var path = RNFS.DocumentDirectoryPath + "/" + key;

	try {
		var dir = await RNFS.readDir(RNFS.DocumentDirectoryPath);
		var fileExists = dir.filter((item) => {
			return item.name === key;
		}).length > 0;

		if(fileExists) {
			await RNFS.unlink(path);
			console.log(key + " Found in Cache, Deleting");
		}

		console.log(key + " set in cache");
		await RNFS.writeFile(path, JSON.stringify(value), "utf8");
	}
	catch (error) {
		console.log("Error: " + error);
	}
};

exports.clear = async function () {
	try {
		var dir = await RNFS.readDir(RNFS.DocumentDirectoryPath);
		dir.map(async (item) => {
			var path = RNFS.DocumentDirectoryPath + "/" + item.name;
			if(item.name.startsWith(Constants.FS_CACHE_HEADER)){
				await RNFS.unlink(path);
				console.log(item.name + " Found in Cache, Deleting");
			}
		}) ;
	}
	catch (error) {
		console.log("Error: " + error);
	}
};