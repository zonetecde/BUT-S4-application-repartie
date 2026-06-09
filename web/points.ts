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
 * Récupère tous les points géographiques via le service RMI
 */
export async function recupererPointsGeo(): Promise<PointGeo[]> {
    try {
        const response = await fetch("http://localhost:8080/ProxyServeur", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                service: "point",
                method: "recupererTousLesPoints",
                params: [],
            }),
        });

        const data = await response.json();
        
        if (data.success) {
            return data.result || [];
        } else {
            console.error("Erreur lors de la récupération des points:", data.error);
            return [];
        }
    } catch (error: any) {
        console.error("Erreur lors de l'appel au proxy :", error);
        return [];
    }
}

/**
 * Ajoute un nouveau point géographique
 */
export async function ajouterPointGeo(
    coordonneesX: number,
    coordonneesY: number,
    emoji: string,
    titre: string,
    description: string
): Promise<PointGeo | null> {
    try {
        const response = await fetch("http://localhost:8080/ProxyServeur", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                service: "point",
                method: "ajouterPoint",
                params: [coordonneesX, coordonneesY, emoji, titre, description],
            }),
        });

        const data = await response.json();

        if (data.success) {
            console.log("Point ajouté :", data.result);
            return data.result;
        } else {
            console.error("Erreur lors de l'ajout du point:", data.error);
            return null;
        }
    } catch (error: any) {
        console.error("Erreur lors de l'appel au proxy :", error);
        return null;
    }
}

/**
 * Supprime un point géographique
 */
export async function supprimerPointGeo(idPoint: number): Promise<boolean> {
    try {
        const response = await fetch("http://localhost:8080/ProxyServeur", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                service: "point",
                method: "supprimerPoint",
                params: [idPoint],
            }),
        });

        const data = await response.json();
        return data.success;
    } catch (error: any) {
        console.error("Erreur lors de l'appel au proxy :", error);
        return false;
    }
}
