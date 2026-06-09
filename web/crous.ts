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

export async function recupererCrousNancy(): Promise<Restaurant[]> {
    // On récupère l'ensemble des crous de Nancy

    // 1. On charge toutes les régions dispo et on recherche celle avec "Nancy" dedans
    const url = "https://api.croustillant.menu/v1/regions";

    const response = await fetch(url);
    const dataRegions = (await response.json()) as { data: Region[] };

    const regionNancy = dataRegions.data.find((region) => region.libelle.toLowerCase().includes("nancy"));

    // Si Nancy n'est pas dans la liste alors indispo
    if (!regionNancy) {
        console.error("Région Nancy non trouvée");
        return [];
    }

    // Maintenant on récupère tout les restaurants de Nancy
    const codeNancy = regionNancy.code;

    const urlResto = `https://api.croustillant.menu/v1/regions/${codeNancy}/restaurants`;

    const responseResto = await fetch(urlResto);
    const dataResto = (await responseResto.json()) as { data: Restaurant[] };

    return dataResto.data;
}

export async function chargerMenu(idRestaurant: number): Promise<Menu> {
    // On récupère le menu du restaurant dont on a cliqué sur la carte
    const url = `https://api.croustillant.menu/v1/restaurants/${idRestaurant}/menu`;

    const response = await fetch(url);
    const dataMenu = (await response.json()) as any;

    const jour = dataMenu.data[0];
    const date = jour.date;

    // Le menu est sous un format bizarre dans la rep API du coup
    // on va juste en faire un str ici

    let menuStr = "";
    for (const repas of jour.repas) {
        const typeRepas = repas.type === "matin" ? "Matin" : "Soir";
        menuStr += `${typeRepas}\n`;
        for (const categorie of repas.categories) {
            menuStr += `  ${categorie.libelle} :\n`;
            for (const plat of categorie.plats) {
                menuStr += `    - ${plat.libelle}\n`;
            }
        }
    }

    return {
        date,
        menu: menuStr,
    };
}
