# Koyeb root-level Dockerfile: builds the Node.js backend in /backend
FROM node:20-alpine

WORKDIR /app

COPY backend/package*.json ./
RUN npm install --omit=dev

COPY backend/ ./

ENV NODE_ENV=production
ENV PORT=8000
EXPOSE 8000

CMD ["node", "server.js"]
