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

export interface TableDisponible {
    idTable: number;
    nbPlaces: number;
}

export async function recupererTablesRestaurant(nomRestaurant: string): Promise<TableDisponible[]> {
    const url = `http://localhost:8081/api/restaurants/tables?nomRestaurant=${encodeURIComponent(nomRestaurant)}`;

    const response = await fetch(url);
    const json = await response.json();

    if (!json.success && !json.succes) {
        throw new Error(json.message);
    }

    return Object.entries(json.data).map(([idTable, nbPlaces]) => ({
        idTable: Number(idTable),
        nbPlaces: Number(nbPlaces),
    }));
}

export async function reserverTableRestaurant(data: {
    nomRestaurant: string;
    idTable: number;
    dateHeure: string;
    nom: string;
    prenom: string;
    nombreConvives: number;
    telephone: string;
}) {
    const params = new URLSearchParams();

    params.append("nomRestaurant", data.nomRestaurant);
    params.append("idTable", String(data.idTable));
    params.append("dateHeure", data.dateHeure);
    params.append("nom", data.nom);
    params.append("prenom", data.prenom);
    params.append("nombreConvives", String(data.nombreConvives));
    params.append("telephone", data.telephone);

    const response = await fetch(`http://localhost:8081/api/restaurants/reserver?${params.toString()}`, {
        method: "POST",
    });

    const text = await response.text();
    console.log("Réponse réservation :", text);

    return JSON.parse(text);
}

export interface ReservationResponse {
    idRestaurant: number;
    idReservation: number;
    nomRestaurant: string;
    idTable: number;
    dateRes: string;
    nomClient: string;
    prenomClient: string;
    nbConvives: number;
    numTel: string;
}

export async function recupererReservations(): Promise<ReservationResponse[]> {
    const response = await fetch("http://localhost:8081/api/restaurants/reservations");
    const json = await response.json();

    if (!json.success) {
        throw new Error(json.message);
    }

    return json.data;
}