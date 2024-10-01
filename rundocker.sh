./gradlew installDist
docker build -t mcc-server .
docker run -p 8080:8080 mcc-server
