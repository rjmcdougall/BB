
var express = require('express');
var router = express.Router();
var util = require('util')

// Imports the Google Cloud client library
const Storage = require('@google-cloud/storage');

// Creates a client
const storage = new Storage();


// If modifying these scopes, delete your previously saved credentials
// at ~/.credentials/drive-nodejs-quickstart.json
var SCOPES = ['https://www.googleapis.com/auth/drive.metadata.readonly'];
var TOKEN_DIR = (process.env.HOME || process.env.HOMEPATH ||
    process.env.USERPROFILE) + '/.credentials/';
var TOKEN_PATH = TOKEN_DIR + 'media.json';

/* GET home page. */
router.get('/', function (req, res, next) {
  listFiles(res);
});
  
module.exports = router;

const options = {
  prefix: '',
};

function listFiles(res) {
  var callback = function(err, files, nextQuery, apiResponse) {
    if (nextQuery) {
      // More results exist.
      bucket.getFiles(nextQuery, callback);
    }

    res.render('tableMedia', { Datarows: files })
  };

  // Lists files in the bucket, filtered by a prefix
  var stuff = storage
  .bucket('burner-board')
  .getFiles({
    autoPaginate: false,
    delimiter: '/',
    prefix: 'BurnerBoardMusic/'
  }, callback);

}
