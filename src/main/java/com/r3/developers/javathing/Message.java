package com.r3.developers.javathing;

import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.base.types.MemberX500Name;

// // A class which will contain a message, It must be marked with @CordaSerializable for Corda
//// to be able to send from one virtual node to another.
@CordaSerializable
public class Message {
    Message(MemberX500Name _sender, String _message) {
        sender = _sender;
        message = _message;
    }
    public MemberX500Name sender;
    public String message;
}
