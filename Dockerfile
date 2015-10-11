FROM java:8
VOLUME /tmp
EXPOSE 8081 8081
ADD /target/storer-0.0.1-SNAPSHOT.war /app.war
RUN bash -c 'touch /app.war'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.war", "--spring.profiles.active=prod"]
