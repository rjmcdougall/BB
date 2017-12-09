var express = require('express');
var router = express.Router();
var fs = require('fs');
var readline = require('readline');
var google = require('googleapis');
var googleAuth = require('google-auth-library');
var util = require('util')


// If modifying these scopes, delete your previously saved credentials
// at ~/.credentials/drive-nodejs-quickstart.json
var SCOPES = ['https://www.googleapis.com/auth/drive.metadata.readonly'];
var TOKEN_DIR = (process.env.HOME || process.env.HOMEPATH ||
	process.env.USERPROFILE) + '/.credentials/';
var TOKEN_PATH = TOKEN_DIR + 'media.json';

/* GET home page. */
router.get('/', function (req, res, next) {
	console.log('foo');
	//res.render('index', { title: 'media' });

	// Load client secrets from a local file.
	fs.readFile('client_secret.json', function processClientSecrets(err, content) {
		if (err) {
			console.log('Error loading client secret file: ' + err);
			return;
		}
		// Authorize a client with the loaded credentials, then call the
		// Drive API.
		authorize(JSON.parse(content), listFiles, res);
	});

});

module.exports = router;




/**
 * Create an OAuth2 client with the given credentials, and then execute the
 * given callback function.
 *
 * @param {Object} credentials The authorization client credentials.
 * @param {function} callback The callback to call with the authorized client.
 */
function authorize(credentials, callback, res) {
	var clientSecret = credentials.installed.client_secret;
	var clientId = credentials.installed.client_id;
	var redirectUrl = credentials.installed.redirect_uris[0];
	var auth = new googleAuth();
	var oauth2Client = new auth.OAuth2(clientId, clientSecret, redirectUrl);

	// Check if we have previously stored a token.
	console.log('tp = ' + TOKEN_PATH);
	fs.readFile(TOKEN_PATH, function (err, token) {
		if (err) {
			return getNewToken(oauth2Client, callback, res);
		} else {
			oauth2Client.credentials = JSON.parse(token);
			callback(oauth2Client, res);
			//console.log("authorize return = " + rtn + " extra " + rtn.id);
			return;
		}
	});
}

/**
 * Get and store new token after prompting for user authorization, and then
 * execute the given callback with the authorized OAuth2 client.
 *
 * @param {google.auth.OAuth2} oauth2Client The OAuth2 client to get token for.
 * @param {getEventsCallback} callback The callback to call with the authorized
 *     client.
 */
function getNewToken(oauth2Client, callback, res) {
	var authUrl = oauth2Client.generateAuthUrl({
		access_type: 'offline',
		scope: SCOPES
	});
	console.log('Authorize this app by visiting this url: ', authUrl);
	var rl = readline.createInterface({
		input: process.stdin,
		output: process.stdout
	});
	rl.question('Enter the code from that page here: ', function (code) {
		rl.close();
		oauth2Client.getToken(code, function (err, token) {
			if (err) {
				console.log('Error while trying to retrieve access token', err);
				return;
			}
			oauth2Client.credentials = token;
			storeToken(token);
			callback(oauth2Client, res);
		});
	});
}

/**
 * Store token to disk be used in later program executions.
 *
 * @param {Object} token The token to store to disk.
 */
function storeToken(token) {
	try {
		fs.mkdirSync(TOKEN_DIR);
	} catch (err) {
		if (err.code != 'EEXIST') {
			throw err;
		}
	}
	fs.writeFile(TOKEN_PATH, JSON.stringify(token));
	console.log('Token stored to ' + TOKEN_PATH);
}


var files;

/**
 * Lists the files 
 *
 * @param {google.auth.OAuth2} auth An authorized OAuth2 client.
 */
function listFiles(auth, res) {
	var service = google.drive('v3');
	service.files.list({
		auth: auth,
		pageSize: 1000,
		q: "'1lX9ViPsf-PJIeHA7lDONFt4kz1D6zu3s' in parents",
		fields: "nextPageToken, files(id, name, mimeType, description, size, videoMediaMetadata)"
	}, function (err, response) {
		if (err) {
			console.log('The API returned an error: ' + err);
			return;
		}
		//var files = response.files;
		files = response.files;
		if (files.length == 0) {
			console.log('No files found.');
		} else {
			//filelist = files;
			console.log('Files:');
			for (var i = 0; i < files.length; i++) {
				var file = files[i];
				//filelist.insert(file);
				//console.log('%s (%s)', file.name, file.id);
				console.log(util.inspect(file, { showHidden: false, depth: null }))
			}
			//console.log("listFiles files = " + files);
			console.log("files = " + files + " other " + files.length)
			res.render('tableMedia', { Datarows: files })
			return;
		}
	});
}
