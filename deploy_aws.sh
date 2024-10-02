#bin/bash
docker build --platform linux/amd64 -t schacherpro/mcc-server .
docker tag schacherpro/mcc-server:latest 533267060253.dkr.ecr.eu-central-1.amazonaws.com/schacherpro/mcc-server:latest
docker push 533267060253.dkr.ecr.eu-central-1.amazonaws.com/schacherpro/mcc-server:latest