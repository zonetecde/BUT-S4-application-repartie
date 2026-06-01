package fr.zonetec;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionBuilder {
    private static final String DB_URL_ENV = "DB_URL";
    private static final String DB_USERNAME_ENV = "DB_USERNAME";
    private static final String DB_PASSWORD_ENV = "DB_PASSWORD";

    private ConnectionBuilder() {
    }

    /**
     * Crée une connexion à la base de données avec les variables d'environnement.
     *
     * @return connexion JDBC ouverte
     * @throws SQLException si la connexion JDBC échoue
     */
    public static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(
                getRequiredEnv(DB_URL_ENV),
                getRequiredEnv(DB_USERNAME_ENV),
                getRequiredEnv(DB_PASSWORD_ENV));
    }

    /**
     * Lit une variable d'environnement obligatoire.
     *
     * @param name nom de la variable d'environnement
     * @return valeur de la variable d'environnement
     */
    private static String getRequiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variable d'environnement manquante : " + name);
        }
        return value;
    }
}
