#bin/bash
./gradlew installDist
docker build --platform linux/amd64 -t mcc-server .
docker run -p 8080:8080 mcc-server
