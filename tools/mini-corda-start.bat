java  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 ^
  -Dco.paralleluniverse.fibers.verifyInstrumentation=true ^
  -jar  C:\Users\eric\.corda\corda5\corda-combined-worker-5.0.0.0-beta-1659748256231.jar ^
  --instanceId=0 -mbus.busType=DATABASE ^
  -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt ^
  -ddatabase.user=user -ddatabase.pass=password ^
  -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster
