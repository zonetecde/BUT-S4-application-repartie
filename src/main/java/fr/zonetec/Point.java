package fr.zonetec;

import java.io.Serializable;

/**
 * Classe représentant un point géographique personnalisé
 */
public class Point implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int idPoint;
    private double coordonneesX;
    private double coordonneesY;
    private String emoji;
    private String titre;
    private String description;
    
    /**
     * Constructeur par défaut
     */
    public Point() {
    }
    
    /**
     * Constructeur avec tous les paramètres
     */
    public Point(int idPoint, double coordonneesX, double coordonneesY, String emoji, String titre, String description) {
        this.idPoint = idPoint;
        this.coordonneesX = coordonneesX;
        this.coordonneesY = coordonneesY;
        this.emoji = emoji;
        this.titre = titre;
        this.description = description;
    }
    
    /**
     * Constructeur sans ID
     */
    public Point(double coordonneesX, double coordonneesY, String emoji, String titre, String description) {
        this.coordonneesX = coordonneesX;
        this.coordonneesY = coordonneesY;
        this.emoji = emoji;
        this.titre = titre;
        this.description = description;
    }

    public int getIdPoint() {
        return idPoint;
    }
    
    public void setIdPoint(int idPoint) {
        this.idPoint = idPoint;
    }
    
    public double getCoordonneesX() {
        return coordonneesX;
    }
    
    public void setCoordonneesX(double coordonneesX) {
        this.coordonneesX = coordonneesX;
    }
    
    public double getCoordonneesY() {
        return coordonneesY;
    }
    
    public void setCoordonneesY(double coordonneesY) {
        this.coordonneesY = coordonneesY;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
    
    public String getTitre() {
        return titre;
    }
    
    public void setTitre(String titre) {
        this.titre = titre;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "Point{" +
                "idPoint=" + idPoint +
                ", coordonneesX=" + coordonneesX +
                ", coordonneesY=" + coordonneesY +
                ", emoji='" + emoji + '\'' +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
