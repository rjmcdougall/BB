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

exports.setBoards = async function (boardNames) {
	// create a path you want to write to
	var path = RNFS.DocumentDirectoryPath + "/boardNames.json";

	try {
		var fileExists = (await RNFS.readDir(RNFS.DocumentDirectoryPath)).filter((item) => {
			return item.name === "boardNames.json";
		}).length > 0;

		if(fileExists) {
			RNFS.unlink(path);
			console.log("FileSystemConfig: File Found, Deleting");
		}

		await RNFS.writeFile(path, JSON.stringify(boardNames), "utf8");
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

		return peripheral;
	}
	catch (error) {
		console.log("FileSystemConfig: Error: " + error);
	}
};


exports.getBoards = async function () {
	// create a path you want to write to
	var path = RNFS.DocumentDirectoryPath + "/boardNames.json";

	try {
		var fileExists = (await RNFS.readDir(RNFS.DocumentDirectoryPath)).filter((item) => {
			return item.name === "boardNames.json";
		}).length > 0;

		if (!fileExists) {
			console.log("FileSystemConfig: File Not Found. Returning Null.");
			return null;
		}

		var boards = JSON.parse(await RNFS.readFile(path, "utf8"));
		return boards;
	}
	catch (error) {
		console.log("FileSystemConfig: Error: " + error);
	}
};

exports.getUserPrefs = async function () {
	// create a path you want to write to
	var path = RNFS.DocumentDirectoryPath + "/userPrefs.json";

	try {
		var dir = await RNFS.readDir(RNFS.DocumentDirectoryPath);
		var fileExists = dir.filter((item) => {
			return item.name === "userPrefs.json";
		}).length > 0;

		if (!fileExists) {
			console.log("FileSystemConfig: userPrefs File Not Found. Returning Null.");
			return null;
		}

		var peripheral = JSON.parse(await RNFS.readFile(path, "utf8"));
		return peripheral;
	}
	catch (error) {
		console.log("FileSystemConfig: Error: " + error);
	}
};

exports.setUserPrefs = async function (userPrefs) {
	// create a path you want to write to
	var path = RNFS.DocumentDirectoryPath + "/userPrefs.json";

	try {
		var dir = await RNFS.readDir(RNFS.DocumentDirectoryPath);
		var fileExists = dir.filter((item) => {
			return item.name === "userPrefs.json";
		}).length > 0;

		if(fileExists) {
			await RNFS.unlink(path);
			console.log("FileSystemConfig: userPrefs File Found, Deleting");
		}

		await RNFS.writeFile(path, JSON.stringify(userPrefs), "utf8");
	}
	catch (error) {
		console.log("FileSystemConfig: Error: " + error);
	}
};