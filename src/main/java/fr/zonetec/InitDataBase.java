package fr.zonetec;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class InitDataBase {

    public static void creerTables(Connection connection) {
        String restaurantTable =
            "CREATE TABLE Restaurant (" +
            " idRestaurant NUMBER(3) GENERATED AS IDENTITY," +
            " nom VARCHAR2(100) NOT NULL," +
            " adresse VARCHAR2(200)," +
            " coordonneesX NUMBER(9,6)," +
            " coordonneesY NUMBER(9,6)," +
            " CONSTRAINT pk_restaurant PRIMARY KEY (idRestaurant)" +
            ")";

        String tableTable = 
            "CREATE TABLE Table_Resto (" +
            " idTable NUMBER(3) GENERATED AS IDENTITY," +
            " idRestaurant NUMBER(3)," + 
            " nbPlaces NUMBER(2)," +
            " CONSTRAINT pk_table PRIMARY KEY (idTable)," +
            " CONSTRAINT fk_table_restaurant FOREIGN KEY (idRestaurant) REFERENCES Restaurant(idRestaurant)" +
            ")";

        String reservationTable =
            "CREATE TABLE Reservation (" +
            " idRestaurant  NUMBER(3)," +
            " idReservation NUMBER(3) GENERATED AS IDENTITY," +
            " nomClient VARCHAR2(50) NOT NULL," +
            " prenomClient VARCHAR2(50)," +
            " idTable NUMBER(3)," + 
            " nbConvives NUMBER(2)," +
            " numTel VARCHAR2(15)," +
            " CONSTRAINT pk_reservation PRIMARY KEY (idRestaurant, idReservation)," +
            " CONSTRAINT fk_res_restaurant FOREIGN KEY (idRestaurant) REFERENCES Restaurant(idRestaurant)," + 
            " CONSTRAINT fk_res_table FOREIGN KEY (idTable) REFERENCES Table_Resto(idTable)" +
            ")";

        String commandeTable = 
            "CREATE TABLE Commande (" +
            " idCommande NUMBER(3) GENERATED AS IDENTITY," +
            " dateCom DATE NOT NULL," +
            " datePaie DATE," +
            " montant NUMBER(6,2)," +
            " nbPers NUMBER(2)," + 
            " idTable NUMBER(3)," + 
            " CONSTRAINT pk_commande PRIMARY KEY (idCommande)," +
            " CONSTRAINT fk_com_table FOREIGN KEY (idTable) REFERENCES Table_Resto(idTable)" +
            ")";

        String platTable = 
            "CREATE TABLE Plat (" +
            " idPlat NUMBER(3) GENERATED AS IDENTITY," +
            " libelle VARCHAR2(100) NOT NULL," +
            " prixUnit NUMBER(5,2) NOT NULL," + 
            " qteStockee NUMBER(4)," +
            " CONSTRAINT pk_plat PRIMARY KEY (idPlat)" +
            ")";
   
        String contientTable = 
            "CREATE TABLE Contient (" +
            " idCommande NUMBER(3)," +
            " idPlat NUMBER(3)," +
            " quantite NUMBER(3) NOT NULL," +
            " CONSTRAINT pk_contient PRIMARY KEY (idCommande, idPlat)," +
            " CONSTRAINT fk_contient_commande FOREIGN KEY (idCommande) REFERENCES Commande(idCommande)," +
            " CONSTRAINT fk_contient_plat FOREIGN KEY (idPlat) REFERENCES Plat(idPlat)" +
            ")";

        try (Statement stmt = connection.createStatement()) {
            
            executeTable(stmt, restaurantTable, "Restaurant");
            executeTable(stmt, tableTable, "Table_Resto");
            executeTable(stmt, reservationTable, "Reservation");
            executeTable(stmt, commandeTable, "Commande");
            executeTable(stmt, platTable, "Plat");
            executeTable(stmt, contientTable, "Contient");
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création des tables : " + e.getMessage());
        }
    }

    public static void executeTable(Statement stmt, String table, String nomTable) throws SQLException {
        try {
            stmt.execute(table);
            System.out.println("Table" + nomTable + " créée");
        } catch (SQLException e) {
            if (e.getErrorCode() == 955) {
                // Si on a une exception c'est très certainement car la table a déjà été créée.
                System.out.println("Table " + nomTable + " déjà existante");
            } else {
                throw e;
            }
        }
    }


    public static void creerEntreesRestaurant(Connection connection) {
        String nbInsertion = "SELECT COUNT(*) FROM Restaurant";
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(nbInsertion)) {
            
            if (rs.next()) {
                int count = rs.getInt(1);

                if (count == 0) {
                    System.out.println("Table Restaurant vide --> Insertion des données");

                    String resto1 = "INSERT INTO Restaurant (nom, adresse, coordonneesX, coordonneesY) VALUES ('Grand Café Foy', '1 Place Stanislas, 54000 Nancy', 6.183181, 48.693452)";
                    String resto2 = "INSERT INTO Restaurant (nom, adresse, coordonneesX, coordonneesY) VALUES ('Le Bouche à Oreille', '42 Rue des Bastions, 54000 Nancy', 6.179320, 48.697010)";
                    String resto3 = "INSERT INTO Restaurant (nom, adresse, coordonneesX, coordonneesY) VALUES ('Transparence', '23 Rue Stanislas, 54000 Nancy', 6.181215, 48.691790)";
                    String resto4 = "INSERT INTO Restaurant (nom, adresse, coordonneesX, coordonneesY) VALUES ('Brasserie L''Excelsior', '50 Rue Henri-Poincaré, 54000 Nancy', 6.176211, 48.690183)";
                    String resto5 = "INSERT INTO Restaurant (nom, adresse, coordonneesX, coordonneesY) VALUES ('A la Table du Bon Roi Stanislas', '7 Rue Gustave Simon, 54000 Nancy', 6.181412, 48.692695)";


                    stmt.execute(resto1);
                    stmt.execute(resto2);
                    stmt.execute(resto3);
                    stmt.execute(resto4);
                    stmt.execute(resto5);
                    System.out.println("Insertion finie");
                } else {
                    System.out.println("La table Restaurant contient déjà des données");
                }
            } 
        } catch (SQLException e) {
            System.err.println("Erreur lors des insertions dans la table Restaurant : " + e.getMessage());
        }
    }
}
