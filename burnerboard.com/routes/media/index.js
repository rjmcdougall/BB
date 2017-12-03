var express = require('express');
var router = express.Router();
var util = require('util')
var fs = require('fs');

var format = require('util').format;
var Multer = require('multer');
var bodyParser = require('body-parser');
var process = require('process'); // Required to mock environment variables

// Imports the Google Cloud client library
const Storage = require('@google-cloud/storage');
const storage = Storage(); 
const bucket = storage.bucket('burner-board');

router.use(bodyParser.json());

// If modifying these scopes, delete your previously saved credentials
// at ~/.credentials/drive-nodejs-quickstart.json
var SCOPES = ['https://www.googleapis.com/auth/drive.metadata.readonly'];
var TOKEN_DIR = (process.env.HOME || process.env.HOMEPATH ||
    process.env.USERPROFILE) + '/.credentials/';
var TOKEN_PATH = TOKEN_DIR + 'media.json';

const MUSIC_PATH = "BurnerBoardMusic";
const MEDIA_CATALOG = "DownloadDirectory.json"

// Multer is required to process file uploads and make them available via
// req.files.
const upload = Multer({
  storage: Multer.memoryStorage(),
  limits: {
    fileSize: 20 * 1024 * 1024 // no larger than 5mb, you can change as needed.
  }
});

/* test generating json */
router.get('/genJSON', function (req, res, next) {

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
    res.status(200).send(publicUrl);
  });

  // cb function for iterating bucket
  let cb=(err, files,next,apires) => {
    
    listFileArrayForJSON(files);

    //fileStream.write(JSON.stringify(files));
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


  //fileStream.write(JSON.stringify(stuff[0]));
  //console.log(JSON.stringify(stuff));
console.log('made it here')
});

function listFileArrayForJSON(files){
  var filepath = MUSIC_PATH + '/' + MEDIA_CATALOG;
  var mp3Length = require('mp3-length');
  
    var newFileArray = [];

    for (var i = 0, len = files.length; i < len; i++) {

      if(files[i].name.endsWith("mp3")){
        newFileArray.push({'name' : files[i].name});
        
              mp3Length(filepath + '/' + files[i].name, function (err, length) {
                  if (err == null) {
                    newFileArray.push({'length' : length});
                  }
              });
      }

      
    }
    
    
  files
};

/* GET home page. */
router.get('/', function (req, res, next) {
  listFiles(res);
});

function listFiles(res) {
  var callback = function(err, files, nextQuery, apiResponse) {
    if (nextQuery) {
      // More results exist.
      bucket.getFiles(nextQuery, callback);
    }

    res.render('tableMedia', { Datarows: files })
  };

  // Lists files in the bucket, filtered by a prefix
  var stuff = bucket
  .getFiles({
    autoPaginate: false,
    delimiter: '/',
    prefix: MUSIC_PATH + '/'
  }, callback);

}

// [START form]
// Display a form for uploading files.
router.get('/upload', (req, res) => {
    res.render('uploadForm', { title: 'burnerboard.com' });
  });

// [START process]
// Process the file upload and upload to Google Cloud Storage.
router.post('/upload', upload.single('file'), (req, res, next) => {
  if (!req.file) {
    res.status(400).send('No file uploaded.');
    return;
  }

  // Create a new blob in the bucket and upload the file data.
  var contenttype = '';
  if(req.file.originalname.endsWith('mp3'))
    contenttype = 'audio/mpeg';
  else if(req.file.originalname.endsWith('mp4'))
    contenttype = 'video/mp4';

  var filepath = MUSIC_PATH + '/' + req.file.originalname;
  const file = bucket.file(filepath);

  const fileStream = file.createWriteStream({
    metadata:{
      contentType: contenttype
    }
  });

  fileStream.on('error', (err) => {
    next(err);
  });

  fileStream.on('finish', () => {
    // The public URL can be used to directly access the file via HTTP.
    const publicUrl = format(`https://storage.googleapis.com/${bucket.name}/${file.name}`);
    res.status(200).send(publicUrl);
  });

  fileStream.end(req.file.buffer);
});
// [END process]

module.exports = router;
