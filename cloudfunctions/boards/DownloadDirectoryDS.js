const constants = require("./Constants");
const format = require("util").format;
const Datastore = require("@google-cloud/datastore");
const datastore = new Datastore({
	projectId: process.env.PROJECT_ID,
});

exports.activateBoardProfile = async function (boardID, profileID, isProfileGlobal) {

	const boardQuery = datastore.createQuery("board")
		.filter("name", "=", boardID);

	try {
		var results = await datastore.runQuery(boardQuery);

		// set profile 1 to profile 2
		results[0][0].profile2 = results[0][0].profile;
		results[0][0].isProfileGlobal2 = results[0][0].isProfileGlobal;	

		// set new profile to profile 1
		results[0][0].profile = profileID;
		results[0][0].isProfileGlobal = isProfileGlobal;

		await datastore.update(results[0]);
		return results[0];
	}
	catch (error) {
		throw new Error(error);
	}
};


exports.deactivateBoardProfile = async function (boardID, profileID, isProfileGlobal) {

	const boardQuery = datastore.createQuery("board")
		.filter("name", "=", boardID);

	try {
		var results = await datastore.runQuery(boardQuery);

		if(results[0][0].profile == profileID){
			results[0][0].profile = null;
			results[0][0].isProfileGlobal = null;
		}
		else {
			results[0][0].profile2 = null;
			results[0][0].isProfileGlobal2 = null;
		}

		await datastore.update(results[0]);
		return results[0];
	}
	catch (error) {
		throw new Error(error);
	}
};


exports.addMedia = async function (boardID, profileID, mediaType, fileName, fileSize, fileLength) {

	try {
		var globalFolder = "global";
		var localName = "";

		if (boardID != null)
			localName = fileName.substring(fileName.indexOf(boardID) + boardID.length + 1 + profileID.length + 1);
		else
			localName = fileName.substring(fileName.indexOf(globalFolder) + globalFolder.length + 1 + profileID.length + 1);

		const audioKey = datastore.key([mediaType]);
		var newAttributes = "";

		if (mediaType == "audio") {
			newAttributes = {
				board: boardID,
				URL: format(`${constants.GOOGLE_CLOUD_BASE_URL}/${constants.BUCKET_NAME}/${fileName}`),
				localName: localName,
				Size: fileSize,
				Length: fileLength,
				profile: profileID,
				ordinal: null //set later
			};
		}
		else { /* video */
			newAttributes = {
				board: boardID,
				URL: format(`${constants.GOOGLE_CLOUD_BASE_URL}/${constants.BUCKET_NAME}/${fileName}`),
				localName: localName,
				Size: fileSize,
				Length: fileLength,
				profile: profileID,
				ordinal: null //set later
			};
		}

		const entity = {
			key: datastore.key(mediaType),
			data: newAttributes,
		};

		const existenceQuery = datastore.createQuery(mediaType)
			.filter("board", "=", boardID)
			.filter("profile", "=", profileID)
			.filter("localName", "=", localName)
			.limit(1);

		const maxOrdinalQuery = datastore.createQuery(mediaType)
			.filter("board", "=", boardID)
			.filter("profile", "=", profileID)
			.order("ordinal", {
				descending: true
			})
			.limit(1);

		var results = await datastore.runQuery(existenceQuery);

		if (results[0].length > 0)
			throw new Error("the file " + localName + " already exists for board " + boardID + " in profile " + profileID);
		else {
			var results2 = await datastore.runQuery(maxOrdinalQuery);

			var maxOrdinal = 0;
			if (results2[0].length > 0)
				maxOrdinal = results2[0][0].ordinal;

			newAttributes.ordinal = maxOrdinal + 1;

			await datastore.save(entity);
			return mediaType + " " + localName + " created successfully with ordinal " + newAttributes.ordinal;
		}
	}
	catch (error) {
		throw new Error(error);
	}
};


exports.cloneTemplate = async function (deviceID) {
	try {

	}
	catch (error) {
		throw new Error(error);
	}
};

exports.createNewBoard = async function (deviceID) {

	try {

		var boardQuery;
 
		boardQuery = datastore.createQuery("board")
			.filter("bootName", "=", "template");

		var results = (await datastore.runQuery(boardQuery))[0][0];
 
		results.bootName = deviceID;

		var results = await datastore
			.insert({
				key: datastore.key("board"),
				data: results
			});

		return "board " + deviceID + " created ";
	}
	catch (error) {
		return error;
	}
};

exports.createProfile = async function (boardID, profileID, isGlobal) {
	try {
		var i = 2;
		var results = await datastore
			.save({
				key: datastore.key(["profile"]),
				data: {
					name: profileID,
					board: boardID,
					isGlobal: isGlobal,
				},
			});

		if (isGlobal)
			return "global profile " + profileID + " created";
		else
			return "profile " + profileID + " created for board " + boardID;

	}
	catch (error) {
		throw new Error(error);
	}
};

exports.listBoards = async function (boardID) {
	try {
		var boardQuery;

		if (boardID != null)
			boardQuery = datastore.createQuery("board")
				.filter("name", "=", boardID);
		else
			boardQuery = datastore.createQuery("board")
				.filter("isActive","=",true)
				.order("name");

		var results = await datastore.runQuery(boardQuery);

		if (boardID == null)
			results[0].splice(results[0].findIndex(board => {
				return board.bootName == "template";
			}), 1);

		return (results[0]);

	}
	catch (error) {
		throw new Error(error);
	}
};

exports.listProfiles = async function (boardID, profileID) {

	try {
		var profiles;

		if (boardID == null)
			if (profileID == null)
				profiles = datastore.createQuery("profile")
					.order("board")
					.order("name");
			else
				profiles = datastore.createQuery("profile")
					.filter("name", "=", profileID)
					.order("board");
		else if (profileID == null)
			profiles = datastore.createQuery("profile")
				.filter("board", "=", boardID)
				.order("name");
		else
			profiles = datastore.createQuery("profile")
				.filter("board", "=", boardID)
				.filter("name", "=", profileID);

		var results = await datastore.runQuery(profiles);

		return results[0].filter(item => {
			if (item.board != "template")
				return item;
		});
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.listGlobalProfiles = async function (profileID) {

	try {

		var profiles;

		if (profileID == null)
			profiles = datastore.createQuery("profile")
				.filter("isGlobal", "=", true)
				.order("board")
				.order("name");
		else
			profiles = datastore.createQuery("profile")
				.filter("isGlobal", "=", true)
				.filter("name", "=", profileID)
				.order("board");

		var results = await datastore.runQuery(profiles);

		return results[0].filter(item => {
			if (item.board != "template")
				return item;
		});
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.boardExists = async function (boardID) {
	try {

		const boardExists = datastore.createQuery("board")
			.filter("name", "=", boardID);

		var results = await datastore.runQuery(boardExists);
		return (results[0].length > 0);
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.profileExists = async function (boardID, profileID) {
	try {
		var profileExists;
		if (boardID != null)
			profileExists = datastore.createQuery("profile")
				.filter("name", "=", profileID)
				.filter("board", "=", boardID)
				.filter("isGlobal", "=", false);
		else
			profileExists = datastore.createQuery("profile")
				.filter("name", "=", profileID)
				.filter("board", "=", boardID)
				.filter("isGlobal", "=", true);

		var results = await datastore.runQuery(profileExists);

		return (results[0].length > 0);
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.deleteBoard = async function (boardID) {

	try {

		const deleteBoardQuery = datastore.createQuery("board")
			.filter("name", "=", boardID);

		var results = await datastore.runQuery(deleteBoardQuery);

		await datastore.delete(results[0].map((item) => {
			return item[datastore.KEY];
		}));

		return "Deleted " + boardID;
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.deleteProfile = async function (boardID, profileID) {

	try {
		var deleteProfileQuery;
		var countOfItems;

		if (boardID != null && profileID != null) {
			deleteProfileQuery = datastore.createQuery("profile")
				.filter("name", "=", profileID)
				.filter("board", "=", boardID);
		}
		else if (boardID != null && profileID == null) {
			deleteProfileQuery = datastore.createQuery("profile")
				.filter("board", "=", boardID);
		}
		else if (boardID == null && profileID != null) {
			deleteProfileQuery = datastore.createQuery("profile")
				.filter("name", "=", profileID)
				.filter("isGlobal", "=", true);
		}

		var results = await datastore.runQuery(deleteProfileQuery);

		countOfItems = results[0].length;

		await datastore.delete(results[0].map((item) => {
			return item[datastore.KEY];
		}));

		if (boardID != null && profileID != null)
			return "Deleted " + profileID + " for board " + boardID;
		else if (boardID != null && profileID == null)
			return "Deleted " + countOfItems + " profile(s) for board " + boardID;
		else if (boardID == null && profileID != null)
			return "Deleted " + profileID + " global profile(s) ";

	}
	catch (error) {
		throw new Error(error);
	}
};

exports.deleteAllBoardMedia = async function (boardID, mediaType) {
	try {
		const deleteMediaQuery = datastore.createQuery(mediaType)
			.filter("board", "=", boardID);

		var results = await datastore.runQuery(deleteMediaQuery);

		var results2 = await datastore.delete(results[0].map((item) => {
			return item[datastore.KEY];
		}));

		return "Deleted " + results2[0].length + " " + mediaType + " from " + boardID;
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.mediaExists = async function (boardID, profileID, mediaType, localName) {

	try {

		var existenceQuery;

		if (boardID != null)
			existenceQuery = datastore.createQuery(mediaType)
				.select("__key__")
				.filter("board", "=", boardID)
				.filter("profile", "=", profileID)
				.filter("localName", "=", localName)
				.limit(1);
		else
			existenceQuery = datastore.createQuery(mediaType)
				.select("__key__")
				.filter("profile", "=", profileID)
				.filter("localName", "=", localName)
				.limit(1);

		var results = await datastore.runQuery(existenceQuery);

		if (results[0].length > 0)
			return true;
		else {
			return false;
		}
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.deleteMedia = async function (boardID, profileID, mediaType, localName) {

	try {
		var deleteQuery;

		if (localName != null && profileID != null && boardID != null)
			deleteQuery = datastore.createQuery(mediaType)
				.filter("board", "=", boardID)
				.filter("localName", "=", localName)
				.filter("profile", "=", profileID);
		else if (localName == null && profileID != null && boardID != null)
			deleteQuery = datastore.createQuery(mediaType)
				.filter("board", "=", boardID)
				.filter("profile", "=", profileID);
		else if (localName == null && profileID != null && boardID == null)
			deleteQuery = datastore.createQuery(mediaType)
				.filter("profile", "=", profileID);
		else if (localName != null && profileID != null && boardID == null)
			deleteQuery = datastore.createQuery(mediaType)
				.filter("localName", "=", localName)
				.filter("profile", "=", profileID);

		var results = await datastore.runQuery(deleteQuery);

		var results2 = await datastore.delete(results[0].map((item => {
			return item[datastore.KEY];
		})));
		if (localName != null && profileID != null && boardID != null)
			return "Deleted one file " + boardID + ":" + profileID + ":" + mediaType + ":" + localName + "";
		else if (localName == null && profileID != null && boardID != null)
			return "Deleted " + results2[0].length + " " + mediaType + " for " + boardID + ":" + profileID;
		else if (localName == null && profileID != null && boardID == null)
			return "Deleted " + results2[0].length + " " + mediaType + " for global profile " + profileID;
		else if (localName != null && profileID != null && boardID == null)
			return "Deleted one file for global profile " + profileID;
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.createNewBoardMedia = async function (boardID, mediaType) {

	try {

		var mediaArray = await this.listMedia("template", "default", mediaType);

		var mappedMediaArray = mediaArray.map((item) => {

			var newElement;

			if (mediaType == "audio") {
				newElement = {
					key: datastore.key(mediaType),
					data: {
						board: boardID,
						URL: item.URL.replace("/template/", "/" + boardID + "/"),
						localName: item.localName,
						Size: item.Size,
						Length: item.Length,
						ordinal: item.ordinal,
						profile: item.profile,
					},
				};
			}
			else { /* video */
				newElement = {
					key: datastore.key(mediaType),
					data: {
						board: boardID,
						algorithm: item.algorithm,
						ordinal: item.ordinal,
						profile: item.profile,
					},
				};
			}

			return newElement;
		});

		await datastore.save(mappedMediaArray);
		return "template copied with " + mappedMediaArray.length + " " + mediaType + " elements";
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.cloneBoardMedia = async function (boardID, profileID, cloneFromBoardID, cloneFromProfileID, mediaType) {

	try {
		if (cloneFromBoardID == null)
			var sourcePath = "/global/" + cloneFromProfileID + "/";
		else
			var sourcePath = "/" + cloneFromBoardID + "/" + cloneFromProfileID + "/";

		if (boardID == null)
			var targetPath = "/global/" + profileID + "/";
		else
			var targetPath = "/" + boardID + "/" + profileID + "/";

		var mediaArray = await this.listMedia(cloneFromBoardID, cloneFromProfileID, mediaType);

		var mappedMediaArray = mediaArray.map((item) => {

			var newElement;

			if (mediaType == "audio") {
				newElement = {
					key: datastore.key(mediaType),
					data: {
						board: boardID,
						URL: item.URL.replace(sourcePath, targetPath),
						localName: item.localName,
						Size: item.Size,
						Length: item.Length,
						ordinal: item.ordinal,
						profile: profileID,
					},
				};
			}
			else if (item.algorithm != null) {
				newElement = {
					key: datastore.key(mediaType),
					data: {
						board: boardID,
						algorithm: item.algorithm,
						ordinal: item.ordinal,
						profile: profileID,
					},
				};
			}
			else {
				newElement = {
					key: datastore.key(mediaType),
					data: {
						board: boardID,
						URL: item.URL.replace(sourcePath, targetPath),
						localName: item.localName,
						Size: item.Size,
						ordinal: item.ordinal,
						profile: profileID,
					},
				};
			}

			return newElement;
		});

		await datastore.save(mappedMediaArray);
		return "profile copied with " + mappedMediaArray.length + " " + mediaType + " elements";
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.listMedia = async function (boardID, profileID, mediaType) {

	try {
		var mediaList;

		if (boardID == null) {
			mediaList = datastore.createQuery(mediaType)
				.filter("profile", "=", profileID)
				.order("ordinal", {
					descending: false
				});
		}
		else {
			mediaList = datastore.createQuery(mediaType)
				.filter("board", "=", boardID)
				.filter("profile", "=", profileID)
				.order("ordinal", {
					descending: false
				});
		}

		var mediaList = await datastore.runQuery(mediaList);
		return mediaList[0];
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.DirectoryJSON = async function (boardID, profileID) {

	try {
		var DirectoryJSON = {
			audio: null,
			video: null,
		};

		var results = await this.listMedia(boardID, profileID, "audio");

		DirectoryJSON.audio = results.map(function (item) {
			delete item["board"];
			delete item["profile"];
			return item;
		});

		var results2 = await this.listMedia(boardID, profileID, "video");

		DirectoryJSON.video = results2.map(function (item) {
			delete item["board"];
			delete item["profile"];
			return item;
		});

		return DirectoryJSON;

	}
	catch (error) {
		throw new Error(error);
	}
};

exports.reorderMedia = async function (boardID, profileID, mediaType, mediaArray) {

	try {

		var results = await this.listMedia(boardID, profileID, mediaType);

		for (var i = 0; i < mediaArray.length; i++) {

			var result = results.filter(function (element) {
				if (element.algorithm != null)
					return element.algorithm === mediaArray[i];
				else
					return element.localName === mediaArray[i];
			});

			result[0].ordinal = i;
		}

		await datastore.save(results);
		return await this.listMedia(boardID, profileID, mediaType);

	}
	catch (error) {
		throw new Error(error);
	}
};