"use strict";
(() => {
  // web/velibs.ts
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
        adresse: station.address,
        lat: station.lat,
        lon: station.lon,
        velosDisponibles: details?.num_bikes_available ?? 0,
        placesVoitureDisponibles: details?.num_docks_available ?? 0
      };
    });
    return stations;
  }

  // web/incidents.ts
  async function recupererIncidentsNancy() {
    const resp = await fetch("http://localhost:8080/api/fetch?url=https://carto.g-ny.eu/data/cifs/cifs_waze_v2.json");
    const respData = await resp.json();
    if (!respData.success) {
      console.error("Erreur lors de la r\xE9cup\xE9ration des incidents :", respData.error);
      return [];
    }
    const incidents = JSON.parse(respData.data).incidents;
    return incidents.map((incident) => {
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

  // web/app.mts
  var url = "https://data.geopf.fr/geocodage/search?q=Nancy&limit=1";
  fetch(url).then((response) => response.json()).then(async (data) => {
    const lon = data.features[0].geometry.coordinates[0];
    const lat = data.features[0].geometry.coordinates[1];
    console.log(`Coordonn\xE9es de Nancy : lat=${lat}, lon=${lon}`);
    let map = L.map("map").setView([lat, lon], 13);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);
    const stations = await recupererVelibsNancy();
    stations.forEach((station) => {
      const marker = L.marker([station.lat, station.lon]).addTo(map);
      marker.bindPopup(`<b>${station.nom}</b><br>Adresse : ${station.adresse}<br>V\xE9los disponibles : ${station.velosDisponibles}<br>Places de parking libres : ${station.placesVoitureDisponibles}`);
    });
    const incidents = await recupererIncidentsNancy();
    incidents.forEach((incident) => {
      const marker = L.marker([incident.lat, incident.lon]).addTo(map);
      marker.bindPopup(`<b>${incident.type}</b><br>Description : ${incident.description}`);
    });
  });
})();
//# sourceMappingURL=index.js.map
