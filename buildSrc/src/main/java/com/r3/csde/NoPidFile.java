package com.r3.csde;


// todo: should be renamed as NoPidFileException
public class NoPidFile extends Exception {
    public NoPidFile(String message){
        super(message);
    }
}