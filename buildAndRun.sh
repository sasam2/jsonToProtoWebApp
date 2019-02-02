#!/bin/bash

mvn clean package

if [ $? -ne 0 ];then 
echo "Build app failed."
exit $?; fi

systemctl is-active --quiet docker

if [ $? -ne 0 ];then 
echo "Docker service is not started."
exit $?; fi

docker build . -t jsontoproto

if [ $? -ne 0 ];then 
echo "Docker build failed."
exit $?; fi

docker run -d -p 8080:8080 --name jsontoproto_webapp jsontoproto

if [ $? -ne 0 ];then 
echo "Docker run failed."
exit $?; fi

sleep 10 

curl -d '{"name":"ivo","id":12}' -H "Content-Type:application/json" -X POST http://localhost:8080/webappassignment/AppServlet
echo ""

if [ $? -ne 0 ];then 
echo "Test app failed."; fi

echo ""
echo "Press key to exit."
read 

docker logs jsontoproto_webapp

docker container stop jsontoproto_webapp

docker container rm jsontoproto_webapp

