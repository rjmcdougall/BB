
// Imports the Google Cloud client library
const Storage = require('@google-cloud/storage');
const storage = Storage();
const bucket = storage.bucket('burner-board');
var format = require('util').format;

// If modifying these scopes, delete your previously saved credentials
// at ~/.credentials/drive-nodejs-quickstart.json
var SCOPES = ['https://www.googleapis.com/auth/drive.metadata.readonly'];
var TOKEN_DIR = (process.env.HOME || process.env.HOMEPATH ||
    process.env.USERPROFILE) + '/.credentials/';
var TOKEN_PATH = TOKEN_DIR + 'media.json';

const MUSIC_PATH = "BurnerBoardMusic";
const MEDIA_CATALOG = "DownloadDirectory.json";
var filepath = MUSIC_PATH + '/' + MEDIA_CATALOG;
const file = bucket.file(filepath);

exports.addVideo = function (fileName, speechCue) {

    var jsonContent;

    file.download(function (err, contents) {

        jsonContent = JSON.parse(contents);

        const writeStream = file.createWriteStream({
            metadata: {
                contentType: 'application/json'
            }
        });

        writeStream.on('error', (err) => {
            next(err);
        });

        writeStream.on('finish', () => {
            // The public URL can be used to directly access the file via HTTP.
            const publicUrl = format(`https://storage.googleapis.com/${bucket.name}/${fileName}`);
        });

        for (var i = 0, len = jsonContent.video.length; i < len; i++) {
            if (jsonContent.video[i].localName == fileName.substring(fileName.indexOf("/") + 1)){
                jsonContent.video.splice(i,1);
                break;
            }

        }

        jsonContent.video.push({
            URL: format(`https://storage.googleapis.com/${bucket.name}/${fileName}`),
            localName: fileName.substring(fileName.indexOf("/") + 1),
            SpeachCue: speechCue
        });

        writeStream.write(JSON.stringify(jsonContent));
        writeStream.end();

    });
}

exports.addAudio = function (fileName, fileSize, fileLength) {

    var jsonContent;

    file.download(function (err, contents) {

        jsonContent = JSON.parse(contents);

        const writeStream = file.createWriteStream({
            metadata: {
                contentType: 'application/json'
            }
        });

        writeStream.on('error', (err) => {
            next(err);
        });

        writeStream.on('finish', () => {
            // The public URL can be used to directly access the file via HTTP.
            const publicUrl = format(`https://storage.googleapis.com/${bucket.name}/${fileName}`);
        });

        for (var i = 0, len = jsonContent.audio.length; i < len; i++) {
            if (jsonContent.audio[i].localName == fileName.substring(fileName.indexOf("/") + 1)){
                jsonContent.audio.splice(i,1);
                break;
            }
        }

        jsonContent.audio.push({
            URL: format(`https://storage.googleapis.com/${bucket.name}/${fileName}`),
            localName: fileName.substring(fileName.indexOf("/") + 1),
            Size: fileSize,
            Length: fileLength
        });

        writeStream.write(JSON.stringify(jsonContent));
        writeStream.end();

    });

};

exports.fileExists = function (fileName) {
    var jsonContent;
    var doesExist = false;

    file.download(function (err, contents) {

        jsonContent = JSON.parse(contents);

        for (var i = 0, len = jsonContent.audio.length; i < len; i++) {
            if (jsonContent.audio[i].localName == fileName.substring(fileName.indexOf("/") + 1))
                doesExist = true;
        }

        for (var i = 0, len = jsonContent.video.length; i < len; i++) {
            if (jsonContent.video[i].localName == fileName.substring(fileName.indexOf("/") + 1))
                doesExist = true;
        }

        return doesExist;
    });
}

exports.deleteFile = function (fileName) {
    var jsonContent;
    var doesExist = false;

    file.download(function (err, contents) {

        jsonContent = JSON.parse(contents);

        for (var i = 0, len = jsonContent.audio.length; i < len; i++) {
            if (jsonContent.audio[i].localName == fileName.substring(fileName.indexOf("/") + 1))
                jsonContent.audio.splice(i,1);
        }

        for (var i = 0, len = jsonContent.video.length; i < len; i++) {
            if (jsonContent.video[i].localName == fileName.substring(fileName.indexOf("/") + 1))
                jsonContent.video.splice(i,1);
        }

        return doesExist;
    });
}