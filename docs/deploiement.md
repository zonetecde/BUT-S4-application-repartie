### Prérequis :

-   Java version 17
-   Maven
-   accès au réseau de l'IUT
-   accès à la base de données

Il faut renseigner les paramètres de connexion à la base de données dans le fichier `.env` à la racine du projet. Un exemple de contenu du fichier `.env` est donné dans le fichier `.env.example`.

### Compilation et lancement du service :

Depuis la racine du projet, exécuter les commandes suivantes :

```
mvn clean compile
mvn dependency:build-classpath '-Dmdep.outputFile=target\classpath.txt'
java -cp "target\classes;$(Get-Content target\classpath.txt)" fr.zonetec.App
```

N'oubliez pas de lancer `rmiregistry` avant.
