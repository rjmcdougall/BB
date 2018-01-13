var express = require('express');
var router = express.Router();

/* GET users listing. */
router.get('/', function (req, res, next) {
	res.send('respond with a resource');
});

router.post('/Auth', function (req, res, next) {

	var JWT = req.headers['x-access-token'];

	var GoogleAuth = require('google-auth-library');
	var auth = new GoogleAuth;
	var client = new auth.OAuth2(process.env.CLIENT_ID, '', '');
	client.verifyIdToken(
		JWT,
		process.env.CLIENT_ID,
		function (e, login) {
			if(e){
				res.status(401).send(e.message.substr(0,30) + "... Please Try Again.");
			}
			else {
				var payload = login.getPayload();
				var userid = payload['sub'];
				// If request specified a G Suite domain:
				//var domain = payload['hd'];
				var i = 1;
				res.status(200).send(JWT);
			}
		});
});
module.exports = router;
