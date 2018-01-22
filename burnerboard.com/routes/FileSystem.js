const constants = require('./Constants');
const Storage = require('@google-cloud/storage');
const storage = Storage();
const google = require('googleapis');
const drive = google.drive('v2');
const bucket = storage.bucket('burner-board');

var fileAttributes = {
	fileSize: 0,
	mimeType: "",
	title: "",
	songDuration: 0,
	speechCue: 0,
};

exports.addGDriveFile = async function (boardID, profileID, fileId, oauthToken, speechCue, callback) {

	return new Promise((resolve, reject) => {

		var tempFileName = "";

		drive.files.get({
			fileId: fileId,
			'access_token': oauthToken,
		}, function (err, jsonContent) {

			if (err)
				return reject(err);
			else {
				fileAttributes = {
					fileSize: parseInt(jsonContent.fileSize),
					mimeType: jsonContent.mimeType,
					title: jsonContent.title,
					songDuration: 0,
					speechCue: "",
				};

				if (jsonContent.title.endsWith("mp3") || jsonContent.title.endsWith("mp4")) {
					checkForFileExists(boardID, profileID, jsonContent.title)
						.then(result => {
							if (result == true)
								throw new Error("the file " + fileAttributes.title + " already exists for board " + boardID + " in profile " + profileID);
							else {

								drive.files.get({
									fileId: fileId,
									'access_token': oauthToken,
									alt: 'media'
								}, {
										encoding: null // Make sure we get the binary data
									}, function (err, content) {

										if (err)
											return reject(err);
										else {
											// now get the real file and save it.
											var filePath = constants.MUSIC_PATH + '/' + boardID + '/' + profileID + '/' + fileAttributes.title;
											var file3 = bucket.file(filePath);
											var fileStream3 = file3.createWriteStream({
												metadata: {
													contentType: fileAttributes.mimeType,
												}
											});

											fileStream3.on('error', (err) => {
												return reject(err);
											});

											fileStream3.on('finish', () => {
												file3.makePublic();

												DownloadDirectoryDS = require('./DownloadDirectoryDS');
												if (filePath.endsWith('mp3')) {

													var streamToBuffer = require('stream-to-buffer');
													var stream3 = file3.createReadStream();

													streamToBuffer(stream3, function (err, buffer) {
														var i = buffer.length;
														var mp3Duration = require('mp3-duration');
														mp3Duration(buffer, function (err, duration) {
															if (err)
																callback(err);
															fileAttributes.songDuration = Math.floor(duration);

															DownloadDirectoryDS.addMedia(boardID,
																profileID,
																'audio',
																filePath,
																fileAttributes.fileSize,
																fileAttributes.songDuration,
																"")
																.then(result => {
																	return resolve(result);
																})
																.catch(function (err) {
																	return reject(err);
																});
														});
													});
												}
												else if (filePath.endsWith('mp4')) {

													DownloadDirectoryDS.addMedia(boardID,
														profileID,
														'video',
														filePath,
														0,
														0,
														"speechCue")
														.then(result => {
															return resolve(result);
														})
														.catch(function (err) {
															return reject(err);
														});
												}
											});
											fileStream3.end(content);
										};
									});
							}
						})
						.catch(function (err) {
							return reject(err);
						});
				}
				else {
					return reject(new Error("The file must have an mp3 or mp4 extension."))
				}
			}
		});
	});
}

exports.listProfileFiles = async function (boardID, profileID) {

	return new Promise((resolve, reject) => {
		bucket.getFiles({
			autoPaginate: false,
			delimiter: '/',
			prefix: constants.MUSIC_PATH + '/' + boardID + '/' + profileID + '/'
		})
			.then(result => {
				return resolve(result[0].map(item => {
					return item.name;
				}));
			})
			.catch(function (err) {
				return reject(err);
			});
	});
}

exports.createRootBoardFolder = async function (boardID) {

	return new Promise((resolve, reject) => {

		try {
			var result = bucket.getFiles({
				autoPaginate: false,
				delimiter: '/',
				prefix: constants.MUSIC_PATH + '/template/default/'
			}).then(result => {
				for (var i = 0; i < result[0].length; i++) {
					result[0][i].copy(result[0][i].name.replace('template', boardID),
						function (err, copiedFile, apiResponse) {
							copiedFile.makePublic();
						});
				}
				return true;
			})
				.then((worked) => {
					return resolve("Root Folder " + boardID + " created");
				})
		}
		catch (err) {
			return reject(err);
		}
	});
}

checkForFileExists = function (boardID, profileID, fileName) {

	return new Promise((resolve, reject) => {
		bucket.getFiles({
			autoPaginate: false,
			delimiter: '/',
			prefix: constants.MUSIC_PATH + '/' + boardID + '/' + profileID + '/' + fileName
		})
			.then(result => {
				if (result[0].length > 0)
					return resolve(true);
				else
					return resolve(false);
			})
			.catch(function (err) {
				return reject(err);
			});
	});
}

exports.deleteMedia = async function (boardID, profileID, fileName) {

	return new Promise((resolve, reject) => {

		bucket
			.file(constants.MUSIC_PATH + '/' + boardID + '/' + profileID + "/" + fileName)
			.delete()
			.then(() => {
				return resolve("File " + constants.MUSIC_PATH + '/' + boardID + '/' + profileID + '/' + fileName + " deleted.");
			})
			.catch(err => {
				return reject(err);
			});
	});
}

exports.deleteBoard = async function (boardID) {

	return new Promise((resolve, reject) => {

		bucket
			.deleteFiles({ prefix: constants.MUSIC_PATH + "/" + boardID })
			.then(() => {
				return resolve(constants.MUSIC_PATH + "/" + boardID + "* deleted");
			})
			.catch(err => {
				return reject(err);
			});
	});
}