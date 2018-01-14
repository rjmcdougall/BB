var express = require('express');
var router = express.Router();

/* GET users listing. */
router.get('/', function (req, res, next) {
	res.send('respond with a resource');
});

router.post('/Auth', async function (req, res, next) {

	var JWT = req.headers['x-access-token'];
	var results = [];

	UserStore = require('./UserStore');
	try{
		results.push(await UserStore.verifyJWT(JWT));
		res.status(200).send(results);
	}
	catch(err){
		res.status(401).send(err.message.substr(0,30) + "... Please Try Again.");
	}

});

module.exports = router;
