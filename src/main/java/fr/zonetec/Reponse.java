package fr.zonetec;

/**
 * Classe de réponse qui va permettre d'avoir un même format pour toutes les réponses.
 * Ça permet qu'après quand on appelera les méthodes du service RMI on puisse toujours
 * faire le même traitement de la réponse (ex: vérifier si c'est une erreur ou pas, etc).
 */
public class Reponse {
    private boolean success;
    private String message;
    private Object data;

    public Reponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public String toJson(){
        return "";
    }
}
