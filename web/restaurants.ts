export interface RestaurantResponse {
    idRestaurant: number;
    nom: string;
    adresse: string;
    lat: number;
    lon: number;
}

export async function recupererRestoNancy(proxyUrl: string): Promise<RestaurantResponse[]> {
    // On commence par récupérer l'ensemble des Restaurants sur Nancy
    // On utilise le Proxy Java qu'on a créé, et qui expose l'endpoint /api/restaurants/coordonnees qui retourne
    // les coordonnées de tous les restaurants de Nancy dans la base
    const url = `${proxyUrl}/api/restaurants/coordonnees`;

    const response = await fetch(url);
    const dataResto = (await response.json()) as { success: boolean; message: string; data: RestaurantResponse[] };

    if (!dataResto.success) {
        console.error("Erreur lors de la récupération des restaurants :", dataResto.message);
        return [];
    }

    // Transforme les données de l'API en interface Restaurant
    const restaurants = dataResto.data.map((resto: RestaurantResponse) => ({
        idRestaurant: resto.idRestaurant,
        nom: resto.nom,
        adresse: resto.adresse,
        lat: resto.lat,
        lon: resto.lon,
    }));

    return restaurants;
}
