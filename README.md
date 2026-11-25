# 教学评价系统

前后端分离的教学评价系统，包含管理端与学生端。

- `teachingevaluationsystem-frontend/`：Vue 3、TypeScript、Vite。
- `teachingevaluationsystem-backend/`：Java 21、Spring Boot、MyBatis-Plus。
- `schema.sql`：MySQL 表结构。
- `data/indicator_migrated_import.sql`：评价指标初始化数据。

## 本地运行

准备 Java 21、Node.js 20.19+ 或 22.12+、MySQL 和 Redis。在 MySQL 中创建 `teaching_evaluation` 数据库并导入 `schema.sql`；按需导入指标数据。

后端通过环境变量 `DB_USERNAME`（默认 `root`）、`DB_PASSWORD`（默认空）配置数据库账号。默认连接本机 MySQL 3306、Redis 6379，后端端口为 8080。其他配置可使用 Spring Boot 标准环境变量覆盖。

也可创建 `teachingevaluationsystem-backend/src/main/resources/application-local.yml` 保存本机配置，该文件已被 Git 忽略：

```yaml
spring:
  datasource:
    username: root
    password: "your-local-password"
```

启动后端：

```sh
cd teachingevaluationsystem-backend
./mvnw spring-boot:run
```

启动前端：

```sh
cd teachingevaluationsystem-frontend
npm ci
npm run dev
```

访问 `http://127.0.0.1:5173`。学生入口为 `/student/login`，管理入口为 `/admin/login`，开发环境的 `/api` 请求代理到后端。

## 校验

```sh
cd teachingevaluationsystem-frontend
npm run type-check
npm run build
```

```sh
cd teachingevaluationsystem-backend
./mvnw -DskipTests compile
```

当前后端测试包含会写入数据库的管理员初始化测试，运行测试前应配置独立测试数据库。

## 提交范围

提交源码、依赖锁文件、数据库表结构及评价指标种子数据。学生及教师名单、Excel 导入文件、本机凭据、依赖目录、构建产物、日志和 IDE 缓存均不提交。
