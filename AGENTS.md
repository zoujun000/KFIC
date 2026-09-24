# Repository Guidelines

货代管理系统（freight-system）是前后端分离的 monorepo，本文档适用于所有贡献者与 Codex 智能体。

## Project Structure & Module Organization

- `backend/`：Spring Boot 3 + Java 25 + Maven + MyBatis-Plus + MySQL + Redis + JWT。代码位于 `backend/src/main/java/com/freight/`，按 `controller → service/impl → mapper → entity` 分层，`dto/`、`vo/`、`config/`、`util/`、`security/`、`common/` 各司其职；测试位于 `backend/src/test/java/`。
- `frontend/`：Vue 3 + Vite + Element Plus + Pinia。页面在 `frontend/src/views/<模块>/<模块>View.vue`，接口统一放 `frontend/src/api/index.js`，另有 `store/`、`router/`、`composables/`、`utils/`、`styles/`。
- `sql/`：版本化迁移，命名 `V<yyyyMMdd>__<snake_case 描述>.sql`，须保持幂等（参考 `V20260728__ensure_columns_idempotent.sql`）。

## Build, Test, and Development Commands

- 后端（`backend/`）：`mvn spring-boot:run` 本地启动（端口 8080）、`mvn test` 运行测试、`mvn package` 打包。
- 前端（`frontend/`）：`npm install` 安装依赖、`npm run dev` 开发（Vite 热更新）、`npm run build` 生产构建、`npm run preview` 预览产物。
- 数据库与 Redis 连接配置见 `backend/src/main/resources/application.yml`（本地默认 `localhost:3306/KFIC`）。

## Coding Style & Naming Conventions

- Java 使用 4 空格缩进，类名 `PascalCase`、方法/变量 `camelCase`，沿用 Spring 与 MyBatis-Plus 既有写法。
- 前端使用 2 空格缩进，组件文件 `PascalCaseView.vue`，状态管理走 Pinia，请求统一走 `utils/request.js`。
- SQL 表名、列名用 `snake_case`。仓库未配置 ESLint/Prettier，新代码与相邻既有代码风格保持一致即可。

## Testing Guidelines

- 后端使用 JUnit 5（`spring-boot-starter-test`），`mvn test` 运行；测试类以 `Test` 结尾，权限/越权相关以 `<类名>SecurityTest` 命名（参考 `FreightOrderServiceImplSecurityTest`）。
- 前端暂无自动化测试；UI 改动需手动验证主要交互，并在 PR 中记录验证结果。

## Commit & Pull Request Guidelines

- 提交信息使用 Conventional Commits + 中文描述：`feat:` / `fix:` / `refactor:` / `chore:` / `perf:`，一次提交只做一个逻辑变更。
- PR 需说明改动内容与验收方式，UI 改动附截图，涉及数据结构的改动附迁移脚本说明，可关联的 Issue 一并链接。

## Work Log（工作记录）

- 每次在本项目完成修复或新增需求，必须更新 `worklog-vault/` 下的 Obsidian 工作日志：在对应日期笔记（`worklog-vault/YYYY-MM-DD.md`）中追加记录（类型、内容、涉及文件、备注），并同步维护 `worklog-vault/工作日志.md` 索引。
- 在 Obsidian 中打开 `worklog-vault/` 文件夹作为仓库即可查看和管理日志。
- 记录与代码改动同步完成，随 PR/提交一起提交，不补记、不遗漏。

## Agent-Specific Instructions

- 只改任务所需代码，不顺手清理无关内容（含历史 `.bak` 文件）。
- 后端改动后执行 `mvn test`，前端改动后执行 `npm run build` 确认无错误。
- 涉及数据库字段的新需求，新增幂等迁移脚本，不修改既有迁移文件。
- 完成修复或新增需求后，按 Work Log 章节要求更新 `worklog-vault/` 下的 Obsidian 工作日志。
- 如果我提出了需求模棱两可的时候要向我提问并给出方案，不要直接操作。
