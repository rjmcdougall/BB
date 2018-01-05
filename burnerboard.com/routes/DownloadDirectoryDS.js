//import { Key } from '@google-cloud/datastore/src/entity';
var format = require('util').format;

// Imports the Google Cloud client library
const Datastore = require('@google-cloud/datastore');

const MUSIC_PATH = "BurnerBoardMedia";
const MEDIA_CATALOG = "DownloadDirectory.json";
const GOOGLE_CLOUD_BASE_URL = "https://storage.googleapis.com";
const BUCKET_NAME = 'burner-board';

// Your Google Cloud Platform project ID
const projectId = 'burner-board';

// Creates a client
const datastore = new Datastore({
  projectId: projectId,
});
 

/* add either media type w/ optional parameters
TODO: right now you could get duplicate ordinals with rapid succession of calls.
this should not affect overall app */
exports.addMedia = function (boardID, mediaType, fileName, fileSize, fileLength, speechCue) {

  return new Promise((resolve, reject) => {

    const localName = fileName.substring(fileName.indexOf(boardID) + boardID.length + 1);
    const audioKey = datastore.key([mediaType]);
    var newAttributes = "";

    if (mediaType == 'audio') {
      newAttributes = {
        board: boardID,
        URL: format(`${GOOGLE_CLOUD_BASE_URL}/${BUCKET_NAME}/${fileName}`),
        localName: localName,
        Size: fileSize,
        Length: fileLength,
        ordinal: null //set later
      };
    }
    else { /* video */
      newAttributes = {
        board: boardID,
        URL: format(`${GOOGLE_CLOUD_BASE_URL}/${BUCKET_NAME}/${fileName}`),
        localName: fileName.substring(fileName.indexOf(boardID) + boardID.length + 1),
        SpeachCue: speechCue,
        ordinal: null //set later
      };
    }

    const entity = {
      key: datastore.key(mediaType),
      data: newAttributes,
    };

    const existenceQuery = datastore.createQuery(mediaType)
      .filter('board', '=', boardID)
      .filter('localName', '=', localName)
      .limit(1);

    const maxOrdinalQuery = datastore.createQuery(mediaType)
      .filter('board', '=', boardID)
      .order('ordinal', {
        descending: true
      })
      .limit(1);

    datastore.runQuery(existenceQuery)
      .then(results => {
        if (results[0].length > 0)
          throw new Error("the file " + localName + " already exists for board " + boardID);
        else {
          datastore.runQuery(maxOrdinalQuery)
            .then(results => {

              var maxOrdinal = 0
              if (results[0].length > 0)
                maxOrdinal = results[0][0].ordinal;
              return maxOrdinal;

            })
            .then(function (maxOrdinal) {

              newAttributes.ordinal = maxOrdinal + 1;

              datastore
                .save(entity)
                .then(() => {
                  resolve(mediaType + ` ${localName} created successfully with ordinal ` + newAttributes.ordinal);
                  // console.log(`Video ${localName} created successfully with ordinal ` + newAttributes.ordinal);
                })
                .catch(err => {
                  return reject(err);
                  //console.error('ERROR:', err);
                });
            })
            .catch(err => {
              //console.error('ERROR:', err);
              return reject(err);
            });
        }
      })
      .catch(err => {
        return reject(err);
        //console.error('ERROR:', err);
      });
  });
}


exports.createNewBoardMedia = async function (boardID, mediaType) {

  var entityArray1 = [];

  return new Promise((resolve, reject) => {

    this.listMedia('template', mediaType)
      .then(results => {

        var resultsPromise = new Promise((resolve, reject) => {
          var mediaArray = results;

          resolve(mediaArray.map((item) => {

            var newElement;

            if (mediaType == 'audio') {
              newElement = {
                key: datastore.key(mediaType),
                data: {
                  board: boardID,
                  URL: item.URL.replace('/template/', '/' + boardID + '/'),
                  localName: item.localName,
                  Size: item.Size,
                  Length: item.Length,
                  ordinal: item.ordinal,
                },
              };
            }
            else { /* video */
              newElement = {
                key: datastore.key(mediaType),
                data: {
                  board: boardID,
                  algorithm: item.algorithm,
                  ordinal: item.ordinal,
                },
              };
            }

            entityArray1.push(newElement);
          }))
        })
          .then(() => {
            datastore
              .save(entityArray1)
              .then(() => {
                resolve("template copied with " + entityArray1.length + " " + mediaType +  " elements");
              })
              .catch(err => {
                return reject(err);
              });
          })
      });
  });
}


exports.createNewBoardAudio = async function (boardID) {

  var entityArray1 = [];

  return new Promise((resolve, reject) => {

    this.listMedia('template', 'audio')
      .then(results => {

        var resultsPromise = new Promise((resolve, reject) => {
          var mediaArray = results;

          resolve(mediaArray.map((item) => {
            entityArray1.push({
              key: datastore.key('audio'),
              data: {
                board: boardID,
                algorithm: item.algorithm,
                ordinal: item.ordinal,
              },
            })
          }))
        })
          .then(() => {
            datastore
              .save(entityArray1)
              .then(() => {
                resolve("template copied with " + entityArray1.length + " audio elements");
              })
              .catch(err => {
                return reject(err);
              });
          })
      });
  });
}


// exports.createNewBoard = async function (boardID) {

//       return new Promise((resolve, reject) => {
//        
//         var entityArray2 = [];

   
//           .then(() => {

//           var resultsPromise = new Promise((resolve, reject) => {

//             this.listMedia('template', 'audio')
//               .then(results => {

//                 var mediaArray = results;
//                 entityArray2 = [];

//                 resolve(mediaArray.map((item) => {
//                   entityArray2.push({
//                     key: datastore.key('audio'),
//                     data: {
//                       board: boardID,
//                       algorithm: item.algorithm,
//                       ordinal: item.ordinal,
//                     },
//                   })
//                 }))

//               })
//               .then(() => {

//                 datastore
//                   .save(entityArray2)
//                   .then(() => {
//                     console.log("template copied with " + entityArray2.length + "items");
//                     return resolve(["audio template copied with " + entityArray2.length + "items",
//                     "template copied with " + entityArray1.length + "items"]);
//                   })
//                   .catch(err => {
//                     return reject(err);
//                     //console.error('ERROR:', err);
//                   });

//               });

//           });
//           resolve(["audio template copied with " + entityArray2.length + "items",
//           "template copied with " + entityArray1.length + "items"])

//         });
//       });
//     });
// }



exports.listMedia = async function (boardID, mediaType) {

  return new Promise((resolve, reject) => {

    const mediaList = datastore.createQuery(mediaType)
      .filter('board', '=', boardID)
      .order('ordinal', {
        descending: false
      })

    datastore
      .runQuery(mediaList)
      .then(results => {
        const mediaList = results[0];

        return resolve(mediaList);

      })
      .catch(err => {
        reject(err);
      });
  });
}

exports.DirectoryJSON = function (boardID) {

  return new Promise((resolve, reject) => {

    var DirectoryJSON = {
      audio: null,
      video: null,
    };

    this.listMedia(boardID, 'audio')
      .then(results => {
        DirectoryJSON.audio = results;
        this.listMedia(boardID, 'video')
          .then(results => {
            DirectoryJSON.video = results;
            return resolve(DirectoryJSON);
          })
       }
    )
    .catch(err => {
      reject(err);
    });
  });
 
}
exports.reorderMedia = function (boardID, mediaType, mediaArray) {

  return new Promise((resolve, reject) => {
    this.listMedia(boardID, mediaType)
    .then(results => {
      for (var i = 0; i < mediaArray.length; i++) {

				var result = results.filter(function (element) {
					return element.localName === mediaArray[i];
					console.log("found" + mediaArray[i])
        });
        
        result[0].ordinal = i;
      }

      datastore.save(results)
        .then(() => {
          resolve(this.listMedia(boardID, mediaType));
        })
        .catch(err => {
          reject(err);
        });
      
    })
   });

 
}