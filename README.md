# AI 智能饮食管理系统

一个基于 **AI 大模型 + 本地营养库** 的智能饮食健康管理系统。系统覆盖「食材营养查询、AI 营养分析、AI 食谱生成、拍照识菜、饮食打卡、体重管理、健康档案、营养助手、社区广场、数据看板、后台管理」等完整能力，帮助用户科学管理日常饮食与体重。

> 前端采用 Vue 3 + Vite，后端采用 Spring Boot + SQLite，前后端可**一体化打包成单个 jar**，双击脚本即可启动，无需单独部署前端或安装数据库。

---

## ✨ 功能特性

| 模块 | 说明 |
| --- | --- |
| 🥗 营养与食谱 | 查询 1000+ 食材营养数据，多选食材构建食谱并实时计算热量/蛋白质/碳水/脂肪 |
| 🤖 AI 食谱生成 | 根据已选食材 + 健康档案，调用大模型生成多套推荐菜单与详细做法 |
| 📷 拍照识菜 | 上传食物图片，AI 识别食材并估算营养信息 |
| 🧠 营养助手 | 大模型对话式营养咨询 |
| 📅 饮食打卡 | 记录每日饮食，AI 基于今日摄入 + 健康档案生成个性化建议 |
| ⚖️ 体重管理 | 记录体重、查看趋势曲线与 BMI |
| 🩺 健康档案 | 维护身高/体重/目标体重/年龄/性别/活动量/健康目标/忌口 |
| 📊 健康分析 | 营养摄入与体重变化可视化分析 |
| 👥 社区广场 | 发布动态、点赞、评论、关注、食谱收藏与评分 |
| 📈 数据看板 | 个人数据概览 |
| 🛠️ 后台管理 | 用户管理、内容管理、食材库管理、数据统计（ADMIN 角色） |

---

## 🧱 技术栈

- **前端**：Vue 3、Vite 5、Vue Router 4、Pinia、Element Plus、Axios、ECharts
- **后端**：Spring Boot 3.2、MyBatis-Plus 3.5、JWT (jjwt)、BCrypt
- **数据库**：SQLite（单文件，免安装，首次启动自动建表并导入营养库种子数据）
- **AI**：OpenAI 兼容协议大模型（默认对接阿里云百炼 DashScope，`qwen-plus`）

---

## 📁 目录结构

```
ai-diet-management-system/
├── frontend/                     # 前端工程 (Vue 3 + Vite)
│   ├── src/
│   │   ├── api/                  # 接口封装
│   │   ├── router/               # 路由配置
│   │   ├── stores/               # Pinia 状态管理
│   │   ├── utils/                # axios 等工具
│   │   ├── views/                # 页面组件
│   │   │   ├── login/            # 登录
│   │   │   ├── layout/           # 布局框架
│   │   │   ├── home/             # 首页
│   │   │   ├── food/             # 营养与食谱
│   │   │   ├── recipe/           # AI 食谱生成
│   │   │   ├── health/           # 健康档案/打卡/体重/分析/助手
│   │   │   ├── community/        # 社区广场
│   │   │   ├── dashboard/        # 数据看板
│   │   │   └── admin/            # 后台管理
│   │   └── main.js
│   └── package.json
├── backend/                      # 后端工程 (Spring Boot)
│   ├── src/main/java/com/diet/
│   │   ├── controller/           # 控制器
│   │   ├── service/              # 业务逻辑
│   │   ├── mapper/               # MyBatis-Plus Mapper
│   │   ├── entity/               # 实体
│   │   ├── dto/                  # 数据传输对象
│   │   ├── config/               # 配置(拦截器/静态资源/分页)
│   │   ├── common/               # 通用(返回结构/异常/常量)
│   │   ├── interceptor/          # JWT 拦截器
│   │   └── util/                 # 工具类
│   └── src/main/resources/
│       ├── schema.sql            # 建表脚本(15 张表)
│       ├── data.sql              # 种子数据(admin 账号 + 1010 条食材)
│       └── application.yml       # 主配置
├── docs/                         # 文档
│   ├── 项目说明书.md
│   └── 项目使用说明.md
├── start.bat                     # Windows 一键启动脚本
└── .gitignore
```

---

## 🚀 快速开始

### 环境要求

| 软件 | 版本 | 说明 |
| --- | --- | --- |
| Java | 17+ | 运行后端（JRE 即可，开发需 JDK） |
| Node.js | 18+ | 仅「源码构建前端」时需要 |
| Maven | 3.8+ | 仅「源码构建后端」时需要 |

> 详细安装与使用步骤见 [docs/项目使用说明.md](docs/项目使用说明.md)，功能与架构说明见 [docs/项目说明书.md](docs/项目说明书.md)。

### 方式一：源码运行（开发调试）

```bash
# 1. 启动后端 (默认 8080)
cd backend
mvn spring-boot:run

# 2. 启动前端 (默认 5173，自动代理 /api 到 8080)
cd frontend
npm install
npm run dev
```

浏览器访问 <http://localhost:5173>。

### 方式二：一体化打包（推荐部署）

```bash
# 1. 构建前端，产物输出到 backend/src/main/resources/static
cd frontend
npm install
npm run build

# 2. 打包后端为可运行 jar（会包含前端静态资源）
cd ../backend
mvn clean package

# 3. 一键启动（Windows 双击 start.bat 亦可）
cd ..
start.bat
```

启动后自动打开 <http://localhost:8080>，前端由后端同一端口托管，无需单独部署。

---

## 👤 默认账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | ADMIN（管理员） |

> 首次登录后请在「健康档案」或数据库层面尽快修改密码。普通用户可自行在登录页「注册」。

---

## 🤖 AI 功能配置（可选）

AI 相关功能（识菜 / 食谱生成 / 营养助手 / 饮食建议）依赖大模型。系统默认以 **`AI_MOCK=false`** 真实模式运行，密钥通过环境变量注入，**不写入仓库**。

```powershell
# Windows - 设置用户级环境变量(重启终端或重新运行 start.bat 后生效)
[Environment]::SetEnvironmentVariable("AI_API_KEY","sk-你的密钥","User")
[Environment]::SetEnvironmentVariable("AI_BASE_URL","https://dashscope.aliyuncs.com/compatible-mode/v1","User")
[Environment]::SetEnvironmentVariable("AI_MODEL","qwen-plus","User")
```

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_API_KEY` | 空 | 大模型密钥（必填） |
| `AI_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | OpenAI 兼容接口地址 |
| `AI_MODEL` | `qwen-plus` | 模型名称 |
| `AI_MOCK` | `false` | `true` 时关闭真实 AI，走本地规则引擎兜底 |

> 未配置密钥时，AI 功能不可用，但营养计算、打卡、体重、社区等本地功能完全不受影响。

---

## 📄 文档

- [项目使用说明](docs/项目使用说明.md)：安装步骤、运行方式、各功能使用指南、常见问题
- [项目说明书](docs/项目说明书.md)：项目定位、模块划分、系统架构、数据库设计、技术要点

---

## 📄 License

MIT License