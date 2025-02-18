FROM openjdk:23-jdk-slim

# JAR dosyanızın bulunduğu dizini ayarlıyoruz
WORKDIR /app

# Uygulama JAR dosyasını container'a kopyalıyoruz
COPY ./target/social-book-network.jar /app/social-book-network.jar

# Uygulamanın çalıştırılması için komut
CMD ["java", "-jar", "social-book-network.jar"]