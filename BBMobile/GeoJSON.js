import React from "react";
import { View } from "react-native";
import MapView from "react-native-maps";
import uuid from "uuid";
import PropTypes from "prop-types";

export const makeOverlays = features => {
	const points = features
		.filter(f => f.geometry && (f.geometry.type === "Point" || f.geometry.type === "MultiPoint"))
		.map(feature => makeCoordinates(feature).map(coordinates => makeOverlay(coordinates, feature)))
		.reduce(flatten, [])
		.map(overlay => ({ ...overlay, type: "point" }));

	const lines = features
		.filter(
			f => f.geometry && (f.geometry.type === "LineString" || f.geometry.type === "MultiLineString")
		)
		.map(feature => makeCoordinates(feature).map(coordinates => makeOverlay(coordinates, feature)))
		.reduce(flatten, [])
		.map(overlay => ({ ...overlay, type: "polyline" }));

	const multipolygons = features
		.filter(f => f.geometry && f.geometry.type === "MultiPolygon")
		.map(feature => makeCoordinates(feature).map(coordinates => makeOverlay(coordinates, feature)))
		.reduce(flatten, []);

	const polygons = features
		.filter(f => f.geometry && f.geometry.type === "Polygon")
		.map(feature => makeOverlay(makeCoordinates(feature), feature))
		.reduce(flatten, [])
		.concat(multipolygons)
		.map(overlay => ({ ...overlay, type: "polygon" }));

	const overlays = points.concat(lines).concat(polygons);

	return overlays;
};

const flatten = (prev, curr) => prev.concat(curr);

const makeOverlay = (coordinates, feature) => {
	let overlay = {
		feature,
		id: feature.id ? feature.id : uuid(),
		title: feature.properties.name ? feature.properties.name : null,
	};
	if (feature.geometry.type === "Polygon" || feature.geometry.type === "MultiPolygon") {
		overlay.coordinates = coordinates[0];
		if (coordinates.length > 1) {
			overlay.holes = coordinates.slice(1);
		}
	} else {
		overlay.coordinates = coordinates;
	}
	return overlay;
};

const makePoint = c => {

	const expectedManLat = 40.7866;
	const expectedManLon = -119.20660000000001;

	var latitude =  c[1] - expectedManLat + actualManLat;
	var longitude = c[0] - expectedManLon + actualManLon;
	return ({
		latitude: latitude,
		longitude: longitude
	});
};

const makeLine = l => l.map(makePoint);

var actualManLat = 0;
var actualManLon = 0;

const makeCoordinates = feature => {
	const g = feature.geometry;
 
	if (g.type === "Point") {
		return [makePoint(g.coordinates)];
	} else if (g.type === "MultiPoint") {
		return g.coordinates.map(makePoint);
	} else if (g.type === "LineString") {
		return [makeLine(g.coordinates)];
	} else if (g.type === "MultiLineString") {
		return g.coordinates.map(makeLine);
	} else if (g.type === "Polygon") {
		return g.coordinates.map(makeLine);
	} else if (g.type === "MultiPolygon") {
		return g.coordinates.map(p => p.map(makeLine));
	} else {
		return [];
	}
};

const Geojson = props => {

	actualManLat = props.userPrefs.man.latitude;
	actualManLon = props.userPrefs.man.longitude;

	const overlays = makeOverlays(props.geojson.features);
	return (
		<View>
			{overlays.map(overlay => {
				if (overlay.type === "point") {
					return (
						<MapView.Marker
							key={overlay.id}
							coordinate={overlay.coordinates}
							pinColor={props.pinColor}
							title={overlay.title}
						/>
					);
				}
				if (overlay.type === "polygon") {
					return (
						<MapView.Polygon
							key={overlay.id}
							coordinates={overlay.coordinates}
							holes={overlay.holes}
							strokeColor={props.strokeColor}
							fillColor={props.fillColor}
							strokeWidth={props.strokeWidth}
						/>
					);
				}
				if (overlay.type === "polyline") {
					return (
						<MapView.Polyline
							key={overlay.id}
							coordinates={overlay.coordinates}
							strokeColor={props.strokeColor}
							strokeWidth={props.strokeWidth}
							title={overlay.ref}
						/>
					);
				}
			})}
		</View>
	);
};

Geojson.propTypes = {
	geojson: PropTypes.object,
	userPrefs: PropTypes.object,
	title: PropTypes.string,
	pinColor: PropTypes.string,
	strokeColor: PropTypes.string,
	fillColor: PropTypes.string,
	strokeWidth: PropTypes.number,
};


export default Geojson;
