import AsyncStorage from "@react-native-community/async-storage";
 
exports.get = async function (key) {

	try {
		var value = await AsyncStorage.getItem(key)
		console.log("Get " + key + " From Cache");
		return value;
	}
	catch (error) {
		console.log("Error: " + error);
	}
};

exports.set = async function (key, value) {
 
	try {
		await AsyncStorage.setItem(key, value); 
		console.log(key + " set in cache");
	}
	catch (error) {
		console.log("Error: " + error);
	}
};

exports.clear = async function () {
	try {
		var dir = await AsyncStorage.getAllKeys();
		dir.map(async (item) => {
			await(AsyncStorage.removeItem(item));
			console.log(item.name + " Found in Cache, Deleting");
		}) ;
	}
	catch (error) {
		console.log("Error: " + error);
	}
};