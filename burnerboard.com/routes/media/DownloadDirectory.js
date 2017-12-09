
const Storage = require('@google-cloud/storage');
const storage = Storage();
const bucket = storage.bucket('burner-board');
var format = require('util').format;
const MUSIC_PATH = "BurnerBoardMedia";
const MEDIA_CATALOG = "DownloadDirectory.json";
const GOOGLE_CLOUD_BASE_URL = "https://storage.googleapis.com";

exports.getDirectoryJSONPath = function (boardID) {
    return format(`${GOOGLE_CLOUD_BASE_URL}/${bucket.name}/${MUSIC_PATH}/${boardID}/${MEDIA_CATALOG}`);

}

exports.DirectoryJSON = function (boardID, callback) {

    var jsonContent;

    var filepath = MUSIC_PATH + '/' + boardID + '/' + MEDIA_CATALOG;
    const file = bucket.file(filepath);

    file.download(function (err, contents) {

        if (!err) {
            jsonContent = JSON.parse(contents);
            callback(null, jsonContent);
        } else {
            callback(error, null);
        }

    });
}


exports.addVideo = function (boardID, fileName, speechCue, callback) {

    var jsonContent;

    var filepath = MUSIC_PATH + '/' + boardID + '/' + MEDIA_CATALOG;
    const file = bucket.file(filepath);

    file.download(function (err, contents) {

        jsonContent = JSON.parse(contents);

        const writeStream = file.createWriteStream({
            metadata: {
                contentType: 'application/json'
            }
        });

        writeStream.on('error', (err) => {
            callback(err);
        });

        writeStream.on('finish', () => {
        });

        for (var i = 0, len = jsonContent.video.length; i < len; i++) {
            if (jsonContent.video[i].localName == fileName.substring(fileName.indexOf(boardID) + boardID.length + 1)) {
                jsonContent.video.splice(i, 1);
                break;
            }

        }

        jsonContent.video.push({
            URL: format(`${GOOGLE_CLOUD_BASE_URL}/${fileName}`),
            localName: fileName.substring(fileName.indexOf(boardID) + boardID.length + 1),
            SpeachCue: speechCue
        });

        writeStream.write(JSON.stringify(jsonContent));
        writeStream.end();

        callback(null);

    });
}

exports.addAudio = function (boardID, fileName, fileSize, fileLength, callback) {

    var jsonContent;

    var filepath = MUSIC_PATH + '/' + boardID + '/' + MEDIA_CATALOG;
    const file = bucket.file(filepath);

    file.download(function (err, contents) {

        jsonContent = JSON.parse(contents);

        const writeStream = file.createWriteStream({
            metadata: {
                contentType: 'application/json'
            }
        });

        writeStream.on('error', (err) => {
            callback(err);
        });

        writeStream.on('finish', () => {

        });

        for (var i = 0, len = jsonContent.audio.length; i < len; i++) {
            if (jsonContent.audio[i].localName == fileName.substring(fileName.indexOf(boardID) + boardID.length + 1)) {
                jsonContent.audio.splice(i, 1);
                break;
            }
        }

        jsonContent.audio.push({
            URL: format(`${GOOGLE_CLOUD_BASE_URL}/${fileName}`),
            localName: fileName.substring(fileName.indexOf(boardID) + boardID.length + 1),
            Size: fileSize,
            Length: fileLength
        });

        writeStream.write(JSON.stringify(jsonContent));
        writeStream.end();

        callback(null);

    });

};

// currently unused.  needs tested.
exports.fileExists = function (boardID, fileName) {
    var jsonContent;
    var doesExist = false;

    var filepath = MUSIC_PATH + '/' + boardID + '/' + MEDIA_CATALOG;
    const file = bucket.file(filepath);

    file.download(function (err, contents) {

        jsonContent = JSON.parse(contents);

        for (var i = 0, len = jsonContent.audio.length; i < len; i++) {
            if (jsonContent.audio[i].localName == fileName.substring(fileName.indexOf(boardID) + boardID.length + 1))
                doesExist = true;
        }

        for (var i = 0, len = jsonContent.video.length; i < len; i++) {
            if (jsonContent.video[i].localName == fileName.substring(fileName.indexOf(boardID) + boardID.length + 1))
                doesExist = true;
        }

        return doesExist;
    });
}

// currently unused.  needs tested.
exports.deleteFile = function (fileName) {
    var jsonContent;
    var doesExist = false;

    var filepath = MUSIC_PATH + '/' + boardID + '/' + MEDIA_CATALOG;
    const file = bucket.file(filepath);

    file.download(function (err, contents) {

        jsonContent = JSON.parse(contents);

        for (var i = 0, len = jsonContent.audio.length; i < len; i++) {
            if (jsonContent.audio[i].localName == fileName.substring(fileName.indexOf(boardID) + boardID.length + 1))
                jsonContent.audio.splice(i, 1);
        }

        for (var i = 0, len = jsonContent.video.length; i < len; i++) {
            if (jsonContent.video[i].localName == fileName.substring(fileName.indexOf(boardID) + boardID.length + 1))
                jsonContent.video.splice(i, 1);
        }

        return doesExist;
    });
}

//warning: this cannot get the length of MP3 so they default to 1.
exports.generateNewDirectoryJSON = function (boardID) {

    var filepath = MUSIC_PATH + '/' + boardID + '/' + MEDIA_CATALOG;
    const file = bucket.file(filepath);

    const fileStream = file.createWriteStream({
        metadata: {
            contentType: 'application/json'
        }
    });

    fileStream.on('error', (err) => {
        next(err);
    });

    fileStream.on('finish', () => {
    });

    // cb function for iterating bucket
    let cb = (err, files, next, apires) => {

        var audioArray = [];
        var videoArray = [];
        var applicationArray = [];
        var sectionArray = [];

        for (var i = 0, len = files.length; i < len; i++) {
            if (files[i].name.endsWith("mp3")) {
                audioArray.push({
                    URL: format(`${GOOGLE_CLOUD_BASE_URL}/${bucket.name}/${boardID}/${files[i].name}`),
                    localName: files[i].name.substring(files[i].name.indexOf("/") + 1),
                    Size: files[i].metadata.size,
                    Length: 1
                });
            }
            else if (files[i].name.endsWith("mp4")) {
                videoArray.push({
                    URL: format(`${GOOGLE_CLOUD_BASE_URL}/${bucket.name}/${boardID}/${files[i].name}`),
                    localName: files[i].name.substring(files[i].name.indexOf("/") + 1),
                    SpeachCue: ""
                });
            }
        }

        applicationArray.push({
            URL: format(`${GOOGLE_CLOUD_BASE_URL}/${bucket.name}/${boardID}/bb-7.apk?dl=0`),
            localName: "bb-7.apk?dl=0",
            Version: "7"
        });

        sectionArray.push({ audio: audioArray });
        sectionArray.push({ video: videoArray });
        sectionArray.push({ application: applicationArray });

        fileStream.write(JSON.stringify(sectionArray));
        fileStream.end();

        if (!!next) {
            bucket.getFiles(next, cb);
        }
    }

    bucket.getFiles({
        autoPaginate: false,
        delimiter: '/',
        prefix: MUSIC_PATH + '/'
    }, cb);
}