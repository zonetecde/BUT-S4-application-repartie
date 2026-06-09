/**
 * Interface TypeScript pour représenter un point géographique
 */
export interface PointGeo {
    idPoint: number;
    coordonneesX: number;
    coordonneesY: number;
    emoji: string;
    titre: string;
    description: string;
}

/**
 * Récupère tous les points géographiques via le proxy
 */
export async function recupererPointsGeo(proxyUrl: string): Promise<PointGeo[]> {
    try {
        const response = await fetch(`${proxyUrl}/api/points/list`);
        const data = await response.json();

        if (data.success) {
            return data.data || [];
        } else {
            console.error("Erreur lors de la récupération des points :", data.message);
            return [];
        }
    } catch (error: any) {
        console.error("Erreur lors de l'appel au proxy :", error);
        return [];
    }
}

/**
 * Ajoute un nouveau point géographique via le proxy
 */
export async function ajouterPointGeo(proxyUrl: string, coordonneesX: number, coordonneesY: number, emoji: string, titre: string, description: string): Promise<PointGeo | null> {
    try {
        const params = new URLSearchParams({
            x: String(coordonneesX),
            y: String(coordonneesY),
            emoji,
            titre,
            description,
        });
        const response = await fetch(`${proxyUrl}/api/points/add?${params}`, { method: "POST" });
        const data = await response.json();

        if (data.success) {
            console.log("Point ajouté");
            return data.data;
        } else {
            console.error("Erreur lors de l'ajout du point :", data.message);
            return null;
        }
    } catch (error: any) {
        console.error("Erreur lors de l'appel au proxy :", error);
        return null;
    }
}
