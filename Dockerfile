FROM gcr.io/distroless/java17-debian11

ENV APP_HOME=/app

COPY ./Yiski5-Fat.jar $APP_HOME/

WORKDIR $APP_HOME

CMD ["Yiski5-Fat.jar"]
