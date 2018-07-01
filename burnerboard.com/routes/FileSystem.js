const constants = require("./Constants");
const Storage = require("@google-cloud/storage");
const storage = Storage();
const google = require("googleapis");
const drive = google.drive("v2");
const bucket = storage.bucket("burner-board");
const ffmpegPath = require("ffmpeg-static").path;
const ffmpeg = require("fluent-ffmpeg");
var ffprobe = require("ffprobe-static");
ffmpeg.setFfmpegPath(ffmpegPath);
ffmpeg.setFfprobePath(ffprobe.path);
const DownloadDirectoryDS = require("./DownloadDirectoryDS");

var fileAttributes = {
	fileSize: 0,
	mimeType: "",
	title: "",
	songDuration: 0,
};

exports.filePath = function (boardID, profileID) {
	if (boardID != null)
		return constants.MUSIC_PATH + "/" + boardID + "/" + profileID + "/" + fileAttributes.title;
	else
		return constants.MUSIC_PATH + "/global/" + profileID + "/" + fileAttributes.title;
};


exports.getGDriveMetadata = async function (fileId, oauthToken) {
	return new Promise((resolve, reject) => {

		drive.files.get({
			fileId: fileId,
			"access_token": oauthToken,
		}, function (err, jsonContent) {

			if (err)
				return reject(err);
			else {
				fileAttributes = {
					fileSize: parseInt(jsonContent.fileSize),
					mimeType: jsonContent.mimeType,
					title: jsonContent.title,
					songDuration: 0,
				};
				resolve();
			}
		}
		);
	});
};

exports.getDriveContent = async function (boardID, profileID, fileId, oauthToken) {

	var module = this;

	return new Promise((resolve, reject) => {

		drive.files.get({
			fileId: fileId,
			"access_token": oauthToken,
			alt: "media"
		}, { encoding: null }, function (err, content) {

			if (err)
				return reject(err);
			else {
				// now get the real file and save it.
				var filePath = module.filePath(boardID, profileID);

				var file3 = bucket.file(filePath);
				var fileStream3 = file3.createWriteStream({
					metadata: {
						contentType: fileAttributes.mimeType,
					}
				})
					.on("error", (err) => {
						return reject(err);
					})
					.on("finish", () => {
						file3.makePublic();
						return resolve();
					})
					.end(content);
			}
		});
	});
};

function getSeconds(str) {
	var durationParts = str.split(":");

	var seconds = 0;
	seconds += durationParts[0] * 60 * 60;
	seconds += durationParts[1] * 60;
	seconds += Math.floor(durationParts[2]);
	return seconds;
}

exports.addGDriveFile = async function (boardID, profileID, fileId, oauthToken) {

	try {

		await this.getGDriveMetadata(fileId, oauthToken);

		var filePath = this.filePath(boardID, profileID);

		if (!fileAttributes.title.endsWith("mp3") && !fileAttributes.title.endsWith("mp4") && !fileAttributes.title.endsWith("m4a"))
			throw new Error("The file must have an mp3, or mp4 extension.");

		var result = await this.checkForFileExists(boardID, profileID, fileAttributes.title);
		if (result == true)
			throw new Error("the file " + fileAttributes.title + " already exists for board " + boardID + " in profile " + profileID);

		await this.getDriveContent(boardID, profileID, fileId, oauthToken);

		// duration is jacked.  two different approaches.
		if(filePath.endsWith(".mp4") || filePath.endsWith(".m4a"))
			fileAttributes.songDuration = Math.floor(await this.getDurationMP4(boardID, profileID));
		else // mp3
			fileAttributes.songDuration = Math.floor(await this.getDurationMP3(boardID, profileID));

		if (filePath.endsWith(".mp3") || filePath.endsWith(".m4a")) {

			return await DownloadDirectoryDS.addMedia(boardID,
				profileID,
				"audio",
				filePath,
				fileAttributes.fileSize,
				fileAttributes.songDuration);
		}
		else if (filePath.endsWith(".mp4")) {

			return await DownloadDirectoryDS.addMedia(boardID,
				profileID,
				"video",
				filePath,
				fileAttributes.fileSize,
				fileAttributes.songDuration);
		}
	}
	catch (error) {
		throw error;
	}
};

exports.listProfileFiles = async function (boardID, profileID) {
	try {

		var result = await bucket.getFiles({
			autoPaginate: false,
			delimiter: "/",
			prefix: constants.MUSIC_PATH + "/" + boardID + "/" + profileID + "/"
		});

		return result[0].map(item => {
			return item.name;
		});
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.copyProfileFiles = async function (boardID, profileID, cloneFromBoardID, cloneFromProfileID) {

	try {

		var replacementPath = "";
		var profilePath = "";

		if (boardID != null)
			replacementPath = constants.MUSIC_PATH + "/" + boardID + "/" + profileID + "/";
		else
			replacementPath = constants.MUSIC_PATH + "/global/" + profileID + "/";

		if (cloneFromBoardID != null)
			profilePath = constants.MUSIC_PATH + "/" + cloneFromBoardID + "/" + cloneFromProfileID + "/";
		else
			profilePath = constants.MUSIC_PATH + "/global/" + cloneFromProfileID + "/";

		var result = await bucket.getFiles({
			autoPaginate: false,
			delimiter: "/",
			prefix: profilePath,
		});

		return result[0].map(item => {
			item.copy(item.name.replace(profilePath, replacementPath));
			return item.name;
		});
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.createRootBoardFolder = async function (boardID) {

	try {
		var result = await bucket.getFiles({
			autoPaginate: false,
			delimiter: "/",
			prefix: constants.MUSIC_PATH + "/template/default/"
		});

		for (var i = 0; i < result[0].length; i++) {
			result[0][i].copy(result[0][i].name.replace("template", boardID),
				function (err, copiedFile, apiResponse) {
					copiedFile.makePublic();
				});
		}

		return "Root Folder " + boardID + " created";
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.checkForFileExists = async function (boardID, profileID, fileName) {

	try {
		var profilePath = "";
		if (boardID != null)
			profilePath = constants.MUSIC_PATH + "/" + boardID + "/" + profileID + "/" + fileName;
		else
			profilePath = constants.MUSIC_PATH + "/global/" + profileID + "/" + fileName;

		var result = await bucket.getFiles({
			autoPaginate: false,
			delimiter: "/",
			prefix: profilePath
		});

		if (result[0].length > 0)
			return true;
		else
			return false;

	}
	catch (error) {
		throw new Error(error);
	}
};

exports.deleteMedia = async function (boardID, profileID, fileName) {

	try {

		var filePath = "";
		if (boardID != null)
			filePath = constants.MUSIC_PATH + "/" + boardID + "/" + profileID + "/" + fileName;
		else
			filePath = constants.MUSIC_PATH + "/global/" + profileID + "/" + fileName;

		await bucket
			.file(filePath)
			.delete();

		return "File " + filePath + " deleted.";
	}
	catch (error) {
		throw new Error(error);
	}
};

exports.deleteProfile = async function (boardID, profileID) {

	try {
		var filePath = "";

		if (boardID != null)
			filePath = constants.MUSIC_PATH + "/" + boardID + "/" + profileID;
		else
			filePath = constants.MUSIC_PATH + "/global/" + profileID;

		await bucket.deleteFiles({ prefix: filePath });
		return filePath + "* deleted";

	}
	catch (error) {
		throw new Error(error);
	}
};

exports.deleteBoard = async function (boardID) {
	try {
		await bucket.deleteFiles({ prefix: constants.MUSIC_PATH + "/" + boardID });
		return constants.MUSIC_PATH + "/" + boardID + "* deleted";
	}
	catch (error) {
		throw new Error(error);
	}
};

// DOES NOT WORK~~
exports.convertToMP4 = async function (boardID, profileID, fileName) {

	ffmpeg.getAvailableCodecs(function (err, codecs) {
		console.log("Available codecs:");
		console.dir(codecs);
	});


	var duration = 0; // we can only get this via converting the file. No Metadata!!
	var filePath = "";

	if (boardID != null)
		filePath = constants.MUSIC_PATH + "/" + boardID + "/" + profileID + "/" + fileName;
	else
		filePath = constants.MUSIC_PATH + "/global/" + profileID + "/" + fileName;

	var file = bucket.file(filePath);
	var newFilePath = file.name.replace(".mp3", ".mp4", ".m4a");
	var remoteReadStream = bucket.file(file.name).createReadStream();
	var remoteWriteStream = bucket.file(newFilePath)
		.createWriteStream({
			metadata: {
				contentType: "audio/mp4",
			}
		});

	return new Promise((resolve, reject) => {
		ffmpeg()
			.input(remoteReadStream)
			.format("mp4")
			.outputOptions("-movflags empty_moov")
			.audioCodec("aac")
			.audioBitrate(128)
			.outputOptions("-y")
			.on("start", (cmdLine) => {
				console.log("Started ffmpeg with command:", cmdLine);
			})
			.on("progress", (progress) => {
				console.log("[ffmpeg] " + JSON.stringify(progress));
				duration = progress.timemark;
			})
			.on("end", async (data) => {
				console.log("Successfully re-encoded audio.");
				console.log(JSON.stringify(data));

				resolve(duration);
			})
			.on("error", (err, stdout, stderr) => {
				console.error("An error occured during encoding", err.message);
				console.error("stdout:", stdout);
				console.error("stderr:", stderr);
				reject(err);
			})
			.pipe(remoteWriteStream, { end: true });
	});

};

exports.getDurationMP4 = async function (boardID, profileID, fileName) {

	var duration = 0;
	var filePath = "";

	var filePath = this.filePath(boardID, profileID);

	var file = bucket.file(filePath);
	var remoteReadStream = bucket.file(file.name).createReadStream();

	return new Promise((resolve, reject) => {

		ffmpeg()
			.input(remoteReadStream)
			.ffprobe(function (err, data) {
				if (err)
					reject(err);
				else {
					resolve(data.format.duration);
				}
			});

	});

};

exports.getDurationMP3 = async function (boardID, profileID) {

	var duration = 0;  
	var filePath = this.filePath(boardID, profileID);

	var file = bucket.file(filePath);
	var newFilePath = file.name.replace(".mp3", ".tmp");
	var remoteReadStream = bucket.file(file.name).createReadStream();
	var remoteWriteStream = bucket.file(newFilePath)
		.createWriteStream({
			metadata: {
				contentType: "audio/mp3",
			}
		});
	var duration;

	return new Promise((resolve, reject) => {
		ffmpeg()
			.input(remoteReadStream)
			.format("mp3")
			.addOption("-c copy")
			.on("start", (cmdLine) => {
				console.log("Started ffmpeg with command:", cmdLine);
			})
			.on("progress", (progress) => {
				console.log("[ffmpeg]" + JSON.stringify(progress));
				duration = progress.timemark;
			})
			.on("end", async (data) => {
				console.log("Successfully re-encoded audio.");
				console.log(JSON.stringify(data));

				var a = duration.split(":");
				var seconds = (a[0] * 60 * 60) + (a[1] * 60) + (a[2]); 
				resolve(seconds);
			})
			.on("error", (err, stdout, stderr) => {
				console.error("An error occured during encoding", err.message);
				console.error("stdout:", stdout);
				console.error("stderr:", stderr);
				reject(err);
			})
			.pipe(remoteWriteStream, { end: true });  
	});
};
