# 个人博客与私人知识库

依据原《Java Web 项目设计方案》实现单博主的内容平台，参考 NBlog 的前后台业务划分及 Ruyu-Blog 的 Spring Boot 3 / Vue 3 技术路线，代码、接口和页面独立实现。

公开入口包括文章列表、文章详情、分类、标签、搜索及按月归档。管理入口包括首次创建博主账号、登录、概览、文章管理、草稿、私人知识库、收藏、Markdown 编辑、分类标签、资源管理和站点设置。公开查询必须同时满足 PUBLISHED 和 PUBLIC，匿名请求不得获知草稿、私人文章或其专属分类标签。

后端保留 Java 17、Spring Boot 3.5.16、8081；新增 MyBatis-Plus、Spring Security JWT、MySQL 和 Redis。前端采用 Vue 3、Vite、Pinia、Axios、Element Plus、md-editor-v3；使用 5174。MySQL 与 Redis 为博客独立容器，分别绑定本机 3307 与 6380，内容表和凭据独立于网盘。

数据库以 article / category / tag / article_tag 维护内容关系；sys_user 只允许一个博主；file_resource 记录本地文件元数据；blog_state 维护站点设置及公共内容修订号。文章保存使用 version 防止同时编辑覆盖，文章和标签关系在同一事务提交。

公开列表、详情和分类标签采用 Cache Aside。写入提交后失效缓存，同时提升数据库中的公共修订号，以修订号构建缓存 key；Redis 不可用时回源 MySQL。公共缓存中不放入草稿和私人正文。

上传先保存至博客自己的 storage/uploads，文件名使用随机 UUID。资源默认私有；公开文章引用的资源可匿名读取，其他资源需要博主身份或短期、限定文件的签名预览地址。Markdown 渲染使用 DOMPurify 清理 HTML，后台预览使用短期签名地址，正文保存稳定资源 URL。

初次创建账号需要启用 `BLOG_SETUP_ENABLED`，且请求必须来自回环地址。数据库使用固定的博主 ID 和唯一约束防止重复初始化。JWT 校验签名、过期时间、issuer、audience 与账号的 tokenVersion。改密码后旧令牌失效。

响应采用 {code,message,data}；列表分页 {items,total,page,pageSize}；验证失败 400、未登录 401、权限不足 403、不可见内容 404、版本冲突或关系冲突 409。删除分类/标签时明确校验关联文章。保存失败必须保留编辑内容；未保存内容离开时确认。

复验方式见 [仓库说明](../README.md#验证)。

界面方向以作者的计算机科学、ROS 2、算法与控制学习背景为依据，采用技术文档样式、蓝灰色系和无衬线字体。首页方向入口复用已有分类或关键词搜索，不新增项目管理数据表，不写入示例文章。闭环控制图属于静态主题示意。
