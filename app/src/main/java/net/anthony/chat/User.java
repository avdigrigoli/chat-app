package net.whispwriting.mantischat;

public class User {
    public String name;
    public String image;
    public String status;
    public String thumbImage;
    public String uid;

    public User(){

    }

    public User(String name, String image, String status, String uid) {
        this.name = name;
        this.image = image;
        this.status = status;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUid(){
        return uid;
    }

    public void setUid(String uid){
        this.uid = uid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getThumbImage() {
        return thumbImage;
    }

    public void setThumbImage(String image) {
        this.thumbImage = image;
    }

    @Override
    public String toString(){
        return "name: " + name + ", status: " + status + ", image: " + image;
    }
}