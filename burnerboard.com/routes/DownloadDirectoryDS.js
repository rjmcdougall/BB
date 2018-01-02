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
 

exports.addAudio = function(boardID, fileName, fileSize, fileLength)  {
 
    const localName = fileName.substring(fileName.indexOf(boardID) + boardID.length + 1);

    const audioKey = datastore.key(['board', 'vega', 'audio', localName]);

    var newAttributes = {
        URL: format(`${GOOGLE_CLOUD_BASE_URL}/${BUCKET_NAME}/${fileName}`),
        localName: localName,
        Size: fileSize,
        Length: fileLength
    };
 
    const entity = {
      key: audioKey,
      data: newAttributes,
    };
  
    datastore
      .save(entity)
      .then(() => {
        console.log(`Audio ${localName} created successfully.`);
      })
      .catch(err => {
        console.error('ERROR:', err);
      });
  }


  exports.addVideo = function (boardID, fileName, speechCue, callback) {
 
    const localName = fileName.substring(fileName.indexOf(boardID) + boardID.length + 1);

    const videoKey = datastore.key(['board', 'vega', 'video', localName]);

    max_x = db.GqlQuery("SELECT * FROM MyModel ORDER BY x DESC").get().x

    max_x = MyModel.all().order('-x').get().x

    const videoOrdinal = 1;

    var newAttributes = {
        URL: format(`${GOOGLE_CLOUD_BASE_URL}/${BUCKET_NAME}/${fileName}`),
        localName: fileName.substring(fileName.indexOf(boardID) + boardID.length + 1),
        SpeachCue: speechCue,
        ordinal: videoOrdinal
    };
 
    const entity = {
      key: videoKey,
      data: newAttributes,
    };
  
    datastore
      .save(entity)
      .then(() => {
        console.log(`Video ${localName} created successfully.`);
      })
      .catch(err => {
        console.error('ERROR:', err);
      });
  }
  
exports.getMaxAudioOrdinal = function(){
    const ancestorKey = datastore.key(['board', 'vega']);

    const query = datastore.createQuery('audio')
                            .hasAncestor(ancestorKey)
                            .order('ordinal DESC')
                            ;
  
    datastore
      .runQuery(query)
      .then(results => {
        const audioResults = results[0];
  
        console.log('audio:');
        audioResults.forEach(audio => {
          const taskKey = audio[datastore.KEY];
          console.log(audio);
        });
      })
      .catch(err => {
        console.error('ERROR:', err);
      });
}

exports.listAudio = function() {
    
  //  const datastore = new Datastore(); const key = datastore.key({ namespace: 'ns', path: ['Company', 123] });

  const ancestorKey = datastore.key(['board', 'vega']);

    const query = datastore.createQuery('audio')
                            .hasAncestor(ancestorKey);
  
    datastore
      .runQuery(query)
      .then(results => {
        const audioResults = results[0];
  
        console.log('audio:');
        audioResults.forEach(audio => {
          const taskKey = audio[datastore.KEY];
          console.log(audio);
        });
      })
      .catch(err => {
        console.error('ERROR:', err);
      });
  }
 