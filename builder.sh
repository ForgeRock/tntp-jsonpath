/Users/kcarron/Downloads/tomcat/bin/shutdown.sh

cd /Users/kcarron/Repositories/tntp-jsonpath/
mvn clean install

rm -f /Users/kcarron/Downloads/tomcat/logs/*

rm /Users/kcarron/Downloads/tomcat/webapps/openam/WEB-INF/lib/jsonpath-0.0.14.jar

cp /Users/kcarron/Repositories/tntp-jsonpath/target/jsonpath-0.0.14.jar /Users/kcarron/Downloads/tomcat/webapps/openam/WEB-INF/lib/

sleep 3
/Users/kcarron/Downloads/tomcat/bin/catalina.sh jpda start
tail -f /Users/kcarron/Downloads/tomcat/logs/catalina.out