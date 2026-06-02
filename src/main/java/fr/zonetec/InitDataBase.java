package fr.zonetec;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

public class InitDataBase {
    
    public static void createTables(Connection connection) {

        String restaurantTable = 
            "CREATE TABLE IF NOT EXISTS Restaurant (" +
            " idRestaurant NUMBER(3)," +
            " nom VARCHAR2(100) NOT NULL," +
            " adresse VARCHAR2(200)," +
            " coordonneesX NUMBER(9,6)," +
            " coordonneesY NUMBER(9,6),"  +
            " CONSTRAINT pk_restaurant PRIMARY KEY (idRestaurant)" +
            ")";

        String reservationTable = 
            "CREATE TABLE IF NOT EXISTS Reservation (" +
            "  idRestaurant NUMBER(3)," +
            "  idReservation NUMBER(3)," +
            "  nomClient VARCHAR2(50) NOT NULL," +
            "  prenomClient VARCHAR2(50)," +
            "  nbConvives NUMBER(2)," +
            "  numTel VARCHAR2(15)," +
            "  CONSTRAINT pk_reservation PRIMARY KEY (idRestaurant, idReservation)," +
            "  CONSTRAINT fk_res_restaurant FOREIGN KEY (idRestaurant) REFERENCES Restaurant(idRestaurant)" +
            ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(restaurantTable);
            System.out.println("Table Restaurant crée");

            stmt.execute(reservationTable);
            System.out.println("Table Réservation crée");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création des tables : " + e.getMessage());
        }

    }

}
