# Déploiement

## Prérequis

-   Java version 17
-   Maven sur la machine utilisée pour compiler
-   accès au réseau de l'IUT
-   accès à la base de données Oracle

Il faut renseigner les paramètres de connexion à la base de données dans un fichier `.env`. Un exemple de contenu est donné dans le fichier `.env.example`.

## Compilation du serveur

Depuis une machine avec Maven installé, exécuter depuis la racine du projet :

```bash
mvn clean package
```

Le fichier généré est :

```text
target/restaurant-rmi-server.jar
```

## Lancement du serveur sur une machine de l'IUT

Comme Maven n'est pas disponible sur la machine de l'IUT, il faut copier au minimum ces fichiers sur la machine :

```text
restaurant-rmi-server.jar
.env
```

Les deux fichiers doivent être dans le même dossier. Depuis ce dossier, lancer :

```bash
export CLASSPATH=restaurant-rmi-server.jar
rmiregistry 1099 &
java -jar restaurant-rmi-server.jar
```

Si le projet complet est présent sur la machine de l'IUT, il est aussi possible de lancer depuis la racine du projet :

```bash
cd target
export CLASSPATH=restaurant-rmi-server.jar
rmiregistry 1099 &
java -jar restaurant-rmi-server.jar
```

## Lancement du proxy HTTP

Le proxy HTTP doit être lancé après le serveur RMI.
Il permet d'exposer les services RMI via une API REST, ce qui permet au site internet
d'accéder aux services RMI.
Depuis la racine du projet, exécuter :

```bash
java -cp "target/restaurant-rmi-server.jar" fr.zonetec.ProxyServeur localhost 1099 8081
```

Les arguments correspondent à l'hôte RMI (donc localhost vu que les services sont sur la même machine), au port RMI, puis au port HTTP du proxy (on l'expose sur le port 8081). Si le serveur RMI est sur une autre machine, il faut remplacer `localhost` par son adresse IP.

## Lancement du client de test

Depuis la racine du projet, lancer :

```bash
javac -cp "target/restaurant-rmi-server.jar" -d target/client client/RestaurantClient.java && java -cp "target/client:target/restaurant-rmi-server.jar" RestaurantClient localhost 1099
```

`localhost` fonctionne uniquement si le client est lancé sur la même machine que le serveur. Si le serveur est sur une autre machine, remplacer `localhost` par l'adresse IP du serveur :

```bash
javac -cp "target/restaurant-rmi-server.jar" -d target/client client/RestaurantClient.java && java -cp "target/client:target/restaurant-rmi-server.jar" RestaurantClient IP_DU_SERVEUR 1099
```

On devrait voir apparaître les réponses JSON des différentes requêtes faites au service RMI.
