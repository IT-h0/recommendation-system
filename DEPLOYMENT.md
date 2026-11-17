# 部署指南

## 开发环境部署

### 1. 数据库准备

```bash
# 登录MySQL
mysql -u root -p

# 执行数据库脚本
source D:/develop/Code/recommendation-system/sql/schema.sql
```

### 2. 后端部署

```bash
# 进入项目目录
cd D:/develop/Code/recommendation-system

# 编译项目
mvn clean install

# 启动后端
cd recommendation-server
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 3. 前端部署

```bash
# 进入前端目录
cd D:/develop/Code/recommendation-system/frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

## 生产环境部署

### 后端打包

```bash
cd recommendation-server
mvn clean package

# 生成的jar文件在 target/recommendation-server-1.0.0.jar

# 运行
java -jar target/recommendation-server-1.0.0.jar
```

### 前端打包

```bash
cd frontend
npm run build

# 生成的静态文件在 dist 目录

# 使用nginx部署
# 配置nginx.conf指向dist目录
```

### Nginx配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## Docker部署(可选)

### 后端Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/recommendation-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

### 前端Dockerfile

```dockerfile
FROM node:16-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
```

## 常见问题

### 1. 数据库连接失败
检查MySQL服务是否启动,用户名密码是否正确

### 2. Redis连接失败
检查Redis服务是否启动,密码是否正确

### 3. 前端无法访问后端API
检查后端服务是否启动,CORS配置是否正确

### 4. 推荐结果为空
需要先在数据库中添加用户评分数据,算法才能计算推荐结果
