CREATE TABLE IF NOT EXISTS sys_user (
 id BIGINT NOT NULL PRIMARY KEY,
 username VARCHAR(32) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 nickname VARCHAR(40) NOT NULL,
 status INT NOT NULL DEFAULT 1,
 token_version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE IF NOT EXISTS category (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(40) NOT NULL UNIQUE,
 description VARCHAR(240) NOT NULL DEFAULT '',
 sort_order INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE IF NOT EXISTS tag (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(40) NOT NULL UNIQUE,
 description VARCHAR(240) NOT NULL DEFAULT '',
 sort_order INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE IF NOT EXISTS article (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(160) NOT NULL,
 summary VARCHAR(400) NOT NULL DEFAULT '',
 content LONGTEXT NOT NULL,
 cover VARCHAR(255) NULL,
 category_id BIGINT NULL,
 status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',
 visibility VARCHAR(12) NOT NULL DEFAULT 'PUBLIC',
 is_top BOOLEAN NOT NULL DEFAULT FALSE,
 is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
 view_count BIGINT NOT NULL DEFAULT 0,
 word_count INT NOT NULL DEFAULT 0,
 version BIGINT NOT NULL DEFAULT 1,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 published_at TIMESTAMP(6) NULL,
 CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES category(id)
);
CREATE TABLE IF NOT EXISTS article_comment (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 article_id BIGINT NOT NULL,
 submission_id CHAR(36) NOT NULL UNIQUE,
 nickname VARCHAR(40) NOT NULL,
 content TEXT NOT NULL,
 status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
 reply_content TEXT NULL,
 reply_updated_at TIMESTAMP(6) NULL,
 reviewed_at TIMESTAMP(6) NULL,
 version BIGINT NOT NULL DEFAULT 1,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 INDEX idx_comment_article (article_id,status,created_at,id),
 INDEX idx_comment_status (status,created_at,id),
 CONSTRAINT fk_comment_article FOREIGN KEY(article_id) REFERENCES article(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS article_tag (
 article_id BIGINT NOT NULL,
 tag_id BIGINT NOT NULL,
 PRIMARY KEY(article_id,tag_id),
 CONSTRAINT fk_article_tag_article FOREIGN KEY(article_id) REFERENCES article(id) ON DELETE CASCADE,
 CONSTRAINT fk_article_tag_tag FOREIGN KEY(tag_id) REFERENCES tag(id)
);
CREATE TABLE IF NOT EXISTS file_resource (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 object_key VARCHAR(80) NOT NULL UNIQUE,
 content_type VARCHAR(100) NOT NULL,
 size BIGINT NOT NULL,
 is_public BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE IF NOT EXISTS blog_state (
 id INT NOT NULL PRIMARY KEY,
 title VARCHAR(50) NOT NULL,
 description VARCHAR(180) NOT NULL,
 about_markdown TEXT NOT NULL,
 revision BIGINT NOT NULL DEFAULT 0
);
INSERT INTO blog_state (id,title,description,about_markdown,revision)
 SELECT 1,'Airuike 技术博客','计算机科学与技术 · ROS 2 · 算法与控制。记录实现过程、实验结果与技术复盘。',
 CONCAT('# 关于这个技术博客',CHAR(10),'',CHAR(10),'我是一名工科生，学习计算机科学与技术、ROS 2、算法与控制。',CHAR(10),'',CHAR(10),'## 记录的内容',CHAR(10),'',CHAR(10),'- 计算机基础、C++ / Python 和软件工程实践',CHAR(10),'- ROS 2 节点通信、机器人开发与系统集成',CHAR(10),'- 算法推导、数据结构与代码实现',CHAR(10),'- 系统建模、控制算法与实验复盘',CHAR(10),'',CHAR(10),'这里保存公开技术文章与私人知识库，以问题、实现和验证为主线，保留关键代码、配置与实验条件。'),0
 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM blog_state WHERE id=1);
