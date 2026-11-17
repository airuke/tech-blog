# 技术博客与私人知识库

面向个人作者的 Markdown 内容平台。作者在同一个后台管理公开文章、草稿和私人笔记，访客可以搜索公开文章、按分类或月份浏览，以及提交待审核的评论。

**Java 17 · Spring Boot 3 · Spring Security · MyBatis-Plus · MySQL · Redis · Vue 3 · Pinia · md-editor-v3**

## 工程亮点

- **按文章权限控制正文和附件。** 访客接口查询已发布的公开文章。上传资源默认私有，关联到公开文章后可随文章访问。后台通过限时签名预览私有资源，读者端使用 DOMPurify 清理 Markdown 渲染后的 HTML。[文章权限](backend/src/main/java/com/airuike/blog/backend/service/ArticleService.java) · [资源权限](backend/src/main/java/com/airuike/blog/backend/service/ResourceService.java)
- **发布和撤回同步更新缓存。** 公开列表、详情和分类标签使用 Redis Cache Aside。业务更新与公共内容修订号在同一 MySQL 事务写入，读取按修订号构造缓存键，提交后清理旧键，Redis 异常时回源数据库。[公开缓存](backend/src/main/java/com/airuike/blog/backend/service/PublicCache.java)
- **编审流程与防覆盖。** 支持 Markdown 双栏预览、草稿、发布、撤回和附件管理。文章保存通过版本号拒绝过期写入，访客评论需审核才会公开。评论使用客户端提交标识处理重试，单实例按请求来源限制提交频率，支持博主回复和隐藏。[评论流程](backend/src/main/java/com/airuike/blog/backend/service/CommentService.java) · [Markdown 编辑器](frontend/src/views/admin/EditorView.vue)

```mermaid
flowchart LR
    A["Markdown 写作"] --> B["草稿或私人知识库"]
    B --> C["公开发布"]
    C --> D["MySQL 保存文章和公共修订号"]
    D --> E["读者请求 · Redis 按修订号缓存"]
    E --> F["访客留言 · 博主审核后展示"]
```

## 本地运行

需要 JDK 17、Docker Compose v2 和 Node.js 22.22.2+（22.x）或 24.15.0+（24.x）。以下命令在 Bash 中执行。

1. 从仓库根目录创建配置。编辑 `infra/.env`，可用 `openssl rand -hex 24` 分别生成 MySQL 和 Redis 密码，用 `openssl rand -base64 32` 生成 `BLOG_JWT_SECRET`。

   ```bash
   test -f infra/.env || install -m 600 infra/.env.example infra/.env
   ```

2. 保存配置后启动数据服务。

   ```bash
   docker compose --env-file infra/.env -f infra/compose.yaml up -d --wait
   ```

3. 启动后端。后端默认在本机 8081 监听，启动时初始化数据库表。

   ```bash
   set -a
   . ./infra/.env
   set +a
   cd backend
   ./mvnw spring-boot:run
   ```

4. 在另一终端启动前端。打开 `http://127.0.0.1:5174/admin` 创建博主账号，首页在 `http://127.0.0.1:5174`。建好账号后，将 `infra/.env` 中的 `BLOG_SETUP_ENABLED` 改为 `false` 并重新加载配置、重启后端。默认文件目录为 `storage/uploads/`。

   ```bash
   cd frontend
   npm ci
   npm run dev
   ```

## 验证

发布版通过 9 项后端集成测试，涵盖公开查询权限、过期编辑拒绝、评论审核、附件授权和修改密码后旧登录令牌失效。测试使用 H2 和临时文件目录。前端 ESLint 与 Vite 构建已通过。

在本地 MySQL + Redis 联调中，公开列表重复查询命中缓存，文章撤回后详情返回 404 且列表移除该条目。私人知识库条目保持在管理端，访客评论通过审核后出现在公开页。

```bash
(cd backend && ./mvnw test)
(cd frontend && npm ci && npm run lint && npm run build)
```

数据表、权限和缓存设计见 [技术设计](docs/design.md)。
