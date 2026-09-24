# FineUI.Java.AppBox — 通用权限管理系统（Spring Boot + FineUI.Java）

FineUI.Java.AppBox 是一套可直接运行、可作为项目起点的后台权限管理系统样板：用户 / 角色 / 权限 / 部门 / 职称 / 菜单管理、
角色用户与角色权限分配、在线统计、系统配置、登录认证与三层权限校验。UI 用 **FineUI.Java**（社区版，永久免费商用）
的 `f:` 标签在 Thymeleaf 模板里声明，服务端事件写在页面类里；数据访问用 **Spring Data JPA**，数据库用零安装的 **H2**（文件模式）。

- 默认管理员：**admin / admin**（拥有全部权限）；另有约 200 个演示账号 `user0`、`user2`、…（用户名即密码）。
- 页面：登录 → 主框架（左侧菜单树按当前用户权限生成，右侧选项卡工作区）→ 各管理页在选项卡内以 IFrame 打开。

## 环境要求

- JDK 17 或更高（21 亦可）；Maven 3.6+。命令行 `java -version` / `mvn -version` 能正确输出即可。
- 无需安装数据库：H2 以文件模式运行，首次启动自动建表并写入演示数据。

## 依赖方式

项目文件已声明从公共软件包仓库获取的 Maven 包 `com.fineui:fineui-java`。正常联网构建时，包管理器会自动还原依赖；仓库不包含 FineUI.Core.dll、FineUI.Pro.dll、fineui-java.jar，也不包含 FineUI 框架源码。

前端运行时（`/F/FineUI.js`、主题、语言包）已内嵌在 jar 里，无需单独部署静态资源。

FineUI 页面默认开启严格脚本 CSP：只允许同源脚本文件和当前请求授权的模板脚本，不允许原生事件属性或字符串代码执行。`f:` 模板中的普通 `<script>` 由 FineUI.Java 自动加 nonce；升级 `fineui.version` 时须选用包含自动 nonce 处理器的版本。静态帮助页和普通错误响应不在此策略范围内。计算器和万年历的 HTML 与 JS 分文件维护，这是页面代码组织约定，不依赖 CSP 响应头。

## 构建

安装 JDK 17 与 Maven 后，在仓库根目录运行：

```bash
mvn package
```

## 运行

在仓库根目录启动：

```bash
mvn spring-boot:run
```

启动后浏览器打开 <http://localhost:8082/>（端口由 `src/main/resources/application.properties` 的 `server.port` 决定），用 admin / admin 登录。

**不需要授权文件**：本仓库引用的是公共软件包仓库中的社区版，社区版不做授权校验，克隆下来就能直接跑。

### 用 H2 控制台看数据（可选）

先用 admin 登录系统，再开 <http://localhost:8082/h2-console>。

⚠️ 表单里预填的 `jdbc:h2:~/test` 是 H2 控制台的出厂默认值，指向你家目录下一个不存在的库，
直接点连接会报 `Database "C:/Users/xxx/test" not found`。**把 JDBC URL 整个替换成本工程的：**

```
jdbc:h2:file:./data/appboxdb
```

用户名 `sa`，密码留空。连上后左侧就能看到 USERS / ROLES / POWERS 等业务表。

## 工程结构

```
src/main/java/com/fineui/java/appbox/
├─ AppBoxApplication.java          启动类
├─ config/                         Spring Security 安全链、拦截器注册、页面级配置（主题 cookie、水印）
├─ model/                          JPA 实体：User / Role / Power / Menu / Dept / Title / Online / Config / Log
├─ repository/                     Spring Data 仓库
├─ business/                       页面基类、登录身份与权限（AuthService）、@CheckPower 拦截器、在线统计、配置缓存、种子数据
└─ pages/                          页面类（@FineUIPage 声明路由；控件按 id 反射绑定到同名字段）
src/main/resources/
├─ templates/                      Thymeleaf 模板（路由 == 模板路径，小写连字符）；shared/layout.html 是母版
├─ static/res/                     样式、脚本、图标、自定义主题、登录插画等静态资源
├─ static/help/                    万年历 / 科学计算器两个纯静态帮助页
└─ application.properties          端口、H2、JPA、FineUI 全局配置
```

## 认证与权限

- **登录**：登录页按钮事件校验用户名密码（BCrypt 哈希），成功后把用户身份写入 Spring Security 会话；未登录访问页面重定向到
  `/login`，未登录的回发请求返回 401，由客户端提示后跳转登录页。
- **三层权限**：页面类上的 `@CheckPower("CoreUserView")` 在首屏与回发两条路径统一校验（不通过：首屏显示提示页、回发返回 403
  并弹出「您无权进行此操作！」）；页面里按钮与行内命令的可用状态按权限设置；删除 / 保存事件开头再校验一次。
- 权限名与菜单的浏览权限都在「权限管理」「菜单管理」里维护，角色权限在「角色权限管理」里勾选。

## 数据库说明

- 实体注解即表结构（`spring.jpa.hibernate.ddl-auto=update`），演示数据由 `AppBoxDataInitializer` 在库为空时写入一次。
- **重置数据**：停止应用，删除 `./data/appboxdb.mv.db`，重新启动即重建并重新写入演示数据。
- **换数据库**：改 `pom.xml` 里的驱动依赖与 `application.properties` 的连接串即可（MySQL / PostgreSQL / SQL Server 均可），业务代码无需改动；
  生产环境建议用 Flyway / Liquibase 管理表结构版本。

## 常见问题

1. **`Could not find artifact com.fineui:fineui-java`**：先检查 Maven 网络、代理与中央仓库镜像。
2. **页面空白、`/F/FineUI.js` 404**：确认依赖已经成功解析；也请确认 `application.properties` 里没有把资源指向别处。
3. **登录后页面右上角出现「无效授权」角标**：说明本机 `~/.m2` 里装的是企业版库而非社区版；社区版无授权提示。
4. **端口被占用**：改 `application.properties` 的 `server.port`。
5. **H2 控制台报 `Database "C:/Users/xxx/test" not found`**：没替换表单里预填的默认 JDBC URL，见上文「运行」里的 H2 控制台说明。

## 端到端冒烟（可选）

`e2e/` 下是两个 Playwright 脚本：`smoke.js`（登录 → 主框架 → 逐个打开全部管理页，收集控制台/HTTP 错误）和
`crud.js`（角色增删改、用户增删改、角色权限保存、改密校验）。应用跑起来后：

```bash
cd e2e
npm install
npx playwright install chromium
npm run smoke
npm run crud
```

可用环境变量 `BASE`（默认 `http://127.0.0.1:8082`）指向别的地址，`OUT` 指定截图输出目录。

## 发布历史

各版本的更新内容见 [CHANGELOG.md](CHANGELOG.md)。

## 许可边界

本仓库中由合肥三生石上软件有限公司拥有著作权的示例或应用项目源代码采用 [MIT 许可证](LICENSE)。FineUI 各端框架源码、二进制软件包、内嵌的 FineUI.js 运行时以及 FineUI 名称、标识和商标不属于 MIT 授权范围，仍适用各自的商业或社区版许可。具体边界见 [NOTICE.md](NOTICE.md)。

## 参与贡献

请先阅读 `CONTRIBUTING.md`。安全问题请按 `SECURITY.md` 私下报告。
