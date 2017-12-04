
const Storage = require('@google-cloud/storage');
const storage = Storage();
const bucket = storage.bucket('burner-board');
var format = require('util').format;
const MUSIC_PATH = "BurnerBoardMusic";
const MEDIA_CATALOG = "DownloadDirectory.json";
var filepath = MUSIC_PATH + '/' + MEDIA_CATALOG;
const file = bucket.file(filepath);

exports.getDirectoryJSONPath = function(boardID){
    return format(`https://storage.googleapis.com/${bucket.name}/${MUSIC_PATH}/${boardID}/${MEDIA_CATALOG}`);
    
}

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

// currently unused.  needs tested.
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

// currently unused.  needs tested.
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

//warning: this cannot get the length of MP3 so they default to 1.
exports.generateNewDirectoryJSON = function() {

    var filepath = MUSIC_PATH + '/' + MEDIA_CATALOG;
    const file = bucket.file(filepath);
  
    const fileStream = file.createWriteStream({
      metadata:{
        contentType: 'application/json'
      }
    });
  
    fileStream.on('error', (err) => {
      next(err);
    });
  
    fileStream.on('finish', () => {
      // The public URL can be used to directly access the file via HTTP.
      const publicUrl = format(`https://storage.googleapis.com/${bucket.name}/${file.name}`);
    });
  
    // cb function for iterating bucket
    let cb=(err, files,next,apires) => {
      
      //listFileArrayForJSON(files); 
      var audioArray = [];
      var videoArray = [];
      var applicationArray = [];
      var sectionArray = [];
  
      for (var i = 0, len = files.length; i < len; i++) {
       if(files[i].name.endsWith("mp3")){
          audioArray.push({URL:format(`https://storage.googleapis.com/${bucket.name}/${files[i].name}`),
                            localName:files[i].name.substring(files[i].name.indexOf("/") + 1),
                            Size:files[i].metadata.size,
                            Length:1});
       }
       else if (files[i].name.endsWith("mp4")){
        videoArray.push({URL:format(`https://storage.googleapis.com/${bucket.name}/${files[i].name}`),
        localName:files[i].name.substring(files[i].name.indexOf("/") + 1),
        SpeachCue:""});
       }
      }
   
      applicationArray.push({URL:format(`https://storage.googleapis.com/${bucket.name}/bb-7.apk?dl=0`),
      localName:"bb-7.apk?dl=0",
      Version:"7"});
  
      sectionArray.push({audio:audioArray});
      sectionArray.push({video:videoArray});
      sectionArray.push({application:applicationArray});
  
      fileStream.write(JSON.stringify(sectionArray));
      fileStream.end();
  
      if(!!next)
      {
          bucket.getFiles(next,cb);
      }
    }
  
    // Lists files in the bucket, filtered by a prefix
    bucket.getFiles({
                      autoPaginate: false,
                      delimiter: '/',
                      prefix: MUSIC_PATH + '/'
                    }, cb);
}