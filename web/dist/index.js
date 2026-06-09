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
  async function recupererIncidentsNancy() {
    const resp = await fetch("http://localhost:8081/api/fetch?url=https://carto.g-ny.eu/data/cifs/cifs_waze_v2.json");
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
  async function recupererRestoNancy() {
    const url2 = "http://localhost:8081/api/restaurants/coordonnees";
    const response = await fetch(url2);
    const dataResto = await response.json();
    const restaurants2 = dataResto.data.map((resto) => ({
      idRestaurant: resto.idRestaurant,
      nom: resto.nom,
      adresse: resto.adresse,
      lat: resto.lat,
      lon: resto.lon
    }));
    return restaurants2;
  }

  // app.mts
  var url = "https://data.geopf.fr/geocodage/search?q=Nancy&limit=1";
  var incidents = [];
  var velibs = [];
  var restaurants = [];
  var map;
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
      restaurants = await recupererRestoNancy();
      incidents = await recupererIncidentsNancy();
    } catch (error) {
      console.error("Erreur lors de la r\xE9cup\xE9ration des donn\xE9es :", error);
    }
    updateMap();
  });
  function updateMap() {
    map.eachLayer((layer) => {
      if (layer instanceof L.Marker) {
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
          ouvrirReservation(resto);
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
  }
  window.updateMap = updateMap;
  var currentRestaurant = null;
  var currentSelectedTable = null;
  function ouvrirReservation(resto) {
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
      tableSelectionnee.innerText = "Aucune table s\xE9lectionn\xE9e";
    }
  }
  var currentAvailableTables = [];
  async function chercherTablesDispo() {
    if (!currentRestaurant) {
      alert("S\xE9lectionner d'abord un restaurant.");
      return;
    }
    const personnes = parseInt(document.getElementById("reservation-personnes").value, 10);
    const dateHeure = document.getElementById("reservation-date").value;
    if (!dateHeure || !personnes || personnes < 1) {
      alert("Il faut indiquer une date/heure valide et le nombre de personnes.");
      return;
    }
    const params = new URLSearchParams({
      nomRestaurant: currentRestaurant.nom,
      dateHeure,
      nombreConvives: personnes.toString()
    });
    const url2 = `http://localhost:8081/api/restaurants/tables?${params.toString()}`;
    const listeTables = document.getElementById("liste-tables");
    if (!listeTables) {
      return;
    }
    try {
      const response = await fetch(url2);
      const json = await response.json();
      if (!response.ok || !json.success) {
        listeTables.innerHTML = `<p class="text-red-600">${json.message || "Impossible de r\xE9cup\xE9rer les tables disponibles."}</p>`;
        currentAvailableTables = [];
      } else {
        const data = json.data;
        currentAvailableTables = Object.entries(data).map(([id, capacity]) => ({
          id: Number(id),
          capacity: Number(capacity)
        }));
        listeTables.innerHTML = "";
        if (currentAvailableTables.length === 0) {
          listeTables.innerHTML = "<p>Aucune table disponible pour ce cr\xE9neau.</p>";
        } else {
          currentAvailableTables.forEach((table) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "bg-blue-200 text-left px-3 py-2 rounded border border-blue-300 hover:bg-blue-300";
            button.innerText = `Table ${table.id} \u2014 ${table.capacity} personnes`;
            button.onclick = () => selectTable(table.id);
            listeTables.appendChild(button);
          });
        }
      }
    } catch (error) {
      listeTables.innerHTML = '<p class="text-red-600">Erreur de connexion au serveur.</p>';
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
  function selectTable(tableId) {
    const table = currentAvailableTables.find((t) => t.id === tableId);
    if (!table) {
      return;
    }
    currentSelectedTable = table;
    const tableSelectionnee = document.getElementById("table-selectionnee");
    if (tableSelectionnee) {
      tableSelectionnee.innerText = `Table ${table.id} \u2014 ${table.capacity} personnes`;
    }
    const detailsDiv = document.getElementById("reservation-details");
    if (detailsDiv) {
      detailsDiv.style.display = "block";
    }
  }
  async function envoyerReservation() {
    if (!currentRestaurant || !currentSelectedTable) {
      alert("Veuillez s\xE9lectionner d'abord une table disponible.");
      return;
    }
    const prenom = document.getElementById("reservation-prenom").value.trim();
    const nom = document.getElementById("reservation-nom").value.trim();
    const telephone = document.getElementById("reservation-phone").value.trim();
    const personnes = parseInt(document.getElementById("reservation-personnes").value, 10);
    const dateHeure = document.getElementById("reservation-date").value;
    if (!prenom || !nom) {
      alert("Veuillez indiquer le pr\xE9nom et le nom de la personne qui r\xE9serve.");
      return;
    }
    const params = new URLSearchParams({
      nomRestaurant: currentRestaurant.nom,
      idTable: currentSelectedTable.id.toString(),
      dateHeure,
      nom,
      prenom,
      nombreConvives: personnes.toString(),
      telephone
    });
    const url2 = `http://localhost:8081/api/restaurants/reserver?${params.toString()}`;
    try {
      const response = await fetch(url2, { method: "POST" });
      const json = await response.json();
      if (!response.ok || !json.success) {
        alert(`Erreur : ${json.message || "La r\xE9servation n'a pas pu \xEAtre enregistr\xE9e."}`);
        return;
      }
      alert(`R\xE9servation confirm\xE9e pour ${prenom} ${nom} au restaurant ${currentRestaurant.nom}
Table ${currentSelectedTable.id} \u2014 ${currentSelectedTable.capacity} personnes
${dateHeure}
Nombre de personnes : ${personnes}`);
      cacherActions();
    } catch (error) {
      alert("Erreur de connexion au serveur lors de l'enregistrement de la r\xE9servation.");
    }
  }
  window.chercherTablesDispo = chercherTablesDispo;
  window.selectTable = selectTable;
  window.envoyerReservation = envoyerReservation;
  window.cacherActions = function() {
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
    document.getElementById("reservation-prenom").value = "";
    document.getElementById("reservation-nom").value = "";
    document.getElementById("reservation-phone").value = "";
    document.getElementById("reservation-notes").value = "";
    document.getElementById("reservation-date").value = "";
    document.getElementById("reservation-personnes").value = "2";
    currentRestaurant = null;
    currentSelectedTable = null;
    currentAvailableTables = [];
  };
})();
//# sourceMappingURL=index.js.map
