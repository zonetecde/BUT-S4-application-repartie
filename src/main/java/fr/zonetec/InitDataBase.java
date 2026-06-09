package fr.zonetec;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class InitDataBase {
    /**
     * Créer les tables nécessaires à l'application
     * @param connection
     */
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
            " reservee NUMBER(1) DEFAULT 0," +
            " nbPlaces NUMBER(2)," +
            " CONSTRAINT pk_table PRIMARY KEY (idTable)," +
            " CONSTRAINT fk_table_restaurant FOREIGN KEY (idRestaurant) REFERENCES Restaurant(idRestaurant)," +
            " CONSTRAINT chk_table_reservee CHECK (reservee IN (0, 1))" +  // booléen
            ")";

        String reservationTable =
            "CREATE TABLE Reservation (" +
            " idRestaurant  NUMBER(3)," +
            " idReservation NUMBER(3) GENERATED AS IDENTITY," +
            " nomClient VARCHAR2(50) NOT NULL," +
            " prenomClient VARCHAR2(50)," +
            " idTable NUMBER(3)," + 
            " dateRes DATE NOT NULL," +
            " nbConvives NUMBER(2)," +
            " numTel VARCHAR2(15)," +
            " CONSTRAINT pk_reservation PRIMARY KEY (idRestaurant, idReservation)," +
            " CONSTRAINT fk_res_restaurant FOREIGN KEY (idRestaurant) REFERENCES Restaurant(idRestaurant)," + 
            " CONSTRAINT fk_res_table FOREIGN KEY (idTable) REFERENCES Table_Resto(idTable)" +
            ")";

        String commandeTable = 
            "CREATE TABLE Commande (" +
            " idCommande NUMBER(3) GENERATED AS IDENTITY," +
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

        String pointGeoTable =
            "CREATE TABLE Point_Geo (" +
            " idPoint NUMBER(3) GENERATED AS IDENTITY," +
            " coordonneesX NUMBER(9,6) NOT NULL," +
            " coordonneesY NUMBER(9,6) NOT NULL," +
            " emoji NVARCHAR2(10) NOT NULL," +
            " titre VARCHAR2(100) NOT NULL," +
            " description VARCHAR2(500)," +
            " CONSTRAINT pk_point_geo PRIMARY KEY (idPoint)" +
            ")";

        try (Statement stmt = connection.createStatement()) {
            
            executeTable(stmt, restaurantTable, "Restaurant");
            executeTable(stmt, tableTable, "Table_Resto");
            executeTable(stmt, reservationTable, "Reservation");
            executeTable(stmt, commandeTable, "Commande");
            executeTable(stmt, platTable, "Plat");
            executeTable(stmt, contientTable, "Contient");
            executeTable(stmt, pointGeoTable, "Point_Geo");
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création des tables : " + e.getMessage());
        }
    }

    /**
     * Méthode qui permet d'exécuter une table
     * @param stmt le statement
     * @param table la table à exécuter
     * @param nomTable le nom de la table
     * @throws SQLException
     */
    public static void executeTable(Statement stmt, String table, String nomTable) throws SQLException {
        try {
            stmt.execute(table);
            System.out.println("Table " + nomTable + " créée");
        } catch (SQLException e) {
            if (e.getErrorCode() == 955) {
                // Si on a une exception c'est très certainement car la table a déjà été créée.
                System.out.println("Table " + nomTable + " déjà existante");
            } else {
                throw e;
            }
        }
    }


    /**
     * Ajout des entrées de test dans la table Restaurant si elle est vide.
     * @param connection connexion JDBC à la base de données
     */
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


    /**
     * Ajout des entrées de test dans la table Plat si elle est vide.
     * @param connection connexion JDBC à la base de données
     */
    public static void creerEntreesPlat(Connection connection) {
        String nbInsertion = "SELECT COUNT(*) FROM Plat";
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(nbInsertion)) {
            
            if (rs.next()) {
                int count = rs.getInt(1);

                if (count == 0) {
                    System.out.println("Table Plat vide --> Insertion des données");

                    String plat1 = "INSERT INTO Plat (libelle, prixUnit, qteStockee) VALUES ('Quiche Lorraine traditionnelle', 12.50, 40)";
                    String plat2 = "INSERT INTO Plat (libelle, prixUnit, qteStockee) VALUES ('Bouchée à la reine et frites', 16.90, 25)";
                    String plat3 = "INSERT INTO Plat (libelle, prixUnit, qteStockee) VALUES ('Pâté lorrain maison', 8.50, 50)";
                    String plat4 = "INSERT INTO Plat (libelle, prixUnit, qteStockee) VALUES ('Escalope de veau à la crème', 21.00, 15)";
                    String plat5 = "INSERT INTO Plat (libelle, prixUnit, qteStockee) VALUES ('Tarte aux mirabelles de Lorraine', 6.90, 35)";
                    String plat6 = "INSERT INTO Plat (libelle, prixUnit, qteStockee) VALUES ('Crème brûlée', 7.20, 25)";

                    stmt.execute(plat1);
                    stmt.execute(plat2);
                    stmt.execute(plat3);
                    stmt.execute(plat4);
                    stmt.execute(plat5);
                    stmt.execute(plat6);

                    System.out.println("Insertion finie");
                } else {
                    System.out.println("La table Plat contient déjà des données");
                }
            } 
        } catch (SQLException e) {
            System.err.println("Erreur lors des insertions dans la table Plat : " + e.getMessage());
        }
    }


    /**
     * Ajout des entrées de test dans la table Table si elle est vide.
     * @param connection connexion JDBC à la base de données
     */
    public static void creerEntreesTable(Connection connection) {
        String nbInsertion = "SELECT COUNT(*) FROM Table_Resto";
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(nbInsertion)) {
            
            if (rs.next()) {
                int count = rs.getInt(1);

                if (count == 0) {
                    System.out.println("Table Table_Resto vide --> Insertion des données");

                    String table1 = "INSERT INTO Table_Resto (idRestaurant, reservee, nbPlaces) VALUES (1, 0, 2)"; // Table de 2 personnes au Grand Café Foy (libre)
                    String table2 = "INSERT INTO Table_Resto (idRestaurant, reservee, nbPlaces) VALUES (1, 0, 4)"; // Table de 4 personnes au Grand Café Foy (libre)
                    String table3 = "INSERT INTO Table_Resto (idRestaurant, reservee, nbPlaces) VALUES (2, 0, 6)"; // Table de 6 personnes au Bouche à Oreille (libre)
                    String table4 = "INSERT INTO Table_Resto (idRestaurant, reservee, nbPlaces) VALUES (4, 0, 4)"; // Table de 4 personnes à L'Excelsior (libre)
                    String table5 = "INSERT INTO Table_Resto (idRestaurant, reservee, nbPlaces) VALUES (4, 0, 2)"; // Table de 2 personnes à L'Excelsior (libre)

                    stmt.execute(table1);
                    stmt.execute(table2);
                    stmt.execute(table3);
                    stmt.execute(table4);
                    stmt.execute(table5);

                    System.out.println("Insertion finie");
                } else {
                    System.out.println("La table Table_Resto contient déjà des données");
                }
            } 
        } catch (SQLException e) {
            System.err.println("Erreur lors des insertions dans la table Table_Resto : " + e.getMessage());
        }
    }
}
