import RNFS from "react-native-fs";
 
exports.getCache = async function (key) {
	// create a path you want to write to
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
		console.log("FileSystemConfig: Error: " + error);
	}
};

exports.setCache = async function (key, value) {
	// create a path you want to write to
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
		console.log("FileSystemConfig: Error: " + error);
	}
};
 