FROM azul/zulu-openjdk:22
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/mcc-server/ /app/
WORKDIR /app/bin
CMD ["./mcc-server"]