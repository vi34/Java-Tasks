export CLASSPATH="Bank/out/production/Bank"
rmic -d $CLASSPATH examples.rmi.AccountImpl examples.rmi.BankImpl

java -classpath "Bank/out/production/Bank" examples.rmi.Server
