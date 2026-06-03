export interface Incident {
    id: string;
    type: string;
    description: string;
    lat: number;
    lon: number;
    location_description?: string;
}

export async function recupererIncidentsNancy() {
    // Le proxy java est exposé sur le port 8080
    // On appel l'endpoint /api/fetch qui fait appel au service RMI Fetch et  qui prend en paramètre l'url de la page à fetch
    const resp = await fetch("http://localhost:8080/api/fetch?url=https://carto.g-ny.eu/data/cifs/cifs_waze_v2.json");
    const respData = await resp.json(); // contient success et data
    if (!respData.success) {
        console.error("Erreur lors de la récupération des incidents :", respData.error);
        return [];
    }

    const incidents = JSON.parse(respData.data).incidents;

    // Pour chaque incidents
    return incidents.map((incident: any) => {
        // Extrait les coordonnées à partir de l'attribiut polyline
        const [lat, lon] = incident.location.polyline.split(' ');

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
