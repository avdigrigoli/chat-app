package net.whispwriting.mantischat;

public class Message {

    private String message, type, from;
    private long timestamp;

    private Message(){

    }

    public Message(String message, String type, String from, String timestampString){
        this.message = message;
        this.type = type;
        this.from = from;
        this.timestamp = Long.parseLong(timestampString);
    }

    public String getMessage(){
        return message;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getFrom(){
        return from;
    }

    public void setFrom(String from){
        this.from = from;
    }

    public long getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(String timestampString){
        this.timestamp = Long.parseLong(timestampString);
    }
}
