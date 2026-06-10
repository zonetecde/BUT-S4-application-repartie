"use strict";
(() => {
  // velibs.ts
  async function recupererVelibsNancy() {
    const url2 = "https://api.cyclocity.fr/contracts/nancy/gbfs/v2/station_information.json";
    const response = await fetch(url2);
    const dataStations = await response.json();
    const urlDetails = "https://api.cyclocity.fr/contracts/nancy/gbfs/v2/station_status.json";
    const responseDetails = await fetch(urlDetails);
    const dataDetails = await responseDetails.json();
    const stations = dataStations.data.stations.map((station) => {
      const details = dataDetails.data.stations.find((detail) => detail.station_id === station.station_id);
      return {
        id: station.station_id,
        nom: station.name,
        adresse: station.address || "",
        lat: station.lat,
        lon: station.lon,
        velosDisponibles: details?.num_bikes_available ?? 0,
        placesLibres: details?.num_docks_available ?? 0
      };
    });
    return stations;
  }

  // incidents.ts
  async function recupererIncidentsNancy(proxyUrl2) {
    const resp = await fetch(`${proxyUrl2}/api/fetch?url=https://carto.g-ny.eu/data/cifs/cifs_waze_v2.json`);
    const respData = await resp.json();
    if (!respData.success) {
      console.error("Erreur lors de la r\xE9cup\xE9ration des incidents :", respData.error);
      return [];
    }
    const incidents2 = JSON.parse(respData.data).incidents;
    return incidents2.map((incident) => {
      const [lat, lon] = incident.location.polyline.split(" ");
      return {
        id: incident.id,
        type: incident.type,
        description: incident.description,
        lat: parseFloat(lat),
        lon: parseFloat(lon),
        location_description: incident.location?.location_description
      };
    });
  }

  // restaurants.ts
  async function recupererRestoNancy(proxyUrl2) {
    const url2 = `${proxyUrl2}/api/restaurants/coordonnees`;
    const response = await fetch(url2);
    const dataResto = await response.json();
    if (!dataResto.success) {
      console.error("Erreur lors de la r\xE9cup\xE9ration des restaurants :", dataResto.message);
      return [];
    }
    const restaurants2 = dataResto.data.map((resto) => ({
      idRestaurant: resto.idRestaurant,
      nom: resto.nom,
      adresse: resto.adresse,
      lat: resto.lat,
      lon: resto.lon
    }));
    return restaurants2;
  }
  async function recupererTablesRestaurant(nomRestaurant, dateHeure) {
    const params = new URLSearchParams();
    params.append("nomRestaurant", nomRestaurant);
    params.append("dateHeure", dateHeure);
    const response = await fetch(`http://localhost:8081/api/restaurants/tables?${params.toString()}`);
    const json = await response.json();
    if (!json.success && !json.succes) {
      throw new Error(json.message);
    }
    return Object.entries(json.data).map(([idTable, nbPlaces]) => ({
      idTable: Number(idTable),
      nbPlaces: Number(nbPlaces)
    }));
  }
  async function reserverTableRestaurant(data) {
    const params = new URLSearchParams();
    params.append("nomRestaurant", data.nomRestaurant);
    params.append("idTable", String(data.idTable));
    params.append("dateHeure", data.dateHeure);
    params.append("nom", data.nom);
    params.append("prenom", data.prenom);
    params.append("nombreConvives", String(data.nombreConvives));
    params.append("telephone", data.telephone);
    const response = await fetch(`http://localhost:8081/api/restaurants/reserver?${params.toString()}`, {
      method: "POST"
    });
    const text = await response.text();
    console.log("R\xE9ponse r\xE9servation :", text);
    return JSON.parse(text);
  }
  async function recupererReservations() {
    const response = await fetch("http://localhost:8081/api/restaurants/reservations");
    const json = await response.json();
    if (!json.success) {
      throw new Error(json.message);
    }
    return json.data;
  }

  // crous.ts
  async function recupererCrousNancy(proxyUrl2) {
    const url2 = `${proxyUrl2}/api/crous/restaurants?ville=Nancy`;
    const response = await fetch(url2);
    const dataResto = await response.json();
    if (!dataResto.success) {
      console.error("Erreur lors de la r\xE9cup\xE9ration des restaurants CROUS :", dataResto.message);
      return [];
    }
    return dataResto.data;
  }
  async function chargerMenu(proxyUrl2, idRestaurant) {
    const url2 = `${proxyUrl2}/api/crous/menu?idRestaurant=${idRestaurant}`;
    const response = await fetch(url2);
    const dataMenu = await response.json();
    if (!dataMenu.success) {
      console.error("Erreur lors du chargement du menu :", dataMenu.message);
      return { date: "", menu: "Menu indisponible." };
    }
    return dataMenu.data;
  }

  // points.ts
  async function recupererPointsGeo(proxyUrl2) {
    try {
      const response = await fetch(`${proxyUrl2}/api/points/list`);
      const data = await response.json();
      if (data.success) {
        return data.data || [];
      } else {
        console.error("Erreur lors de la r\xE9cup\xE9ration des points :", data.message);
        return [];
      }
    } catch (error) {
      console.error("Erreur lors de l'appel au proxy :", error);
      return [];
    }
  }
  async function ajouterPointGeo(proxyUrl2, coordonneesX, coordonneesY, emoji, titre, description) {
    try {
      const params = new URLSearchParams({
        x: String(coordonneesX),
        y: String(coordonneesY),
        emoji,
        titre,
        description
      });
      const response = await fetch(`${proxyUrl2}/api/points/add?${params}`, { method: "POST" });
      const data = await response.json();
      if (data.success) {
        console.log("Point ajout\xE9");
        return data.data;
      } else {
        console.error("Erreur lors de l'ajout du point :", data.message);
        return null;
      }
    } catch (error) {
      console.error("Erreur lors de l'appel au proxy :", error);
      return null;
    }
  }

  // app.mts
  var proxyHost = prompt("Adresse IP du proxy HTTP ?", "localhost");
  var proxyPort = prompt("Port du proxy HTTP ?", "8081");
  var proxyUrl = `http://${proxyHost || "localhost"}:${proxyPort || "8081"}`;
  var url = "https://data.geopf.fr/geocodage/search?q=Nancy&limit=1";
  var incidents = [];
  var velibs = [];
  var restaurants = [];
  var crous = [];
  var points = [];
  var map;
  var addPointMode = false;
  var selectedEmoji = "\u{1F4CD}";
  var positionActuelMarker = null;
  fetch(url).then((response) => response.json()).then(async (data) => {
    const lon = data.features[0].geometry.coordinates[0];
    const lat = data.features[0].geometry.coordinates[1];
    console.log(`Coordonn\xE9es de Nancy : lat=${lat}, lon=${lon}`);
    map = L.map("map").setView([lat, lon], 13);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);
    try {
      velibs = await recupererVelibsNancy();
      crous = await recupererCrousNancy(proxyUrl);
      restaurants = await recupererRestoNancy(proxyUrl);
      incidents = await recupererIncidentsNancy(proxyUrl);
      points = await recupererPointsGeo(proxyUrl);
    } catch (error) {
      console.error("Erreur lors de la r\xE9cup\xE9ration des donn\xE9es :", error);
    }
    updateMap();
    localiserUtilisateur();
  });
  function afficherPanneau(panelId) {
    const panels = ["filtres", "reservation-form", "menu-crous", "add-point-form"];
    for (const id of panels) {
      const el = document.getElementById(id);
      if (el) el.style.display = "none";
    }
    const panel = document.getElementById(panelId);
    if (panel) panel.style.display = "block";
  }
  function localiserUtilisateur() {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords;
        const userIcon = L.divIcon({
          html: '<div style="width: 28px; height: 28px; font-size: 16px; line-height: 24px; text-align: center; background: #dbeafe; border: 2px solid #2563eb; border-radius: 50%;">\u{1F9D1}</div>',
          className: "",
          iconSize: [28, 28],
          iconAnchor: [14, 14],
          popupAnchor: [0, -14]
        });
        positionActuelMarker = L.marker([latitude, longitude], { icon: userIcon }).addTo(map);
        positionActuelMarker.bindPopup("<b>Vous \xEAtes ici</b>");
        positionActuelMarker.openPopup();
      },
      (error) => {
        console.log("Veuillez autoriser la g\xE9olocalisation pour afficher votre position sur la carte." + error);
      }
    );
  }
  function updateMap() {
    map.eachLayer((layer) => {
      if (layer instanceof L.Marker && layer !== positionActuelMarker) {
        map.removeLayer(layer);
      }
    });
    const filtreVelib = document.getElementById("filtre-velib").checked;
    if (filtreVelib) {
      const bikeIcon = L.divIcon({
        html: '<div style="width: 24px; height: 24px; font-size: 14px; line-height: 20px; text-align: center; background: #bfdbfe; border: 2px solid #1d4ed8; border-radius: 50%;">\u{1F6B2}</div>',
        className: "",
        iconSize: [24, 24],
        iconAnchor: [12, 12],
        popupAnchor: [0, -12]
      });
      velibs.forEach((station) => {
        const marker = L.marker([station.lat, station.lon], { icon: bikeIcon }).addTo(map);
        marker.bindPopup(`<b>${station.nom}</b><br>Adresse : ${station.adresse}<br>V\xE9los disponibles : ${station.velosDisponibles}<br>Places de parking libres : ${station.placesLibres}`);
      });
    }
    const filtreRestaurant = document.getElementById("filtre-restaurant").checked;
    if (filtreRestaurant) {
      const foodIcon = L.divIcon({
        html: '<div style="width: 24px; height: 24px; font-size: 14px; line-height: 20px; text-align: center; background: #bbf7d0; border: 2px solid #15803d; border-radius: 50%;">\u{1F37D}\uFE0F</div>',
        className: "",
        iconSize: [24, 24],
        iconAnchor: [12, 12],
        popupAnchor: [0, -12]
      });
      restaurants.forEach((resto) => {
        const marker = L.marker([resto.lat, resto.lon], { icon: foodIcon }).addTo(map);
        marker.bindPopup(`<b>${resto.nom}</b><br>Adresse : ${resto.adresse}`);
        marker.on("click", (event) => {
          console.log("Restaurant cliqu\xE9 :", resto.nom, event);
          afficherPanneau("reservation-form");
          const restaurantName = document.getElementById("restaurant-name");
          if (restaurantName) {
            restaurantName.innerText = resto.nom;
          }
          const tablesDiv = document.getElementById("tables-dispo");
          if (tablesDiv) {
            tablesDiv.innerHTML = "";
            tablesDiv.classList.add("hidden");
          }
        });
      });
    }
    const filtreCrous = document.getElementById("filtre-crous").checked;
    if (filtreCrous) {
      const crousIcon = L.divIcon({
        html: '<div style="width: 24px; height: 24px; font-size: 14px; line-height: 20px; text-align: center; background: #fef08a; border: 2px solid #ca8a04; border-radius: 50%;">\u{1F393}</div>',
        className: "",
        iconSize: [24, 24],
        iconAnchor: [12, 12],
        popupAnchor: [0, -12]
      });
      crous.forEach((restoCrous) => {
        const marker = L.marker([restoCrous.latitude, restoCrous.longitude], { icon: crousIcon }).addTo(map);
        marker.bindPopup(`<b>${restoCrous.nom}</b><br>Adresse : ${restoCrous.adresse}${restoCrous.horaires ? `<br>Horaires : ${JSON.parse(restoCrous.horaires).join(", ")}` : ""}`);
        marker.on("click", async () => {
          afficherPanneau("menu-crous");
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
    const filtreIncident = document.getElementById("filtre-incident").checked;
    if (filtreIncident) {
      const warningIcon = L.divIcon({
        html: '<div style="width: 24px; height: 24px; font-size: 14px; line-height: 20px; text-align: center; background: #fecaca; border: 2px solid #b91c1c; border-radius: 50%;">\u26A0\uFE0F</div>',
        className: "",
        iconSize: [24, 24],
        iconAnchor: [12, 12],
        popupAnchor: [0, -12]
      });
      incidents.forEach((incident) => {
        const marker = L.marker([incident.lat, incident.lon], { icon: warningIcon }).addTo(map);
        marker.bindPopup(`<b>${incident.type}</b><br>Description : ${incident.description}`);
      });
    }
    const filtrePoints = document.getElementById("filtre-points")?.checked ?? true;
    if (filtrePoints) {
      points.forEach((point) => {
        const pointIcon = L.divIcon({
          html: `<div style="width: 32px; height: 32px; font-size: 20px; line-height: 32px; text-align: center;">${point.emoji}</div>`,
          className: "",
          iconSize: [32, 32],
          iconAnchor: [16, 16],
          popupAnchor: [0, -16]
        });
        const marker = L.marker([point.coordonneesY, point.coordonneesX], { icon: pointIcon }).addTo(map);
        marker.bindPopup(`<b>${point.titre}</b><br>${point.description}`);
      });
    }
    if (addPointMode) {
      map.on("click", (event) => handleMapClick(event));
    } else {
      map.off("click");
    }
  }
  window.updateMap = updateMap;
  window.cacherActions = function() {
    afficherPanneau("filtres");
  };
  window.cacherMenuCrous = function() {
    afficherPanneau("filtres");
  };
  function handleMapClick(event) {
    const lat = event.latlng.lat;
    const lon = event.latlng.lng;
    afficherPanneau("add-point-form");
    window.currentLat = lat;
    window.currentLon = lon;
    const coordsDisplay = document.getElementById("coords-display");
    if (coordsDisplay) {
      coordsDisplay.innerText = `Lat: ${lat.toFixed(4)}, Lon: ${lon.toFixed(4)}`;
    }
  }
  window.validerAjoutPoint = async function() {
    const titre = document.getElementById("point-titre").value;
    const description = document.getElementById("point-description").value;
    if (!titre) {
      alert("Veuillez entrer un titre");
      return;
    }
    const lat = window.currentLat;
    const lon = window.currentLon;
    const newPoint = await ajouterPointGeo(proxyUrl, lon, lat, selectedEmoji, titre, description);
    if (newPoint) {
      points.push(newPoint);
      console.log("Point ajout\xE9 :", newPoint);
      document.getElementById("point-titre").value = "";
      document.getElementById("point-description").value = "";
      updateMap();
      afficherPanneau("filtres");
    } else {
      alert("Erreur lors de l'ajout du point");
    }
  };
  window.activerAjoutPoint = function() {
    addPointMode = !addPointMode;
    const button = document.getElementById("btn-add-point");
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
  window.changeEmoji = function(emoji) {
    selectedEmoji = emoji;
    const emojiButtons = document.querySelectorAll(".emoji-btn");
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
  window.afficherTablesDisponibles = async function() {
    const restaurantName = document.getElementById("restaurant-name");
    const tablesDiv = document.getElementById("tables-dispo");
    const dateInput = document.getElementById("date-reservation-globale");
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
      const tables = await recupererTablesRestaurant(nomRestaurant, dateHeure);
      if (tables.length === 0) {
        tablesDiv.innerHTML = "Aucune table disponible sur ce cr\xE9neau.";
        return;
      }
      tablesDiv.innerHTML = tables.map(
        (table) => `
                <button
                    class="w-full text-left border-b border-blue-300 py-2 hover:bg-blue-200"
                    onclick="selectionnerTable(${table.idTable}, ${table.nbPlaces})"
                >
                    <strong>Table ${table.idTable}</strong> \u2014 ${table.nbPlaces} places
                </button>
            `
      ).join("");
    } catch (error) {
      tablesDiv.innerHTML = error.message;
    }
  };
  window.validerReservation = async function(idTable) {
    const restaurantName = document.getElementById("restaurant-name");
    const dateInput = document.getElementById("date-reservation-globale");
    const nom = document.getElementById("nom-reservation").value;
    const prenom = document.getElementById("prenom-reservation").value;
    const telephone = document.getElementById("tel-reservation").value;
    const nombreConvives = Number(document.getElementById("convives-reservation").value);
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
      telephone
    });
    alert(resultat.message);
    if (resultat.succes || resultat.success) {
      window.cacherActions();
    }
  };
  window.selectionnerTable = function(idTable, nbPlaces) {
    const tablesDiv = document.getElementById("tables-dispo");
    if (!tablesDiv) return;
    tablesDiv.innerHTML = `
        <p class="mb-2"><strong>Table ${idTable}</strong> s\xE9lectionn\xE9e \u2014 ${nbPlaces} places</p>

        <input class="border rounded px-2 py-1 mt-1 w-full" id="nom-reservation" placeholder="Nom" />
        <input class="border rounded px-2 py-1 mt-1 w-full" id="prenom-reservation" placeholder="Pr\xE9nom" />
        <input class="border rounded px-2 py-1 mt-1 w-full" id="tel-reservation" placeholder="T\xE9l\xE9phone" />
        <input class="border rounded px-2 py-1 mt-1 w-full" id="convives-reservation" type="number" min="1" max="${nbPlaces}" placeholder="Nombre de convives" />

        <button
            class="bg-green-200 px-3 py-1 rounded border-2 border-green-400 mt-2 cursor-pointer"
            onclick="validerReservation(${idTable})"
        >
            Valider la r\xE9servation
        </button>

        <button
            class="bg-gray-200 px-3 py-1 rounded border-2 border-gray-400 mt-2 ml-2 cursor-pointer"
            onclick="afficherTablesDisponibles()"
        >
            Choisir une autre table
        </button>
    `;
  };
  window.afficherAdminPanel = async function() {
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
        reservationsList.innerHTML = "Aucune r\xE9servation.";
        return;
      }
      reservationsList.innerHTML = reservations.map(
        (reservation) => `
                <div class="border-b border-blue-300 py-2">
                    <p><strong>${reservation.nomRestaurant}</strong></p>
                    <p>Table : ${reservation.idTable}</p>
                    <p>Date : ${reservation.dateRes}</p>
                    <p>Client : ${reservation.prenomClient} ${reservation.nomClient}</p>
                    <p>Convives : ${reservation.nbConvives}</p>
                    <p>T\xE9l\xE9phone : ${reservation.numTel}</p>
                </div>
            `
      ).join("");
    } catch (error) {
      reservationsList.innerHTML = error.message;
    }
  };
  window.cacherAdminPanel = function() {
    const filtresDiv = document.getElementById("filtres");
    const adminPanel = document.getElementById("admin-panel");
    if (adminPanel) adminPanel.classList.add("hidden");
    if (filtresDiv) filtresDiv.style.display = "block";
  };
})();
//# sourceMappingURL=index.js.map
