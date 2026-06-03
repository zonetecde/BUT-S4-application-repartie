interface GBFSStationInfo {
    station_id: string;
    name: string;
    address?: string;
    lat: number;
    lon: number;
}

interface GBFSStationStatus {
    station_id: string;
    num_bikes_available: number;
    num_docks_available: number;
    status: string;
}

export interface StationVelib {
    id: string;
    nom: string;
    adresse: string;
    lat: number;
    lon: number;
    velosDisponibles: number;
    placesLibres: number;
}

export async function recupererVelibsNancy() {
    // On commence par récupérer l'ensemble des stations velibs sur Nancy
    const url = "https://api.cyclocity.fr/contracts/nancy/gbfs/v2/station_information.json";

    const response = await fetch(url);
    const dataStations = await response.json() as { data: { stations: GBFSStationInfo[] } };

    // Récupère aussi les détails des stations
    const urlDetails = "https://api.cyclocity.fr/contracts/nancy/gbfs/v2/station_status.json";
    const responseDetails = await fetch(urlDetails);
    const dataDetails = await responseDetails.json() as { data: { stations: GBFSStationStatus[] } };

    // Itère sur chaque station et récupère
    // - ses coordonnées
    // - son nom
    // - le nbre de vélo dispo
    // - le nbre de place de parking dispo
    const stations = dataStations.data.stations.map((station: GBFSStationInfo) => {
        // Récupère les détails de la station courante
        const details = dataDetails.data.stations.find((detail: GBFSStationStatus) => detail.station_id === station.station_id);

        return {
            id: station.station_id,
            nom: station.name,
            adresse: station.address,
            lat: station.lat,
            lon: station.lon,
            velosDisponibles: details?.num_bikes_available ?? 0,
            placesVoitureDisponibles: details?.num_docks_available ?? 0,
        };
    });

    return stations;
}
