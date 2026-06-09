# Déploiement

## Prérequis

-   Java version 17
-   Maven sur la machine utilisée pour compiler
-   Accès au réseau de l'IUT
-   Accès à la base de données Oracle (pour le service Restaurant)

Il faut renseigner les paramètres de connexion à la base de données dans un fichier `.env`. Un exemple de contenu est donné dans le fichier `.env.example`.

## Compilation

Depuis une machine avec Maven installé, exécuter depuis la racine du projet :

```bash
mvn clean package
```

Le fichier généré est :

```text
target/restaurant-rmi-server.jar
```

Copier ce `.jar` et le fichier `.env` sur **toutes** les machines qui vont héberger un service.

---

## Machine A - Service Restaurant

Le service Restaurant a besoin d'un accès à la base de données Oracle (fichier `.env` à placer dans le dossier courant).

```bash
# 1. Lancer l'annuaire RMI en arrière-plan
rmiregistry 1099 &

# 2. Lancer le service Restaurant
java -cp "target/restaurant-rmi-server.jar" fr.zonetec.LancerRestaurant
```

## Machine B - Service Fetch

```bash
# 1. Lancer l'annuaire RMI en arrière-plan
rmiregistry 1099 &

# 2. Lancer le service Fetch
java -cp "target/restaurant-rmi-server.jar" fr.zonetec.LancerFetch
```

---

## Machine C - Service Crous

```bash
# 1. Lancer l'annuaire RMI en arrière-plan
rmiregistry 1099 &

# 2. Lancer le service Crous
java -cp "target/restaurant-rmi-server.jar" fr.zonetec.LancerCrous
```

---

## Machine D - Proxy HTTP

Le proxy HTTP doit être lancé **après** les trois services RMI.

```bash
java -cp "target/restaurant-rmi-server.jar" fr.zonetec.ProxyServeur \
    8081 \
    <IP_RESTAURANT> 1099 \
    <IP_CROUS> 1099 \
    <IP_FETCH> 1099
```

Les 7 arguments sont :

| #   | Paramètre       | Défaut      | Description                     |
| --- | --------------- | ----------- | ------------------------------- |
| 1   | Port HTTP       | `8081`      | Port d'exposition de l'API REST |
| 2   | Hôte Restaurant | `localhost` | IP de la machine A              |
| 3   | Port Restaurant | `1099`      | Port RMI de la machine A        |
| 4   | Hôte Crous      | `localhost` | IP de la machine C              |
| 5   | Port Crous      | `1099`      | Port RMI de la machine C        |
| 6   | Hôte Fetch      | `localhost` | IP de la machine B              |
| 7   | Port Fetch      | `1099`      | Port RMI de la machine B        |

### Avec tout sur la même machine :

```bash
java -cp "target/restaurant-rmi-server.jar" fr.zonetec.ProxyServeur 8081 localhost 1099 localhost 1099 localhost 1099
```

---

## Client de test

Depuis la racine du projet :

```bash
javac -cp "target/restaurant-rmi-server.jar" -d target/client client/RestaurantClient.java && java -cp "target/client:target/restaurant-rmi-server.jar" RestaurantClient <IP_RESTAURANT> 1099
```
