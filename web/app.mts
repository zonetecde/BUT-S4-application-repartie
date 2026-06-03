import { recupererVelibsNancy } from "./velibs.js";
import { Incident, recupererIncidentsNancy } from "./incidents.js";

// On commence par récupérer les coordonnées de Nancy via l'API https://adresse.data.gouv.fr/outils/api-doc/adresse
const url = "https://data.geopf.fr/geocodage/search?q=Nancy&limit=1";

// L existe car il est definit dans le Html, mais TypeScript ne le sait pas
declare const L: any;

// On fetch la réponse de l'API
fetch(url)
    .then((response) => response.json())
    .then(async (data) => {
        // On récupère les coordonnées de Nancy depuis la réponse de l'API
        const lon = data.features[0].geometry.coordinates[0];
        const lat = data.features[0].geometry.coordinates[1];

        console.log(`Coordonnées de Nancy : lat=${lat}, lon=${lon}`);

        // On initialise la carte avec les coordonnées de Nancy
        let map = L.map("map").setView([lat, lon], 13);

        // On ajoute les tuiles sur la carte
        L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        }).addTo(map);

        // On récupère maintenant les informations sur les stations de velibs à Nancy
        const stations = await recupererVelibsNancy();

        // Ajoute les stations sur la carte
        // On met un emoji de vélo pour les stations de velibs
        const bikeIcon = L.divIcon({
            html: '<div style="font-size: 24px; text-align: center;">🚲</div>',
            className: "bike-icon",
            iconSize: [24, 24],
            iconAnchor: [12, 12],
            popupAnchor: [0, -12],
        });

        stations.forEach((station) => {
            const marker = L.marker([station.lat, station.lon], { icon: bikeIcon }).addTo(map);
            marker.bindPopup(`<b>${station.nom}</b><br>Adresse : ${station.adresse}<br>Vélos disponibles : ${station.velosDisponibles}<br>Places de parking libres : ${station.placesVoitureDisponibles}`);
        });

        // On ajoute maintenant les incidents sur la carte
        // On met un emoji de warning pour les incidents
        const warningIcon = L.divIcon({
            html: '<div style="font-size: 24px; text-align: center;">⚠️</div>',
            className: "warning-icon",
            iconSize: [24, 24],
            iconAnchor: [12, 12],
            popupAnchor: [0, -12],
        });

        // Ajoute les incidents sur la carte
        const incidents = await recupererIncidentsNancy();
        incidents.forEach((incident: Incident) => {
            const marker = L.marker([incident.lat, incident.lon], { icon: warningIcon }).addTo(map);
            marker.bindPopup(`<b>${incident.type}</b><br>Description : ${incident.description}`);
        });
    });
