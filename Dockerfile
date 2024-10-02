FROM openjdk:11-jdk
EXPOSE 8080
RUN mkdir /app
COPY ./build/install/mcc-server/ /app/
WORKDIR /app/bin
CMD ["./mcc-server"]