const Storage = require('@google-cloud/storage');
const storage = Storage();
const bucket = storage.bucket('burner-board');

const MUSIC_PATH = "BurnerBoardMedia";

exports.addFile = function (boardID, uploadedFile, speechCue, callback) {
	var contenttype = '';
	var songDuration;
	var fileSize = uploadedFile.size;
	var localName = uploadedFile.originalname;

	if (uploadedFile.originalname.endsWith('mp3')) {

		var mp3Duration = require('mp3-duration');
		mp3Duration(uploadedFile.buffer, function (err, duration) {
			if (err) {
				callback(err);
			}
			songDuration = duration;
		});
		contenttype = 'audio/mpeg';
	}
	else if (uploadedFile.originalname.endsWith('mp4'))
		contenttype = 'video/mp4';

	var filepath = MUSIC_PATH + '/' + boardID + '/' + uploadedFile.originalname;

	const file = bucket.file(filepath);
	const fileStream = file.createWriteStream({
		metadata: {
			contentType: contenttype
		}
	});

	fileStream.on('error', (err) => {
		callback(err);
	});

	fileStream.on('finish', () => {

		DownloadDirectory = require('./DownloadDirectory');
		if (uploadedFile.originalname.endsWith('mp3'))
			DownloadDirectory.addAudio(boardID,
				file.name,
				file.Size,
				songDuration,
				function (err) {
					if (!err) {
						bucket
							.file(filepath)
							.makePublic()
							.then(() => {
								callback(null, { "localName": localName, "Size": fileSize, "Length": songDuration });
							})
							.catch(err => {
								callback(err, null);
							});

					}
					else {
						callback(err, null);
					}
				});
		else if (uploadedFile.originalname.endsWith('mp4'))
			DownloadDirectory.addVideo(boardID,
				file.name,
				speechCue,
				function (err) {
					if (!err) {
						bucket
							.file(filepath)
							.makePublic()
							.then(() => {
								callback(null, { "localName": localName, "speachCue": speechCue });
							})
							.catch(err => {
								callback(err, null);
							});

					}
					else {
						callback(err, null);
					}
				});

	});

	fileStream.end(uploadedFile.buffer);
}

exports.listFiles = function(boardID, callback) {
    
        bucket
            .getFiles({
                autoPaginate: false,
                delimiter: '/',
                prefix: MUSIC_PATH + '/' + boardID + '/'
            }, function (err, files, nextQuery, apiResponse) {
                if (err) {
                    callback(err, null);
                }
                else {
                    var fileList = [];
                    for(var i=0; i<files.length;i++){
                        fileList.push(files[i].name);
                    }
                    callback(null, fileList);
                }
            })
    
    }