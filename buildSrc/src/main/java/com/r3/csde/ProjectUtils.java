package com.r3.csde;

import static java.lang.Thread.sleep;

public class ProjectUtils {

    static void rpcWait(int millis) {
        try {
            sleep(millis);
        }
        catch(InterruptedException e) {
            throw new UnsupportedOperationException("Interrupts not supported.", e);
        }
    }




}
