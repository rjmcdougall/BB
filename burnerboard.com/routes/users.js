var express = require("express");
var router = express.Router();

router.get("/", function (req, res, next) {
	res.status(400).send("Not Found");
});

router.post("/Auth", async function (req, res, next) {

	var JWT = req.headers["authorization"].replace("Bearer ","");
	var results = [];

	const UserStore = require("./UserStore");
	try {
		results.push(await UserStore.verifyJWT(JWT));
		res.status(200).send(results);
	}
	catch (err) {
		res.status(401).send(err.message.substr(0, 30) + "... Please Try Again.");
	}
});

module.exports = router;
