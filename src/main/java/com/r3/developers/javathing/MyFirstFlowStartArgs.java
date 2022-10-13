package com.r3.developers.javathing;

import net.corda.v5.base.types.MemberX500Name;

// // A class to hold the arguments required to start the flow
//class MyFirstFlowStartArgs(val otherMember: MemberX500Name)
public class MyFirstFlowStartArgs {
    public MemberX500Name othermember;

    public MyFirstFlowStartArgs(MemberX500Name othermember) {
        this.othermember = othermember;
    }
}
