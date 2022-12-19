package com.r3.csde;

import java.io.File;

// Class to to provide utility functions to extract data from Corda jars
public class CordappJarReader {
    public CordappJarReader() {
    }


    private CordappJarReader readJar(String jarName) {
        File jarFile = new File(jarName);

        return this;
    }

    private File jarFile;
}
