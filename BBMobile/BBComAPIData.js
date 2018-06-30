
exports.fetchBoards = async function () {

	
	//const API = "http://www.fakeresponse.com/api/?sleep=5";
	const API = "https://www.burnerboard.com/boards/";
	try {
		var response = await fetch(API, {
			headers: {
				"Accept": "application/json",
				"Content-Type": "application/json",
			}
		});
		var boards = await response.json();

		return boards;

	}
	catch (error) {
		console.log(error);
		return null;
	}
}