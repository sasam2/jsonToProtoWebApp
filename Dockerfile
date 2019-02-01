FROM tomcat

RUN apt-get update && apt-get -y upgrade

WORKDIR /usr/local/tomcat

ADD ./target/webappassignment /usr/local/tomcat/webapps/webappassignment

EXPOSE 8080

CMD ["catalina.sh", "run"]
