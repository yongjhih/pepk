FROM openjdk:8

ADD . /src
WORKDIR /src

RUN ./gradlew shadowJar

FROM java:8

#ENV ARTIFACT_SRC=/src/build/libs/pepk.jar

COPY --from=0 /src/build/libs/pepk.jar /app/pepk.jar

ENTRYPOINT ["java", "-jar", "/app/pepk.jar"]
