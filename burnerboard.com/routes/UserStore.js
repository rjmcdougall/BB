const constants = require("./Constants");
const { OAuth2Client } = require('google-auth-library');
const client = new OAuth2Client(constants.CLIENT_ID);
const Datastore = require("@google-cloud/datastore");
const datastore = new Datastore({
	projectId: constants.PROJECT_ID,
});

exports.verifyJWT = async function (JWT) {

	try {
		const ticket = await client.verifyIdToken({
			idToken: JWT,
			audience: constants.CLIENT_ID
		});
		const payload = ticket.getPayload();
		var email = payload.email;

		var emailExists = datastore.createQuery("user")
			.filter("email", "=", email);

		var results = await datastore.runQuery(emailExists);
		var emailFound = (results[0].length > 0);

		if (emailFound) {
			return JWT;
		}
		else {
			return new Error("Not Authorized for BurnerBoard.com");
		}
	}
	catch (error) {
		return error;
	}

};