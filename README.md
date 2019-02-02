# jsonToProtoFile WebApp

Http server:
 - receives a POST request with JSON object {“name:” “<name>”, “id”: <number>}
 - converts JSON object to protocol buffer format: 
Info {
	required string name=1;
	required int32 id=2;
}
 - saves protocol buffer object to file /infoObject.proto


#Build and Run

Project is expected to be built and run in Linux system. Docker service must be running.

To build and run the project run ./buldAndRun.sh.
This script:
 - builds the app with maven
 - builds docker image (named jsontoproto_webapp)
 - runs docker image
 - sends test post request (with cURL)
 - stops container


#Build

Build steps:

mvn clean package (maven war package)

docker build . -t jsontoproto (build docker image with previously build app)


#Run

Run steps:

docker run -d -p 8080:8080 --name jsontoproto_webapp jsontoproto (run app container)


#Implementation

Java servlet implementing POST requests.
Receives JSON, converts to protobuf.
Launches asynchronous context to write protobuf to file.
Writting to file is synchronized (only one thread writes to the file at a time). But the whole file is overriden for each write.
Asynchronous context timeout (timeout is 500 ms) can occur:
 - during write to file (eg. write to file got blocked). In this case, close file and reset file (reopen file for writting and close it again). [File Rollover]
 - having no write in progress. This can be either before the file has been open for writting (eg. thread waiting to enter synchronized block, while another is writting) or after. If no writting is in progress, do nothing.
Timeouts and errors originate a failed response with Http status 500 - Internal Server Error.


#Improvements

[File Rollover] should be improved. It doesn't consider synchronization, therefore file reset (close and open file again) could occur during another thread writting attempt and lead to failures.
[File write] on the file, there should be saved a list of protobuf objects. Each new received object should be added to the list and to the file. Currently it is only saved the latest received instace. #TODO FIX THIS


#Requirements

Java 8
Maven
Docker (Tomcat 9.0, to run without docker)


