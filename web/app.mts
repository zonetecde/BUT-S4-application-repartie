import { recupererVelibsNancy, StationVelib } from "./velibs.js";
import { Incident, recupererIncidentsNancy } from "./incidents.js";
import { recupererRestoNancy, RestaurantResponse, recupererTablesRestaurant, reserverTableRestaurant, recupererReservations, libererTablesRestaurant, TableDisponible } from "./restaurants.js";
import { recupererCrousNancy, chargerMenu, Restaurant as RestaurantCrous } from "./crous.js";
import { recupererPointsGeo, ajouterPointGeo, PointGeo } from "./points.js";

// Demande l'adresse du proxy au démarrage
const proxyHost = prompt("Adresse IP du proxy HTTP ?", "localhost");
const proxyPort = prompt("Port du proxy HTTP ?", "8081");
// Par défaut c'est localhost 8081
const proxyUrl = `http://${proxyHost || "localhost"}:${proxyPort || "8081"}`;

// On commence par récupérer les coordonnées de Nancy via l'API https://adresse.data.gouv.fr/outils/api-doc/adresse
const url = "https://data.geopf.fr/geocodage/search?q=Nancy&limit=1";

// L existe car il est definit dans le Html, mais TypeScript ne le sait pas
declare const L: any;

let incidents: Incident[] = [];
let velibs: StationVelib[] = [];
let restaurants: RestaurantResponse[] = [];
let crous: RestaurantCrous[] = [];
let points: PointGeo[] = [];
let map: any; // Carte Leaflet
let addPointMode: boolean = false;
let selectedEmoji: string = "📍";
let positionActuelMarker: any = null; // Marker de la position utilisateur actuelle
let reservationLockActif: boolean = false; // Indique si on a déjà récupéré les tables disponibles pour un restaurant (et donc qu'on lock le resto en cours)
let tablesDisponiblesEnCours: TableDisponible[] = [];
let dateHeureReservationEnCours: string | null = null;

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

            // On récupère les restaurants du CROUS à Nancy (bonus)
            crous = await recupererCrousNancy(proxyUrl);

            // On récupère les informations sur les restaurants à Nancy
            restaurants = await recupererRestoNancy(proxyUrl);

            // Et on récupère aussi les incidents à Nancy (via le proxy)
            incidents = await recupererIncidentsNancy(proxyUrl);

            // On récupère les points géographiques personnalisés (via le proxy)
            points = await recupererPointsGeo(proxyUrl);
        } catch (error: any) {
            console.error("Erreur lors de la récupération des données :", error);
        }

        // Affiche la carte
        updateMap();

        // Demande la position de l'utilisateur pour l'afficher sur la carte
        localiserUtilisateur();
    });

/**
 * Cache tous les panneaux d'action (form de réservation, menu crous, ...) et affiche seulement le panneau demandé en paramètre.
 * @param panelId L'id du panneau à afficher (ex: "reservation-form", "menu-crous", ...)
 */
function afficherPanneau(panelId: string): void {
    // Cache tous les panneaux
    const panels = ["filtres", "reservation-form", "menu-crous", "add-point-form"];
    for (const id of panels) {
        const el = document.getElementById(id);
        if (el) el.style.display = "none";
    }

    // Affiche le panneau demandé
    const panel = document.getElementById(panelId);
    if (panel) panel.style.display = "block";
}

function localiserUtilisateur() {
    navigator.geolocation.getCurrentPosition(
        (position) => {
            const { latitude, longitude } = position.coords;

            const userIcon = L.divIcon({
                html: '<div style="width: 28px; height: 28px; font-size: 16px; line-height: 24px; text-align: center; background: #dbeafe; border: 2px solid #2563eb; border-radius: 50%;">🧑</div>',
                className: "",
                iconSize: [28, 28],
                iconAnchor: [14, 14],
                popupAnchor: [0, -14],
            });

            positionActuelMarker = L.marker([latitude, longitude], { icon: userIcon }).addTo(map);
            positionActuelMarker.bindPopup("<b>Vous êtes ici</b>");
            positionActuelMarker.openPopup();
        },
        (error) => {
            console.log("Veuillez autoriser la géolocalisation pour afficher votre position sur la carte." + error);
        }
    );
}

function updateMap() {
    // Clear tous les markers de la carte avant de les réafficher
    map.eachLayer((layer: any) => {
        if (layer instanceof L.Marker && layer !== positionActuelMarker) {
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

            // Pour les restaurants, on veut afficher un form de réservation
            // de table lorsqu'on clique dessus.
            marker.on("click", async (event: any) => {
                if (reservationLockActif) {
                    await (window as any).cacherActions();
                }

                console.log("Restaurant cliqué :", resto.nom, event);

                afficherPanneau("reservation-form");

                // Met le nom du restaurant dans le formulaire de réservation
                const restaurantName = document.getElementById("restaurant-name") as HTMLSpanElement;
                if (restaurantName) {
                    restaurantName.innerText = resto.nom;
                }

                const tablesDiv = document.getElementById("tables-dispo");
                if (tablesDiv) {
                    tablesDiv.innerHTML = "";
                    tablesDiv.classList.add("hidden");
                }
                tablesDisponiblesEnCours = [];
            });
        });
    }

    // Regarde si la checkbox de filtre "filtre-crous" est en true
    const filtreCrous = (document.getElementById("filtre-crous") as HTMLInputElement).checked;

    if (filtreCrous) {
        const crousIcon = L.divIcon({
            html: '<div style="width: 24px; height: 24px; font-size: 14px; line-height: 20px; text-align: center; background: #fef08a; border: 2px solid #ca8a04; border-radius: 50%;">🎓</div>',
            className: "",
            iconSize: [24, 24],
            iconAnchor: [12, 12],
            popupAnchor: [0, -12],
        });

        crous.forEach((restoCrous) => {
            const marker = L.marker([restoCrous.latitude, restoCrous.longitude], { icon: crousIcon }).addTo(map);
            marker.bindPopup(`<b>${restoCrous.nom}</b><br>Adresse : ${restoCrous.adresse}${restoCrous.horaires ? `<br>Horaires : ${JSON.parse(restoCrous.horaires).join(", ")}` : ""}`);

            // Quand on clique sur un resto crous, on affiche son menu à droite
            marker.on("click", async () => {
                afficherPanneau("menu-crous");

                // Met le nom du restaurant
                const crousName = document.getElementById("crous-name");
                if (crousName) {
                    crousName.innerText = restoCrous.nom;
                }

                try {
                    const menu = await chargerMenu(proxyUrl, restoCrous.code);
                    const menuContent = document.getElementById("menu-content");
                    if (menuContent) {
                        menuContent.innerText = menu.menu || "Aucun menu disponible.";
                    }
                    const menuDate = document.getElementById("menu-date");
                    if (menuDate) {
                        menuDate.innerText = menu.date || "";
                    }
                } catch {
                    const menuContent = document.getElementById("menu-content");
                    if (menuContent) {
                        menuContent.innerText = "Impossible de charger le menu.";
                    }
                }
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

    // Affichage des points géographiques personnalisés
    const filtrePoints = (document.getElementById("filtre-points") as HTMLInputElement)?.checked ?? true;

    if (filtrePoints) {
        points.forEach((point: PointGeo) => {
            const pointIcon = L.divIcon({
                html: `<div style="width: 32px; height: 32px; font-size: 20px; line-height: 32px; text-align: center;">${point.emoji}</div>`,
                className: "",
                iconSize: [32, 32],
                iconAnchor: [16, 16],
                popupAnchor: [0, -16],
            });

            const marker = L.marker([point.coordonneesY, point.coordonneesX], { icon: pointIcon }).addTo(map);
            marker.bindPopup(`<b>${point.titre}</b><br>${point.description}`);
        });
    }

    // Active l'écouteur de clic pour ajouter un point
    if (addPointMode) {
        map.on("click", (event: any) => handleMapClick(event));
    } else {
        map.off("click");
    }
}

(window as any).updateMap = updateMap; // Expose la fonction updateMap pour qu'elle puisse être appelée depuis le HTML

(window as any).cacherActions = async function () {
    const restaurantName = document.getElementById("restaurant-name") as HTMLSpanElement;

    if (reservationLockActif && restaurantName?.innerText && dateHeureReservationEnCours) {
        try {
            await libererTablesRestaurant(restaurantName.innerText, dateHeureReservationEnCours);
        } catch (error: any) {
            console.log("Erreur lors de la liberation du verrou :", error);
        }
    }

    reservationLockActif = false;
    tablesDisponiblesEnCours = [];
    dateHeureReservationEnCours = null;
    afficherPanneau("filtres");
};

(window as any).cacherMenuCrous = function () {
    afficherPanneau("filtres");
};

// avant de quitter la page on libère le verrou pour pas que on reste bloqué à l'infini
// si l'utilisateur quitte la page
window.addEventListener("beforeunload", () => {
    const restaurantName = document.getElementById("restaurant-name") as HTMLSpanElement;

    if (reservationLockActif && restaurantName?.innerText && dateHeureReservationEnCours) {
        navigator.sendBeacon(`${proxyUrl}/api/restaurants/tables/liberer?nomRestaurant=${encodeURIComponent(restaurantName.innerText)}&dateHeure=${encodeURIComponent(dateHeureReservationEnCours)}`);
    }
});

/**
 * Gère le clic sur la carte pour ajouter un point
 */
function handleMapClick(event: any): void {
    const lat = event.latlng.lat;
    const lon = event.latlng.lng;

    afficherPanneau("add-point-form");

    // Stocke les coordonnées
    (window as any).currentLat = lat;
    (window as any).currentLon = lon;

    // Met les coordonnées dans le formulaire
    const coordsDisplay = document.getElementById("coords-display");
    if (coordsDisplay) {
        coordsDisplay.innerText = `Lat: ${lat.toFixed(4)}, Lon: ${lon.toFixed(4)}`;
    }
}

/**
 * Valide et ajoute le point
 */
(window as any).validerAjoutPoint = async function () {
    const titre = (document.getElementById("point-titre") as HTMLInputElement).value;
    const description = (document.getElementById("point-description") as HTMLTextAreaElement).value;

    if (!titre) {
        alert("Veuillez entrer un titre");
        return;
    }

    const lat = (window as any).currentLat;
    const lon = (window as any).currentLon;

    const newPoint = await ajouterPointGeo(proxyUrl, lon, lat, selectedEmoji, titre, description);

    if (newPoint) {
        points.push(newPoint);
        console.log("Point ajouté :", newPoint);

        // Réinitialise le formulaire
        (document.getElementById("point-titre") as HTMLInputElement).value = "";
        (document.getElementById("point-description") as HTMLTextAreaElement).value = "";

        // Réaffiche la carte
        updateMap();
        afficherPanneau("filtres");
    } else {
        alert("Erreur lors de l'ajout du point");
    }
};

/**
 * Active/désactive le mode ajout de point
 */
(window as any).activerAjoutPoint = function () {
    addPointMode = !addPointMode;
    const button = document.getElementById("btn-add-point") as HTMLButtonElement;

    if (addPointMode) {
        button.style.backgroundColor = "#fca5a5";
        button.innerText = "Cliquez sur la carte";
    } else {
        button.style.backgroundColor = "#bfdbfe";
        button.innerText = "Ajouter un point";
        map.off("click");
    }

    updateMap();
};

/**
 * Change l'emoji sélectionné
 */
(window as any).changeEmoji = function (emoji: string) {
    selectedEmoji = emoji;

    const emojiButtons = document.querySelectorAll<HTMLButtonElement>(".emoji-btn");

    emojiButtons.forEach((btn) => {
        if (btn.textContent?.trim() === emoji) {
            btn.classList.add("border-4", "border-blue-600");
            btn.classList.remove("border-2", "border-blue-300");
        } else {
            btn.classList.remove("border-4", "border-blue-600");
            btn.classList.add("border-2", "border-blue-300");
        }
    });
};

(window as any).afficherTablesDisponibles = async function (utiliserTablesChargees = false) {
    const restaurantName = document.getElementById("restaurant-name") as HTMLSpanElement;
    const tablesDiv = document.getElementById("tables-dispo");
    const dateInput = document.getElementById("date-reservation-globale") as HTMLInputElement;

    if (!restaurantName || !tablesDiv || !dateInput) return;

    if (!dateInput.value) {
        alert("Veuillez choisir une date et une heure.");
        return;
    }

    const nomRestaurant = restaurantName.innerText;
    const dateHeure = dateInput.value.replace("T", " ") + ":00";

    tablesDiv.innerHTML = "Chargement des tables disponibles...";
    tablesDiv.classList.remove("hidden");

    try {
        const memeCreneau = reservationLockActif && dateHeureReservationEnCours === dateHeure;
        if (reservationLockActif && !memeCreneau && dateHeureReservationEnCours) {
            await libererTablesRestaurant(nomRestaurant, dateHeureReservationEnCours);
            reservationLockActif = false;
            tablesDisponiblesEnCours = [];
            dateHeureReservationEnCours = null;
        }
        const tables = memeCreneau ? tablesDisponiblesEnCours : await recupererTablesRestaurant(nomRestaurant, dateHeure);

        if (tables.length === 0) {
            tablesDiv.innerHTML = "Aucune table disponible sur ce créneau.";
            return;
        }

        reservationLockActif = true;
        tablesDisponiblesEnCours = tables;
        dateHeureReservationEnCours = dateHeure;
        tablesDiv.innerHTML = tables
            .map(
                (table) => `
                <button
                    class="w-full text-left border-b border-blue-300 py-2 hover:bg-blue-200"
                    onclick="selectionnerTable(${table.idTable}, ${table.nbPlaces})"
                >
                    <strong>Table ${table.idTable}</strong> — ${table.nbPlaces} places
                </button>
            `
            )
            .join("");
    } catch (error: any) {
        reservationLockActif = false;
        tablesDisponiblesEnCours = [];
        dateHeureReservationEnCours = null;
        tablesDiv.innerHTML = `
            <p>${error.message}</p>
            <button class="bg-blue-200 px-3 py-1 rounded border-2 border-blue-400 mt-2 cursor-pointer" onclick="afficherTablesDisponibles()">Actualiser</button>
        `;
    }
};

(window as any).validerReservation = async function (idTable: number) {
    const restaurantName = document.getElementById("restaurant-name") as HTMLSpanElement;
    const dateInput = document.getElementById("date-reservation-globale") as HTMLInputElement;

    const nom = (document.getElementById("nom-reservation") as HTMLInputElement).value;
    const prenom = (document.getElementById("prenom-reservation") as HTMLInputElement).value;
    const telephone = (document.getElementById("tel-reservation") as HTMLInputElement).value;
    const nombreConvives = Number((document.getElementById("convives-reservation") as HTMLInputElement).value);

    if (!nom || !prenom || !telephone || !dateInput.value || !nombreConvives) {
        alert("Veuillez remplir tous les champs.");
        return;
    }

    const dateHeure = dateInput.value.replace("T", " ") + ":00";

    const resultat = await reserverTableRestaurant({
        nomRestaurant: restaurantName.innerText,
        idTable,
        dateHeure,
        nom,
        prenom,
        nombreConvives,
        telephone,
    });

    alert(resultat.message);

    if (resultat.succes || resultat.success) {
        reservationLockActif = false;
        tablesDisponiblesEnCours = [];
        dateHeureReservationEnCours = null;
        (window as any).cacherActions();
    }
};

(window as any).selectionnerTable = function (idTable: number, nbPlaces: number) {
    const tablesDiv = document.getElementById("tables-dispo");

    if (!tablesDiv) return;

    tablesDiv.innerHTML = `
        <p class="mb-2"><strong>Table ${idTable}</strong> sélectionnée — ${nbPlaces} places</p>

        <input class="border rounded px-2 py-1 mt-1 w-full" id="nom-reservation" placeholder="Nom" />
        <input class="border rounded px-2 py-1 mt-1 w-full" id="prenom-reservation" placeholder="Prénom" />
        <input class="border rounded px-2 py-1 mt-1 w-full" id="tel-reservation" placeholder="Téléphone" />
        <input class="border rounded px-2 py-1 mt-1 w-full" id="convives-reservation" type="number" min="1" max="${nbPlaces}" placeholder="Nombre de convives" />

        <button
            class="bg-green-200 px-3 py-1 rounded border-2 border-green-400 mt-2 cursor-pointer"
            onclick="validerReservation(${idTable})"
        >
            Valider la réservation
        </button>

        <button
            class="bg-gray-200 px-3 py-1 rounded border-2 border-gray-400 mt-2 ml-2 cursor-pointer"
            onclick="afficherTablesDisponibles(true)"
        >
            Choisir une autre table
        </button>
    `;
};

(window as any).afficherAdminPanel = async function () {
    const filtresDiv = document.getElementById("filtres");
    const reservationForm = document.getElementById("reservation-form");
    const adminPanel = document.getElementById("admin-panel");
    const reservationsList = document.getElementById("reservations-list");

    if (filtresDiv) filtresDiv.style.display = "none";
    if (reservationForm) reservationForm.style.display = "none";
    if (adminPanel) adminPanel.classList.remove("hidden");

    if (!reservationsList) return;

    reservationsList.innerHTML = "Chargement...";

    try {
        const reservations = await recupererReservations();

        if (reservations.length === 0) {
            reservationsList.innerHTML = "Aucune réservation.";
            return;
        }

        reservationsList.innerHTML = reservations
            .map(
                (reservation) => `
                <div class="border-b border-blue-300 py-2">
                    <p><strong>${reservation.nomRestaurant}</strong></p>
                    <p>Table : ${reservation.idTable}</p>
                    <p>Date : ${reservation.dateRes}</p>
                    <p>Client : ${reservation.prenomClient} ${reservation.nomClient}</p>
                    <p>Convives : ${reservation.nbConvives}</p>
                    <p>Téléphone : ${reservation.numTel}</p>
                </div>
            `
            )
            .join("");
    } catch (error: any) {
        reservationsList.innerHTML = error.message;
    }
};

(window as any).cacherAdminPanel = function () {
    const filtresDiv = document.getElementById("filtres");
    const adminPanel = document.getElementById("admin-panel");

    if (adminPanel) adminPanel.classList.add("hidden");
    if (filtresDiv) filtresDiv.style.display = "block";
};
