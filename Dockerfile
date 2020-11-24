ARG BASE_IMAGE=openjdk:13.0.2-jdk-oraclelinux7
FROM ${BASE_IMAGE}
MAINTAINER The Core Team
EXPOSE 8080
ENV APP_BASE=/core
COPY . ${APP_BASE}/
RUN yum install -y unzip
ENTRYPOINT bash ${APP_BASE}/entrypoint.sh