const Storage = require('@google-cloud/storage');
const storage = Storage();
const bucket = storage.bucket('burner-board');

const MUSIC_PATH = "BurnerBoardMedia";

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