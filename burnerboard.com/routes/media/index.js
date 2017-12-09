var express = require('express');
var router = express.Router();
var util = require('util')
var format = require('util').format;
var Multer = require('multer');
var bodyParser = require('body-parser');
const Storage = require('@google-cloud/storage');
const storage = Storage();
const bucket = storage.bucket('burner-board');

router.use(bodyParser.json());

const MUSIC_PATH = "BurnerBoardMedia";

const upload = Multer({
	storage: Multer.memoryStorage(),
	limits: {
		fileSize: 20 * 1024 * 1024
	}
});

/* warning: this will not maintain the length of mp3 files or other metadata */
router.get('/genJSONFromFiles', function (req, res, next) {

	DownloadDirectory = require('./DownloadDirectory');
	DownloadDirectory.generateNewDirectoryJSON();
	res.status(200).send("OK");

});

/* GET home page. */
router.get('/:boardID/listFiles', function (req, res, next) {
	FileSystem = require('./FileSystem');

	FileSystem.listFiles(req.params.boardID, function(err, fileList){
		if(err){
			res.status(500).send("ERROR");
		}
		else {
			res.status(200).json(fileList);
		}

	});
});

/* GET home page. */
router.get('/:boardID/DownloadDirectoryJSON', function (req, res, next) {
	DownloadDirectory = require('./DownloadDirectory')

	DownloadDirectory.DirectoryJSON(req.params.boardID
		, function (err, data) {
			if (!err) {
				res.status(200).send(JSON.stringify(data));
			}
			else {
				res.send(err);
			}

		}
	);
});



router.get('/:boardID/upload', (req, res, next) => {
	res.render('uploadForm', { title: 'burnerboard.com', boardID: req.params.boardID });
});

function AddFile(boardID, uploadedFile, callback){
	var contenttype = '';
	var songDuration;

	if (uploadedFile.originalname.endsWith('mp3')) {

		var mp3Duration = require('mp3-duration');
		mp3Duration(uploadedFile.buffer, function (err, duration) {
			if (err){
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
								callback(null,{"localName":file.name, "Size":uploadedFile.Size, "Length":songDuration});
							})
							.catch(err => {
								callback(err,null);
							});

					}
					else {
						callback(err,null);
					}
				});
		else if (req.file.originalname.endsWith('mp4'))
			DownloadDirectory.addVideo(req.params.boardID,
				file.name,
				req.body.speechCue,
				function (err) {
					if (!err) {
						res.status(200).send("OK");
					}
					else {
						res.send(err);
					}
				});

	});

	fileStream.end(uploadedFile.buffer);
}

router.post('/:boardID/upload', upload.single('file'), (req, res, next) => {
	if (!req.file) {
		res.status(400).send('No file uploaded.');
		return;
	}

	AddFile(req.params.boardID, req.file, function(err, savedFile){
		if(!err){
			res.status(200).json(savedFile);
		}
		else{
			res.status(500).send("ERROR");
		}
	})


});


module.exports = router;
