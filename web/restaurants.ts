
export interface RestaurantAPI {
    idRestaurant: number;
    nom: string;
    adresse: string;
    lat: number;
    lon: number;
}

export async function recupererRestoNancy() {
    // On commence par récupérer l'ensemble des Restaurants sur Nancy
    const url = "http://localhost:8081/api/restaurants/coordonnees";

    const response = await fetch(url);
    const dataResto = await response.json() as { data: RestaurantAPI[] };

    // Transforme les données de l'API en interface Restaurant
    const restaurants = dataResto.data.map((resto: RestaurantAPI) => ({
        id: resto.idRestaurant,
        nom: resto.nom,
        adresse: resto.adresse,
        lat: resto.lat,
        lon: resto.lon,
    }));

    return restaurants;
}