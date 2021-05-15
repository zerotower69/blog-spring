FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD blog-1.0.0.jar blog.jar
EXPOSE 10000
EXPOSE 9043
ENTRYPOINT ["java","-jar","/blog.jar"]