# CSDE-cordapp-template-kotlin


To help make the process of prototyping CorDapps on Corda 5 beta releases more straight forward we have developed the Cordapp Standard Development Environment (CSDE). 

The CSDE is obtained by cloning this CSDE-Cordapp-Template-Kotlin to your local machine. The CSDE provides:

 - A pre-setup Cordapp Project which you can use as a starting point to develop your own prototypes.

 - A base Gradle configuration which brings in the dependencies you need to write and test a Corda 5 Cordapp.

 - A set of Gradle helper tasks which speed up and simplify the development and deployment process.

 - Debug configuration for debugging a local Corda cluster.

 - The MyFirstFlow code which forms the basis of this getting started documentation, this is located in package com.r3.developers.csdetemplate.flowexample

 - A UTXO example in package com.r3.developers.csdetemplate.utxoexample packages

 - Ability to configure the Members of the Local corda Network.

Note, the CSDE is experimental, we may or may not release it as part of Corda 5.0, in part based on developer feedback using it.  

To find out how to use the CSDE please refer to the getting started section in the Corda 5 Beta 1.1 documentation at https://docs.r3.com/

(Note, to use the CSDE you must have installed the Corda Cli, make sure the version matches the version of Corda)

