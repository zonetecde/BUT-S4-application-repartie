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

  // crous.ts
  async function recupererCrousNancy() {
    const url2 = "https://api.croustillant.menu/v1/regions";
    const response = await fetch(url2);
    const dataRegions = await response.json();
    const regionNancy = dataRegions.data.find((region) => region.libelle.toLowerCase().includes("nancy"));
    if (!regionNancy) {
      console.error("R\xE9gion Nancy non trouv\xE9e");
      return [];
    }
    const codeNancy = regionNancy.code;
    const urlResto = `https://api.croustillant.menu/v1/regions/${codeNancy}/restaurants`;
    const responseResto = await fetch(urlResto);
    const dataResto = await responseResto.json();
    return dataResto.data;
  }
  async function chargerMenu(idRestaurant) {
    const url2 = `https://api.croustillant.menu/v1/restaurants/${idRestaurant}/menu`;
    const response = await fetch(url2);
    const dataMenu = await response.json();
    const jour = dataMenu.data[0];
    const date = jour.date;
    let menuStr = "";
    for (const repas of jour.repas) {
      const typeRepas = repas.type === "matin" ? "Matin" : "Soir";
      menuStr += `${typeRepas}
`;
      for (const categorie of repas.categories) {
        menuStr += `  ${categorie.libelle} :
`;
        for (const plat of categorie.plats) {
          menuStr += `    - ${plat.libelle}
`;
        }
      }
    }
    return {
      date,
      menu: menuStr
    };
  }

  // app.mts
  var url = "https://data.geopf.fr/geocodage/search?q=Nancy&limit=1";
  var incidents = [];
  var velibs = [];
  var restaurants = [];
  var crous = [];
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
      crous = await recupererCrousNancy();
      restaurants = await recupererRestoNancy();
      incidents = await recupererIncidentsNancy();
    } catch (error) {
      console.error("Erreur lors de la r\xE9cup\xE9ration des donn\xE9es :", error);
    }
    updateMap();
  });
  function afficherPanneau(panelId) {
    const panels = ["filtres", "reservation-form", "menu-crous"];
    for (const id of panels) {
      const el = document.getElementById(id);
      if (el) el.style.display = "none";
    }
    const panel = document.getElementById(panelId);
    if (panel) panel.style.display = "block";
  }
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
          afficherPanneau("reservation-form");
          const restaurantName = document.getElementById("restaurant-name");
          if (restaurantName) {
            restaurantName.innerText = resto.nom;
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
            const menu = await chargerMenu(restoCrous.code);
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
  }
  window.updateMap = updateMap;
  window.cacherActions = function() {
    afficherPanneau("filtres");
  };
  window.cacherMenuCrous = function() {
    afficherPanneau("filtres");
  };
})();
//# sourceMappingURL=index.js.map
