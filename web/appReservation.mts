import { recupererRestoNancy } from "./restaurants.js";

// Récupération des coordonnées de Nancy via l'API https://adresse.data.gouv.fr/outils/api-doc/adresse
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

        
        // On récupère les informations sur les restaurants à Nancy
        const restaurants = await recupererRestoNancy();

        // On ajoute les restaurants sur la carte
        restaurants.forEach((resto) => {
            const marker = L.marker([resto.lat, resto.lon]).addTo(map);
            marker.bindPopup(`<b>${resto.nom}</b><br>Adresse : ${resto.adresse}`);
        });
        
    });
