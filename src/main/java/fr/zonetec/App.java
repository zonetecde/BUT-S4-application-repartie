package fr.zonetec;

import java.sql.Connection;
import java.sql.SQLException;

public class App {
    /**
     * Crée une connexion à la base puis affiche un message.
     *
     * @param args arguments de la ligne de commande
     * @throws SQLException si la connexion JDBC échoue
     */
    public static void main(String[] args) throws SQLException {
        try (Connection connection = ConnectionBuilder.createConnection()) {
            System.out.println("Hello World!");
        }
    }
}