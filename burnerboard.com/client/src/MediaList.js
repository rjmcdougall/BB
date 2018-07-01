import React, { Component } from "react";
//import ReactDOM from "react-dom";
import { DragDropContext, Droppable, Draggable } from "react-beautiful-dnd";

// a little function to help us with reordering the result
const reorder = (list, startIndex, endIndex) => {
	const result = Array.from(list);
	const [removed] = result.splice(startIndex, 1);
	result.splice(endIndex, 0, removed);

	return result;
};

// using some little inline style helpers to make the app look okay
const grid = 8;
const getItemStyle = (draggableStyle, isDragging) => ({
	// some basic styles to make the items look a bit nicer
	userSelect: "none",
	padding: grid * 2,
	margin: `0 0 ${grid}px 0`,

	// change background colour if dragging
	background: isDragging ? "#002884" : "#757ce8",
	color: isDragging ? "white" : "black",

	// styles we need to apply on draggables
	...draggableStyle,
});
const getListStyle = isDraggingOver => ({
	background: isDraggingOver ? "lightgrey" : "lightgrey",
	padding: grid,
});

class MediaList extends Component {
	constructor(props) {
		super(props);
		this.state = {
			items: [{ "localName": "loading audio..." }],
			currentBoard: props.currentBoard,
			currentProfile: props.currentProfile,
			mediaType: props.mediaType,
		};
		this.onDragEnd = this.onDragEnd.bind(this);
		this.loadDD = this.loadDD.bind(this);

	}

	componentWillReceiveProps(nextProps) {

		this.setState({
			currentBoard: nextProps.currentBoard,
			currentProfile: nextProps.currentProfile,
			mediaType: nextProps.mediaType,
		}, this.loadDD);

	}

	componentDidMount() {

		this.loadDD();

	}

	loadDD() {
		var API;
		if (this.state.currentBoard != null)
			API = "/boards/" + this.state.currentBoard + "/profiles/" + this.state.currentProfile + "/DownloadDirectoryJSON";
		else
			API = "/profiles/" + this.state.currentProfile + "/DownloadDirectoryJSON";

		console.log("URL FOR MEDIA LIST: " + API);


		fetch(API, {
			headers: {
				"Accept": "application/json",
				"Content-Type": "application/json",
				"authorization": window.sessionStorage.JWT,
			}
		})
			.then(response => response.json())
			.then(data => {

				if (this.state.mediaType === "audio") {
					this.setState({
						items: data.audio.map(item => ({
							id: `${item.localName}`,
							content: ` ${item.friendlyName ? item.friendlyName : item.localName}`,
						}))
					});
				}
				else {
					console.log(data.video);
					this.setState({
						items: data.video.map(function (item) {
							if (item.localName != null)
								return {
									id: `${item.localName}`,
									content: `${item.friendlyName ? item.friendlyName : item.localName}`,
								};
							else {
								return {
									id: `${item.algorithm}`,
									content: `${item.friendlyName ? item.friendlyName : item.algorithm}`,
								};
							}
						})
					});
				}
			})
			.catch(error => this.setState({ error }));

	}

	onDragEnd(result) {
		// dropped outside the list
		if (!result.destination) {
			return;
		}

		const items = reorder(
			this.state.items,
			result.source.index,
			result.destination.index
		);

		this.setState({
			items,
		});

		var API;

		if (this.state.currentBoard != null)
			API = "/boards/" + this.state.currentBoard + "/profiles/" + this.state.currentProfile + "/" + this.state.mediaType + "/ReorderMedia";
		else
			API = "/profiles/" + this.state.currentProfile + "/" + this.state.mediaType + "/ReorderMedia";

		console.log("URL UPDATE MEDIA: " + API);

		var mediaArray = this.state.items.map(item => (
			item.id
		));

		fetch(API, {
			method: "POST",
			headers: {
				"Accept": "application/json",
				"Content-Type": "application/json",
				"Authorization": window.sessionStorage.JWT,
			},
			body: JSON.stringify({
				mediaArray: mediaArray,
				mediaType: this.state.mediaType,
			})
		}).then((res) => res.json())
			.catch((err) => console.log(err));

	}

	render() {

		return (
			<DragDropContext onDragEnd={this.onDragEnd}>
				<Droppable droppableId="droppable">
					{(provided, snapshot) => (
						<div
							ref={provided.innerRef}
							style={getListStyle(snapshot.isDraggingOver)}
						>
							{this.state.items.map(item => (
								<Draggable key={item.id} draggableId={item.id}>
									{(provided, snapshot) => (
										<div>
											<div
												ref={provided.innerRef}
												style={getItemStyle(
													provided.draggableStyle,
													snapshot.isDragging
												)}
												{...provided.dragHandleProps}
											>
												{item.content}
											</div>
											{provided.placeholder}
										</div>
									)}
								</Draggable>
							))}
							{provided.placeholder}
						</div>
					)}
				</Droppable>
			</DragDropContext>
		);
	}
}

export default MediaList;
