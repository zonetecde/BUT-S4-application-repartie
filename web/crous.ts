export interface Region {
    code: number;
    libelle: string;
}

export interface Restaurant {
    code: number;
    nom: string;
    adresse: string;
    latitude: number;
    longitude: number;
    horaires: string;
}

export interface Menu {
    date: string;
    menu: string;
}

export async function recupererCrousNancy(proxyUrl: string): Promise<Restaurant[]> {
    // On récupère l'ensemble des crous de Nancy via le proxy Java
    const url = `${proxyUrl}/api/crous/restaurants?ville=Nancy`;

    const response = await fetch(url);
    const dataResto = (await response.json()) as { success: boolean; message: string; data: Restaurant[] };

    if (!dataResto.success) {
        console.error("Erreur lors de la récupération des restaurants CROUS :", dataResto.message);
        return [];
    }

    return dataResto.data;
}

export async function chargerMenu(proxyUrl: string, idRestaurant: number): Promise<Menu> {
    // On récupère le menu du restaurant dont on a cliqué sur la carte, via le proxy Java
    const url = `${proxyUrl}/api/crous/menu?idRestaurant=${idRestaurant}`;

    const response = await fetch(url);
    const dataMenu = (await response.json()) as { success: boolean; message: string; data: Menu };

    if (!dataMenu.success) {
        console.error("Erreur lors du chargement du menu :", dataMenu.message);
        return { date: "", menu: "Menu indisponible." };
    }

    return dataMenu.data;
}
