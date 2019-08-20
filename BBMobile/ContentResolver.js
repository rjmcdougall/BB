/**
 * This exposes the native ToastExample module as a JS module. This has a
 * function 'show' which takes the following parameters:
 *
 * 1. String message: A string with the text to toast
 * 2. int duration: The duration of the toast. May be ToastExample.SHORT or
 *    ToastExample.LONG
 */
import {NativeModules} from "react-native";

class ContentResolver {

	async getLocationJSON() {
		var i = await NativeModules.ContentResolver.getLocationJSON();
		console.log("Returned from Bridge");

		var JSONString = "[";
		
		i.map((item) => {
			JSONString += item + ",";
		});
		JSONString = JSONString.substring(0, JSONString.length - 1);
		JSONString += "]"; 
		return JSONString;
	}
}

export default ContentResolver;