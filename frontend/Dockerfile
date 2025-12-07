# Build stage
FROM node:20-alpine AS build

WORKDIR /app

# 의존성 파일 복사
COPY package*.json ./

# 의존성 설치
RUN npm install

# 소스 코드 복사
COPY . .

# 빌드
RUN npm run build

# Runtime stage
FROM nginx:alpine

# nginx 설정 복사 (Vite는 dist로 빌드)
COPY --from=build /app/build /usr/share/nginx/html

# nginx 설정 (SPA 라우팅 지원)
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
