import React, { Component } from "react";
import MediaList from "./MediaList";
import GlobalMenu from "./GlobalMenu";
import BoardGrid from "./BoardGrid";
import BatteryHistoryGrid from "./BatteryHistoryGrid";
import GoogleDriveMediaPicker from "./GoogleDriveMediaPicker";
import ManageMediaGrid from "./ManageMediaGrid";
import ProfileGrid from "./ProfileGrid";
import AddProfile from "./AddProfile";
import SetActiveProfile from "./SetActiveProfile";

export default class BBApp extends Component {

	constructor(props) {
		super(props);

		this.state = {
			currentAppBody: "none",
			currentBoard: "Select Board",
			currentProfile: "Select Profile",
			currentProfileIsGlobal: false,
			activeProfiles: [{ profile: "1", isGlobal: true }, { profile: "2", isGlobal: true }],
			currentSelection: "",
			drawerIsOpen: false,
			globalDrawerIsOpen: false,
			activateOpenSnackbar: false,
			activateResultsMessage: "",
			createProfileOpenSnackbar: false,
			createProfileResultsMessage: "",
			createProfileBoardName: "GLOBAL",
			profileDeleteSnackbarOpen: false,
			profileDeleteResultsMessage: "",
			profileSelected: "",
			createProfileBoardCloneProfileName: "NONE - NONE",
			boardNames: [{ board_name: "loading boards..." }],
			profileNames: ["select Board"],
			globalProfileNames: ["select Board"],
		};

		this.onSelectAppBody = this.onSelectAppBody.bind(this);
		this.onSelectBoard = this.onSelectBoard.bind(this);
		this.onSelectProfile = this.onSelectProfile.bind(this);
		this.onActivateProfile = this.onActivateProfile.bind(this);
		this.onDeactivateProfile = this.onDeactivateProfile.bind(this);
		this.reloadOnExpiration = this.props.reloadOnExpiration.bind(this);
		this.toggleDrawer = this.toggleDrawer.bind(this);
		this.toggleGlobalDrawer = this.toggleGlobalDrawer.bind(this);
		this.handleProfileDeleteClose = this.handleProfileDeleteClose.bind(this);
		this.handleProfileAddClose = this.handleProfileAddClose.bind(this);
		this.handleActivateProfileClose = this.handleActivateProfileClose.bind(this);
		this.handleCreateProfile = this.handleCreateProfile.bind(this);
		this.handleProfileClick = this.handleProfileClick.bind(this);
		this.onProfileDelete = this.onProfileDelete.bind(this);
		this.handleChange = this.handleChange.bind(this);
	}

	async componentDidMount() {
		const API = "/boards";

		try {
			var response = await fetch(API, {
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"authorization": window.sessionStorage.JWT,
				}
			});
			var jsonResponse = await response.json();
			this.setState({
				boardNames: jsonResponse.map(item => ({
					board_name: item.name,
					type: item.type
				}))
			});
		}
		catch (error) {
			this.setState({ error });
		}
	}

	toggleDrawer(open) {
		this.setState({
			drawerIsOpen: open,
		});
	}

	toggleGlobalDrawer(open) {
		this.setState({
			globalDrawerIsOpen: open,
		});
	}

	handleProfileDeleteClose() {
		this.reloadOnExpiration();

		this.setState({ profileDeleteSnackbarOpen: false });
	}

	handleProfileAddClose() {
		this.reloadOnExpiration();

		this.setState({ createProfileOpenSnackbar: false });
	}

	handleActivateProfileClose() {
		this.reloadOnExpiration();

		this.setState({ activateOpenSnackbar: false });
	}


	async handleCreateProfile(event) {

		this.reloadOnExpiration();

		var comp = this;

		console.log("state: ", JSON.stringify(this.state));

		var boardID;
		if (this.state.createProfileBoardName != null)
			boardID = this.state.createProfileBoardName.trim();
		else
			boardID = "GLOBAL";

		var profileID = this.state.createProfileName.trim();
		var API = "";

		var cloneFromBoardID = this.state.createProfileBoardCloneProfileName.substring(0, this.state.createProfileBoardCloneProfileName.indexOf(" - "));
		var cloneFromProfileID = this.state.createProfileBoardCloneProfileName.substring(this.state.createProfileBoardCloneProfileName.indexOf(" - ") + 3);

		if (boardID !== "GLOBAL")
			API = "/boards/" + boardID + "/profiles/" + profileID;
		else
			API = "/profiles/" + profileID;

		console.log("API TO CREATE PROFILE : " + API);

		try {
			var res = await fetch(API, {
				method: "POST",
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"Authorization": window.sessionStorage.JWT,
				},
				body: JSON.stringify({
					cloneFromBoardID: cloneFromBoardID,
					cloneFromProfileID: cloneFromProfileID,
				})
			});

			if (!res.ok) {
				var json = await res.json();
				comp.setState({
					createProfileOpenSnackbar: true,
					createProfileResultsMessage: JSON.stringify(json),
				});
			}
			else {
				var json2 = await res.json();
				comp.setState({
					createProfileOpenSnackbar: true,
					createProfileResultsMessage: JSON.stringify(json2),
				});
				return true;
			}
		}
		catch (error) {
			comp.setState({
				createProfileOpenSnackbar: true,
				createProfileResultsMessage: error.message
			});
			throw new Error(error);
		}
	}

	handleChange (event) {

		this.reloadOnExpiration();

		console.log("Set state due to form change: " + [event.target.name] + " " + event.target.value)
		this.setState({ [event.target.name]: event.target.value });

	}

	handleProfileClick(event, id) {

		this.reloadOnExpiration();

		let newSelected = id;
		this.setState({ profileSelected: newSelected });
	}

	async updateActiveProfiles(event, updateType) {
		this.reloadOnExpiration();

		try {
			var API;
			if (updateType === "activate")
				API = "/boards/" + this.state.currentBoard + "/activeProfile/" + this.state.currentProfile + "/isGlobal/" + this.state.currentProfileIsGlobal;
			else
				API = "/boards/" + this.state.currentBoard + "/deactiveProfile/" + this.state.currentProfile + "/isGlobal/" + this.state.currentProfileIsGlobal;

			console.log("API TO SET BOARD " + updateType + " " + API);

			var res = await fetch(API, {
				method: "POST",
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"Authorization": window.sessionStorage.JWT,
				},
			});

			var data = await res.json();
			console.log("data after post")
			console.log(data)
			var activeProfiles = [{
				profile: data[0].profile,
				isProfileGlobal: data[0].isProfileGlobal
			},
			{
				profile: data[0].profile2,
				isProfileGlobal: data[0].isProfileGlobal2
			},
			];

			console.log(activeProfiles);
			this.setState({
				activeProfiles: activeProfiles,
				activateOpenSnackbar: true,
				activateResultsMessage: this.state.currentProfile + " " + updateType + "d",
			});
		}
		catch (error) {
			console.log("error : " + error);
			this.setState({
				activateOpenSnackbar: true,
				activateResultsMessage: error.message
			});
		}
	}
	async onDeactivateProfile(event) {
		await this.updateActiveProfiles(event, "deactivate");
	}

	async onActivateProfile(event) {
		await this.updateActiveProfiles(event, "activate");
	}

	async onProfileDelete() {

		this.reloadOnExpiration();

		try {
			var comp = this;
			var profileSelected = this.state.profileSelected.toString();
			var profileID = profileSelected.slice(profileSelected.indexOf("-") + 1);
			var boardID = profileSelected.slice(0, profileSelected.indexOf("-"));

			var API = "";
			if (boardID !== "null")
				API = "/boards/" + boardID + "/profiles/" + profileID;
			else
				API = "/profiles/" + profileID;

			console.log("delete API : " + API);

			var res = await fetch(API, {
				method: "DELETE",
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"Authorization": window.sessionStorage.JWT,
				}
			});

			var json = await res.json();

			if (!res.ok) {
				console.log("error : " + JSON.stringify(json));
				this.setState({
					profileDeleteSnackbarOpen: true,
					profileDeleteResultsMessage: JSON.stringify(json),
				});
			}
			else {
				console.log("success : " + JSON.stringify(json));
				this.setState({
					profileDeleteSnackbarOpen: true,
					profileDeleteResultsMessage: JSON.stringify(json),
					profileSelected: "",
				});
			}
		}
		catch (error) {
			console.log("error : " + error);
			comp.setState({
				profileDeleteSnackbarOpen: true,
				profileDeleteResultsMessage: error.message
			});
		}
	}

	async onSelectAppBody(event, key) {
		this.reloadOnExpiration();

		this.setState({
			currentAppBody: key,
			drawerIsOpen: false,
			globalDrawerIsOpen: false,
		});
	}
	async onSelectBoard(event, key) {
		this.reloadOnExpiration();

		try {
			var selectedBoard = key.slice(6);

			var response, API;
			var profiles, globalProfiles, activeProfiles;
			var data;

			// get active profiles
			API = "/boards/" + selectedBoard;
			response = await fetch(API, {
				method: "GET",
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"Authorization": window.sessionStorage.JWT,
				},
			});
			
			data = await response.json();

			activeProfiles = [{
				profile: data[0].profile,
				isProfileGlobal: data[0].isProfileGlobal
			},
			{
				profile: data[0].profile2,
				isProfileGlobal: data[0].isProfileGlobal2
			},
			];

			console.log("actie profiles")
			console.log(activeProfiles)

			//get a list of available profiles
			API = "/boards/" + selectedBoard + "/profiles/";
			response = await fetch(API, {
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"authorization": window.sessionStorage.JWT,
				}
			});
			data = await response.json();
			profiles = data.map(item => ({
				profile_name: item.name,
			}));
			API = "/profiles/";
			response = await fetch(API, {
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"authorization": window.sessionStorage.JWT,
				}
			});
			data = await response.json();
			globalProfiles = data.map(item => ({
				profile_name: item.name,
			}));

			this.setState({
				currentBoard: selectedBoard,
				activeProfiles: activeProfiles,
				profileNames: profiles,
				globalProfileNames: globalProfiles,
			});
		}
		catch (error) {
			console.log(error);
		}
	}

	async onSelectProfile(event, key) {
		this.reloadOnExpiration();

		if (key.startsWith("profile-")) {
			this.setState({
				currentProfile: key.slice(8),
				currentProfileIsGlobal: false
			});
		}
		else {
			this.setState({
				currentProfile: key.slice(14),
				currentProfileIsGlobal: true
			});
		}
	}

	render() {

		let appBody = null;

		switch (this.state.currentAppBody) {
			case "AppBody-CurrentStatuses":
				appBody = <BoardGrid />;
				break;
			case "AppBody-BatteryHistory":
				appBody = <BatteryHistoryGrid currentBoard={this.state.currentBoard} />;
				break;
			case "AppBody-ReorderAudio":
				if (this.state.currentProfileIsGlobal)
					appBody = <MediaList key="audio1" mediaType="audio" currentProfile={this.state.currentProfile} />;
				else
					appBody = <MediaList key="audio2" mediaType="audio" currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
				break;
			case "AppBody-ReorderVideo":
				if (this.state.currentProfileIsGlobal)
					appBody = <MediaList key="video1" mediaType="video" currentProfile={this.state.currentProfile} />;
				else
					appBody = <MediaList key="video2" mediaType="video" currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
				break;
			case "AppBody-LoadFromGDrive":
				if (this.state.currentProfileIsGlobal)
					appBody = <GoogleDriveMediaPicker currentProfile={this.state.currentProfile} />;
				else
					appBody = <GoogleDriveMediaPicker currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
				break;
			case "AppBody-ManageMedia":
				if (this.state.currentProfileIsGlobal)
					appBody = <ManageMediaGrid currentProfile={this.state.currentProfile} />;
				else
					appBody = <ManageMediaGrid currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
				break;
			case "AppBody-ManageProfiles":
				appBody = <ProfileGrid handleProfileDeleteClose={this.handleProfileDeleteClose} profileSelected={this.state.profileSelected} handleProfileClick={this.handleProfileClick} onProfileDelete={this.onProfileDelete} profileDeleteSnackbarOpen={this.state.profileDeleteSnackbarOpen} profileDeleteResultsMessage={this.state.profileDeleteResultsMessage} />;
				break;
			case "AppBody-AddProfile":
				appBody = <AddProfile createProfileBoardCloneProfileName={this.state.createProfileBoardCloneProfileName} handleProfileAddClose={this.handleProfileAddClose} createProfileBoardName={this.state.createProfileBoardName} handleChange={this.handleChange} handleCreateProfile={this.handleCreateProfile} createProfileOpenSnackbar={this.state.createProfileOpenSnackbar} createProfileResultsMessage={this.state.createProfileResultsMessage} />;
				break;
			case "AppBody-ActivateProfile":
				appBody = <SetActiveProfile handleActivateProfileClose={this.handleActivateProfileClose}
					onActivateProfile={this.onActivateProfile}
					onDeactivateProfile={this.onDeactivateProfile}
					activateResultsMessage={this.state.activateResultsMessage}
					activateOpenSnackbar={this.state.activateOpenSnackbar}
					currentBoard={this.state.currentBoard}
					currentProfile={this.state.currentProfile}
					currentProfileIsGlobal={this.state.currentProfileIsGlobal}
					activeProfiles={this.state.activeProfiles}
				/>;
				break;

			default:
				if (this.state.currentBoard !== "Select Board") {
					appBody = <div style={{
						"backgroundColor": "lightblue",
						"margin": "1cm 1cm 1cm 1cm",
						"padding": "10px 5px 15px 20px"
					}}><div>You selected {this.state.currentBoard}.</div><div>Board-specific options available.</div><div>Select a profile to manage media. The * indicates the active profile on {this.state.currentBoard}.</div></div>;
				}
				else {
					appBody = <div style={{
						"backgroundColor": "lightblue",
						"margin": "1cm 1cm 1cm 1cm",
						"padding": "10px 5px 15px 20px"
					}}><div>Global options available.</div> <div>Please select a board for board-specific options.</div></div>;
				}
				break;
		}

		return (
			<div className="BBApp" style={{ margin: 0 }}>


				<GlobalMenu currentBoard={this.state.currentBoard}
					drawerIsOpen={this.state.drawerIsOpen}
					globalDrawerIsOpen={this.state.globalDrawerIsOpen}
					currentAppBody={this.state.currentAppBody}
					currentProfile={this.state.currentProfile}
					onSelectBoard={this.onSelectBoard}
					onSelectAppBody={this.onSelectAppBody}
					onSelectProfile={this.onSelectProfile}
					activeProfiles={this.state.activeProfiles}
					toggleDrawer={this.toggleDrawer}
					toggleGlobalDrawer={this.toggleGlobalDrawer}
					boardNames={this.state.boardNames}
					profileNames={this.state.profileNames}
					globalProfileNames={this.state.globalProfileNames}
				/>
				{/* <MuiThemeProvider theme={readableText}>
            <Typography>*/}
				{appBody}
				{/*  </Typography>
        </MuiThemeProvider> */}
			</div>
		);
	}
}
