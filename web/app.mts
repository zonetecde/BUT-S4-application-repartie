import { recupererVelibsNancy, StationVelib } from "./velibs.js";
import { Incident, recupererIncidentsNancy } from "./incidents.js";
import { recupererRestoNancy, RestaurantResponse } from "./restaurants.js";

// On commence par récupérer les coordonnées de Nancy via l'API https://adresse.data.gouv.fr/outils/api-doc/adresse
const url = "https://data.geopf.fr/geocodage/search?q=Nancy&limit=1";

// L existe car il est definit dans le Html, mais TypeScript ne le sait pas
declare const L: any;

let incidents: Incident[] = [];
let velibs: StationVelib[] = [];
let restaurants: RestaurantResponse[] = [];
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
            html: '<div style="width: 24px; height: 24px; font-size: 14px; line-height: 20px; text-align: center; background: #bfdbfe; border: 2px solid #1d4ed8; border-radius: 50%;">🚲</div>',
            className: "",
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
            html: '<div style="width: 24px; height: 24px; font-size: 14px; line-height: 20px; text-align: center; background: #bbf7d0; border: 2px solid #15803d; border-radius: 50%;">🍽️</div>',
            className: "",
            iconSize: [24, 24],
            iconAnchor: [12, 12],
            popupAnchor: [0, -12],
        });

        restaurants.forEach((resto) => {
            const marker = L.marker([resto.lat, resto.lon], { icon: foodIcon }).addTo(map);
            marker.bindPopup(`<b>${resto.nom}</b><br>Adresse : ${resto.adresse}`);

            marker.on("click", (event: any) => {
                console.log("Restaurant cliqué :", resto.nom, event);
                ouvrirReservation(resto);
            });
        });
    }

    // Regarde si la checkbox de filtre "filtre-incident" est en true
    const filtreIncident = (document.getElementById("filtre-incident") as HTMLInputElement).checked;

    if (filtreIncident) {
        // On ajoute maintenant les incidents sur la carte
        // On met un emoji de warning pour les incidents
        const warningIcon = L.divIcon({
            html: '<div style="width: 24px; height: 24px; font-size: 14px; line-height: 20px; text-align: center; background: #fecaca; border: 2px solid #b91c1c; border-radius: 50%;">⚠️</div>',
            className: "",
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

let currentRestaurant: RestaurantResponse | null = null;
let currentSelectedTable: { id: number; capacity: number; location: string } | null = null;

function ouvrirReservation(resto: RestaurantResponse) {
    currentRestaurant = resto;
    currentSelectedTable = null;

    const filtresDiv = document.getElementById("filtres");
    if (filtresDiv) {
        filtresDiv.style.display = "none";
    }

    const reservationForm = document.getElementById("reservation-form");
    if (reservationForm) {
        reservationForm.style.display = "block";
    }

    const restaurantName = document.getElementById("restaurant-name");
    if (restaurantName) {
        restaurantName.innerText = resto.nom;
    }

    const etape1 = document.getElementById("reservation-etape-1");
    const tablesDiv = document.getElementById("reservation-tables");
    const detailsDiv = document.getElementById("reservation-details");
    if (etape1) {
        etape1.style.display = "block";
    }
    if (tablesDiv) {
        tablesDiv.style.display = "none";
    }
    if (detailsDiv) {
        detailsDiv.style.display = "none";
    }

    const listeTables = document.getElementById("liste-tables");
    if (listeTables) {
        listeTables.innerHTML = "";
    }
    const tableSelectionnee = document.getElementById("table-selectionnee");
    if (tableSelectionnee) {
        tableSelectionnee.innerText = "Aucune table sélectionnée";
    }
}

let currentAvailableTables: Array<{ id: number; capacity: number }> = [];

async function chercherTablesDispo() {
    if (!currentRestaurant) {
        alert("Sélectionner d'abord un restaurant.");
        return;
    }

    const personnes = parseInt((document.getElementById("reservation-personnes") as HTMLInputElement).value, 10);
    const dateHeure = (document.getElementById("reservation-date") as HTMLInputElement).value;

    if (!dateHeure || !personnes || personnes < 1) {
        alert("Il faut indiquer une date/heure valide et le nombre de personnes.");
        return;
    }

    const params = new URLSearchParams({
        nomRestaurant: currentRestaurant.nom,
        dateHeure,
        nombreConvives: personnes.toString(),
    });
    const url = `http://localhost:8081/api/restaurants/tables?${params.toString()}`;

    const listeTables = document.getElementById("liste-tables");
    if (!listeTables) {
        return;
    }

    try {
        const response = await fetch(url);
        const json = await response.json();

        if (!response.ok || !json.success) {
            listeTables.innerHTML = `<p class="text-red-600">${json.message || "Impossible de récupérer les tables disponibles."}</p>`;
            currentAvailableTables = [];
        } else {
            const data = json.data as Record<string, number>;
            currentAvailableTables = Object.entries(data).map(([id, capacity]) => ({
                id: Number(id),
                capacity: Number(capacity),
            }));

            listeTables.innerHTML = "";
            if (currentAvailableTables.length === 0) {
                listeTables.innerHTML = "<p>Aucune table disponible pour ce créneau.</p>";
            } else {
                currentAvailableTables.forEach((table) => {
                    const button = document.createElement("button");
                    button.type = "button";
                    button.className = "bg-blue-200 text-left px-3 py-2 rounded border border-blue-300 hover:bg-blue-300";
                    button.innerText = `Table ${table.id} — ${table.capacity} personnes`;
                    button.onclick = () => selectTable(table.id);
                    listeTables.appendChild(button);
                });
            }
        }
    } catch (error) {
        listeTables.innerHTML = "<p class=\"text-red-600\">Erreur de connexion au serveur.</p>";
        currentAvailableTables = [];
    }

    const tablesDiv = document.getElementById("reservation-tables");
    if (tablesDiv) {
        tablesDiv.style.display = "block";
    }

    const detailsDiv = document.getElementById("reservation-details");
    if (detailsDiv) {
        detailsDiv.style.display = "none";
    }
}

function selectTable(tableId: number) {
    const table = currentAvailableTables.find((t) => t.id === tableId);
    if (!table) {
        return;
    }

    currentSelectedTable = table;

    const tableSelectionnee = document.getElementById("table-selectionnee");
    if (tableSelectionnee) {
        tableSelectionnee.innerText = `Table ${table.id} — ${table.capacity} personnes`;
    }

    const detailsDiv = document.getElementById("reservation-details");
    if (detailsDiv) {
        detailsDiv.style.display = "block";
    }
}

async function envoyerReservation() {
    if (!currentRestaurant || !currentSelectedTable) {
        alert("Veuillez sélectionner d'abord une table disponible.");
        return;
    }

    const prenom = (document.getElementById("reservation-prenom") as HTMLInputElement).value.trim();
    const nom = (document.getElementById("reservation-nom") as HTMLInputElement).value.trim();
    const telephone = (document.getElementById("reservation-phone") as HTMLInputElement).value.trim();
    const personnes = parseInt((document.getElementById("reservation-personnes") as HTMLInputElement).value, 10);
    const dateHeure = (document.getElementById("reservation-date") as HTMLInputElement).value;

    if (!prenom || !nom) {
        alert("Veuillez indiquer le prénom et le nom de la personne qui réserve.");
        return;
    }

    const params = new URLSearchParams({
        nomRestaurant: currentRestaurant.nom,
        idTable: currentSelectedTable.id.toString(),
        dateHeure,
        nom,
        prenom,
        nombreConvives: personnes.toString(),
        telephone,
    });
    const url = `http://localhost:8081/api/restaurants/reserver?${params.toString()}`;

    try {
        const response = await fetch(url, { method: "POST" });
        const json = await response.json();

        if (!response.ok || !json.success) {
            alert(`Erreur : ${json.message || "La réservation n'a pas pu être enregistrée."}`);
            return;
        }

        alert(`Réservation confirmée pour ${prenom} ${nom} au restaurant ${currentRestaurant.nom}\nTable ${currentSelectedTable.id} — ${currentSelectedTable.capacity} personnes\n${dateHeure}\nNombre de personnes : ${personnes}`);
        cacherActions();
    } catch (error) {
        alert("Erreur de connexion au serveur lors de l'enregistrement de la réservation.");
    }
}

(window as any).chercherTablesDispo = chercherTablesDispo;
(window as any).selectTable = selectTable;
(window as any).envoyerReservation = envoyerReservation;

(window as any).cacherActions = function () {
    const reservationForm = document.getElementById("reservation-form");
    if (reservationForm) {
        reservationForm.style.display = "none";
    }

    const filtresDiv = document.getElementById("filtres");
    if (filtresDiv) {
        filtresDiv.style.display = "block";
    }

    const listeTables = document.getElementById("liste-tables");
    if (listeTables) {
        listeTables.innerHTML = "";
    }

    const tablesDiv = document.getElementById("reservation-tables");
    if (tablesDiv) {
        tablesDiv.style.display = "none";
    }

    const detailsDiv = document.getElementById("reservation-details");
    if (detailsDiv) {
        detailsDiv.style.display = "none";
    }

    (document.getElementById("reservation-prenom") as HTMLInputElement).value = "";
    (document.getElementById("reservation-nom") as HTMLInputElement).value = "";
    (document.getElementById("reservation-phone") as HTMLInputElement).value = "";
    (document.getElementById("reservation-notes") as HTMLTextAreaElement).value = "";
    (document.getElementById("reservation-date") as HTMLInputElement).value = "";
    (document.getElementById("reservation-personnes") as HTMLInputElement).value = "2";

    currentRestaurant = null;
    currentSelectedTable = null;
    currentAvailableTables = [];
};
