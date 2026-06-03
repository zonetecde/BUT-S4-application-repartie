"use strict";
(() => {
  // restaurants.ts
  async function recupererRestoNancy() {
    const url2 = "http://localhost:8080/api/restaurants/coordonnees";
    const response = await fetch(url2);
    const dataResto = await response.json();
    const restaurants = dataResto.data.map((resto) => ({
      id: resto.idRestaurant,
      nom: resto.nom,
      adresse: resto.adresse,
      lat: resto.lat,
      lon: resto.lon
    }));
    return restaurants;
  }

  // appReservation.mts
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
    const restaurants = await recupererRestoNancy();
    restaurants.forEach((resto) => {
      const marker = L.marker([resto.lat, resto.lon]).addTo(map);
      marker.bindPopup(`<b>${resto.nom}</b><br>Adresse : ${resto.adresse}`);
    });
  });
})();
//# sourceMappingURL=indexReservation.js.map
