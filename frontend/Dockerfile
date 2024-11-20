FROM node:20
WORKDIR /app
COPY . .
RUN npm install
EXPOSE 5173
ENTRYPOINT npm run dev -- --host
