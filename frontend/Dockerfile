FROM node:20
WORKDIR /app
COPY . .
RUN npm install
EXPOSE 5173
ENTRYPOINT ["./docker-entrypoint.sh"]
