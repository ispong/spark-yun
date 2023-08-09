FROM openjdk:8

VOLUME /etc/zhiqingyun/conf
VOLUME /root/.zhiqingyun

ARG ADMIN_PASSWORD='admin123'
ARG ACTIVE_ENV='local'

RUN mkdir -p /opt/zhiqingyun
RUN mkdir -p /etc/zhiqingyun/conf

COPY ./spark-yun-backend/spark-yun-main/build/libs/zhiqingyun.jar /opt/zhiqingyun/zhiqingyun.jar
COPY ./spark-yun-backend/spark-yun-main/src/main/resources/application-local.yml /etc/zhiqingyun/conf

EXPOSE 8080

ENV ADMIN_PASSWORD=${ADMIN_PASSWORD}
ENV ACTIVE_ENV=${ACTIVE_ENV}

CMD java -jar /opt/zhiqingyun/zhiqingyun.jar --spring.profiles.active=${ACTIVE_ENV} --spark-yun.admin-passwd=${ADMIN_PASSWORD} --spring.config.additional-location=/etc/zhiqingyun/conf/