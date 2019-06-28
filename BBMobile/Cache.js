import AsyncStorage from "@react-native-community/async-storage";
 
exports.get = async function (key) {

	try {
		var value = JSON.parse(await AsyncStorage.getItem(key));
		if(value)
			console.log("Get " + key + " From Cache");
		else
			console.log(key + " is not in cache, cannot retrieve.");
		return value;
	}
	catch (error) {
		console.log(error);
	}
};

exports.set = async function (key, value) {
 
	try {
		await AsyncStorage.setItem(key, JSON.stringify(value)); 
		console.log(key + " set in cache");
	}
	catch (error) {
		console.log(error);
	}
};

exports.clear = async function () {
	try {
		var dir = await AsyncStorage.getAllKeys();
		dir.map(async (item) => {
			await(AsyncStorage.removeItem(item));
			console.log(item + " Found in Cache, Deleting");
		}) ;
	}
	catch (error) {
		console.log("Error: " + error);
	}
};