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

### Sur les machines de l'IUT :

On a remarqué que maven n'était pas disponible sur les machines de l'IUT. Il faut donc compiler le projet sur une machine où maven est installé, puis copier le fichier `target/restaurant-rmi-server.jar` sur les machines de l'IUT pour pouvoir lancer le service avec la commande suivante :

```
java -jar restaurant-rmi-server.jar
```
