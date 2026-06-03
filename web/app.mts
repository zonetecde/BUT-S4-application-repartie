import { recupererVelibsNancy, StationVelib } from "./velibs.js";
import { Incident, recupererIncidentsNancy } from "./incidents.js";
import { recupererRestoNancy } from "./restaurants.js";

// On commence par récupérer les coordonnées de Nancy via l'API https://adresse.data.gouv.fr/outils/api-doc/adresse
const url = "https://data.geopf.fr/geocodage/search?q=Nancy&limit=1";

// L existe car il est definit dans le Html, mais TypeScript ne le sait pas
declare const L: any;

let incidents: Incident[] = [];
let velibs: StationVelib[] = [];
let restaurants: Awaited<ReturnType<typeof recupererRestoNancy>> = [];
let map: any; // Carte Leaflet

// On fetch la réponse de l'API
fetch(url)
    .then((response) => response.json())
    .then(async (data) => {
        // On récupère les coordonnées de Nancy depuis la réponse de l'API
        const lon = data.features[0].geometry.coordinates[0];
        const lat = data.features[0].geometry.coordinates[1];

        console.log(`Coordonnées de Nancy : lat=${lat}, lon=${lon}`);

        // On initialise la carte avec les coordonnées de Nancy
        map = L.map("map").setView([lat, lon], 13);

        // On ajoute les tuiles sur la carte
        L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        }).addTo(map);

        try {
            // On récupère maintenant les informations sur les stations de velibs à Nancy
            velibs = await recupererVelibsNancy();

            // On récupère les informations sur les restaurants à Nancy
            restaurants = await recupererRestoNancy();

            // Et on récupère aussi les incidents à Nancy
            incidents = await recupererIncidentsNancy();
        } catch (error: any) {
            // C'est un catch controllé : quand on travail sur nos machines,
            // recupererIncidentsNancy ne fonctionne pas car on a pas accès au proxy
            // java.
            console.error("Erreur lors de la récupération des données :", error);
        }

        // Affiche la carte
        updateMap();
    });

function updateMap() {
    // Clear tous les markers de la carte avant de les réafficher
    map.eachLayer((layer: any) => {
        if (layer instanceof L.Marker) {
            map.removeLayer(layer);
        }
    });

    // Regarde si la checkbox de filtre "filtre-velib" est en true
    const filtreVelib = (document.getElementById("filtre-velib") as HTMLInputElement).checked;

    if (filtreVelib) {
        // Ajoute les stations sur la carte
        // On met un emoji de vélo pour les stations de velibs
        const bikeIcon = L.divIcon({
            html: '<div style="font-size: 24px; text-align: center;">🚲</div>',
            iconSize: [24, 24],
            iconAnchor: [12, 12],
            popupAnchor: [0, -12],
        });

        velibs.forEach((station) => {
            const marker = L.marker([station.lat, station.lon], { icon: bikeIcon }).addTo(map);
            marker.bindPopup(`<b>${station.nom}</b><br>Adresse : ${station.adresse}<br>Vélos disponibles : ${station.velosDisponibles}<br>Places de parking libres : ${station.placesLibres}`);
        });
    }

    // Regarde si la checkbox de filtre "filtre-restaurant" est en true
    const filtreRestaurant = (document.getElementById("filtre-restaurant") as HTMLInputElement).checked;

    if (filtreRestaurant) {
        const foodIcon = L.divIcon({
            html: '<div style="font-size: 24px; text-align: center;">🍽️</div>',
            iconSize: [24, 24],
            iconAnchor: [12, 12],
            popupAnchor: [0, -12],
        });

        restaurants.forEach((resto) => {
            const marker = L.marker([resto.lat, resto.lon], { icon: foodIcon }).addTo(map);
            marker.bindPopup(`<b>${resto.nom}</b><br>Adresse : ${resto.adresse}`);
        });
    }

    // Regarde si la checkbox de filtre "filtre-incident" est en true
    const filtreIncident = (document.getElementById("filtre-incident") as HTMLInputElement).checked;

    if (filtreIncident) {
        // On ajoute maintenant les incidents sur la carte
        // On met un emoji de warning pour les incidents
        const warningIcon = L.divIcon({
            html: '<div style="font-size: 24px; text-align: center;">⚠️</div>',
            iconSize: [24, 24],
            iconAnchor: [12, 12],
            popupAnchor: [0, -12],
        });

        incidents.forEach((incident: Incident) => {
            const marker = L.marker([incident.lat, incident.lon], { icon: warningIcon }).addTo(map);
            marker.bindPopup(`<b>${incident.type}</b><br>Description : ${incident.description}`);
        });
    }
}

(window as any).updateMap = updateMap; // Expose la fonction updateMap pour qu'elle puisse être appelée depuis le HTML
