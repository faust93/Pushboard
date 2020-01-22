package com.faust93.pushboard;

/**
 * Created by faust on 16.04.14.
 */
public class pbMessage {

    int _id;
    int _type;
    String _subject;
    String _message;

    public pbMessage(){

    }

    public pbMessage(int id, int type, String subject, String message){
        this._id = id;
        this._type = type;
        this._subject = subject;
        this._message = message;
    }

    public pbMessage(String subject, String message){
        this._subject = subject;
        this._message = message;
    }

    public pbMessage(int type, String subject, String message){
        this._type = type;
        this._subject = subject;
        this._message = message;
    }

    public int getID(){
        return this._id;
    }

    public void setID(int id){
        this._id = id;
    }

    public int getType(){
        return this._type;
    }

    public void setType(int type){
        this._type = type;
    }

    public String getSubject(){
        return this._subject;
    }

    public void setSubject(String subject){
        this._subject = subject;
    }

    public String getMessage(){
        return this._message;
    }

    public void setMessage(String message){
        this._message = message;
    }

}
