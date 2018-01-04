const Storage = require('@google-cloud/storage');
const storage = Storage();
const bucketName = 'burner-board';
const google = require('googleapis');
const drive = google.drive('v2');
const bucket = storage.bucket('burner-board');

const MUSIC_PATH = "BurnerBoardMedia";

var fileAttributes = {
	fileSize: 0,
	mimeType: "",
	title: "",
	songDuration: 0,
	speechCue: 0,
};

exports.addGDriveFile = function (boardID, fileId, oauthToken, speechCue, callback) {

	return new Promise((resolve, reject) => {

		var tempFileName = "";

		drive.files.get({
			fileId: fileId,
			'access_token': oauthToken,
			//	alt: 'media'
		}, function (err, jsonContent) {

			if (err)
				return reject(err);
			else {
				fileAttributes = {
					fileSize: jsonContent.fileSize,
					mimeType: jsonContent.mimeType,
					title: jsonContent.title,
					songDuration: 0,
					speechCue: "",
				};

				checkForFileExists(boardID, jsonContent.title)
					.then(result => {
						if (result == true)
							throw new Error("the file " + fileAttributes.title + " already exists for board " + boardID);
						else {

							drive.files.get({
								fileId: fileId,
								'access_token': oauthToken,
								alt: 'media'
							}, function (err, content) {

								if (err)
									return reject(err);
								else {
									// now get the real file and save it.
									var filePath = 'BurnerBoardMedia' + '/' + boardID + '/' + fileAttributes.title;
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

										// now we need to add a record to directory json
										DownloadDirectoryDS = require('./DownloadDirectoryDS');
										if (filePath.endsWith('mp3')) {
											DownloadDirectoryDS.addMedia(boardID,
												'audio',
												filePath,
												fileAttributes.fileSize,
												fileAttributes.songDuratio,
												"")
												.then(result => {
													return resolve(result);
												})
												.catch(function (err) {
													return reject(err);
												});
										}


										else if (filePath.endsWith('mp4')) {

											DownloadDirectoryDS.addMedia(boardID,
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
		});
	});
}

// ** NEEDS REVISTITED **
exports.addFile = function (boardID, uploadedFile, speechCue, callback) {
	// var contenttype = '';
	// var songDuration;
	// var fileSize = uploadedFile.size;
	// var localName = uploadedFile.originalname;

	// if (uploadedFile.originalname.endsWith('mp3')) {

	// 	var mp3Duration = require('mp3-duration');
	// 	mp3Duration(uploadedFile.buffer, function (err, duration) {
	// 		if (err) {
	// 			callback(err);
	// 		}
	// 		songDuration = duration;
	// 	});
	// 	contenttype = 'audio/mpeg';
	// }
	// else if (uploadedFile.originalname.endsWith('mp4'))
	// 	contenttype = 'video/mp4';

	// var filepath = MUSIC_PATH + '/' + boardID + '/' + uploadedFile.originalname;

	// const file = bucket.file(filepath);
	// const fileStream = file.createWriteStream({
	// 	metadata: {
	// 		contentType: contenttype
	// 	}
	// });

	// fileStream.on('error', (err) => {
	// 	callback(err);
	// });

	// fileStream.on('finish', () => {

	// 	DownloadDirectoryDS = require('./DownloadDirectoryDS'); // fix me
	// 	if (uploadedFile.originalname.endsWith('mp3'))
	// 		DownloadDirectory.addAudio(boardID,
	// 			file.name,
	// 			file.Size,
	// 			songDuration,
	// 			function (err) {
	// 				if (!err) {
	// 					bucket
	// 						.file(filepath)
	// 						.makePublic()
	// 						.then(() => {
	// 							callback(null, { "localName": localName, "Size": fileSize, "Length": songDuration });
	// 						})
	// 						.catch(err => {
	// 							callback(err, null);
	// 						});

	// 				}
	// 				else {
	// 					callback(err, null);
	// 				}
	// 			});
	// 	else if (uploadedFile.originalname.endsWith('mp4'))
	// 		DownloadDirectory.addVideo(boardID,
	// 			file.name,
	// 			speechCue,
	// 			function (err) {
	// 				if (!err) {
	// 					bucket
	// 						.file(filepath)
	// 						.makePublic()
	// 						.then(() => {
	// 							callback(null, { "localName": localName, "speachCue": speechCue });
	// 						})
	// 						.catch(err => {
	// 							callback(err, null);
	// 						});

	// 				}
	// 				else {
	// 					callback(err, null);
	// 				}
	// 			});

	// });

	// fileStream.end(uploadedFile.buffer);
}

exports.listFiles = function (boardID) {

	return new Promise((resolve, reject) => {
		bucket.getFiles({
			autoPaginate: false,
			delimiter: '/',
			prefix: MUSIC_PATH + '/' + boardID + '/'
		})
		.then(result => {
			return resolve(result);
		})
		.catch(function (err) {
			return reject(err);
		});

	});
}

exports.createRootBoardFolder = function(boardID) {

	return new Promise((resolve, reject) => {
		bucket.getFiles({
			autoPaginate: false,
			delimiter: '/',
			prefix: MUSIC_PATH + '/template/'
		})
		.then(result => {

			for(var i=0;i<result[0].length;i++){
				result[0][i].copy(result[0][i].name.replace('template',boardID));
			}
			return resolve("OK");
		})
		.catch(function (err) {
			return reject(err);
		});

	});

}

checkForFileExists = function (boardID, fileName) {

	
	return new Promise((resolve, reject) => {
		bucket.getFiles({
			autoPaginate: false,
			delimiter: '/',
			prefix: MUSIC_PATH + '/' + boardID + '/' + fileName
		})
		.then(result => {
			if(result[0].length>0)
				return resolve(true);
			else
				return resolve(false);				
		})
		.catch(function (err) {
			return reject(err);
		});

	});
}