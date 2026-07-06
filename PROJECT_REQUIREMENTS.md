# HowMuch 项目要求

## 项目定位

HowMuch 是一款用于记录每件物品价值和持有成本的个人资产应用。当前前端是 Vue 3 + Tailwind CSS 的移动端 PWA，数据由 PocketBase 提供；Android 版本通过原生 WebView 打开已部署的 HowMuch 服务，从而生成可安装 APK。

## 功能要求

- 记录物品名称、类别、图标、购入价格、购买日期、状态、备注。
- 自动计算每件物品的持有天数、累计投入和日均持有成本。
- 支持维护/附加费用、退役回收金额和退役日期。
- 支持按类别筛选和按日期、价格、累计成本、日均成本排序。
- 支持目标日均价挑战，达标后显示达成状态。
- 支持 JSON 数据导出与导入备份。
- Android APK 首次启动需要填写已部署的 PocketBase/HowMuch 服务地址。

## 运行环境要求

### 服务端

- Linux 服务器或其他可长期运行 PocketBase 的环境。
- PocketBase 0.22.21 或兼容版本。
- 开放服务端口，README 示例使用 `9001`。
- 导入仓库中的 `pb_schema.json` 以初始化数据表结构。
- 将 `pb_public/index.html`、`pb_public/manifest.json`、`pb_public/icon.png` 放在 PocketBase 的 `pb_public` 目录。

### Android 构建

- JDK 17。
- Android SDK Platform 34。
- Android SDK Build Tools 34.0.0。
- Gradle 8.7。
- Android Gradle Plugin 8.5.2。

### Android 设备

- Android 6.0 API 23 或更高版本。
- 设备需要能访问部署好的 HowMuch 服务地址。

## 构建产物

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。
- Release APK 需要额外配置签名证书后构建。

## 当前限制

- Android APK 是 WebView 封装版本，业务数据仍存储在 PocketBase 服务端。
- 如果服务端不可访问，APK 无法读取或保存资产数据。
- 前端依赖 CDN 加载 Vue、Tailwind、Day.js、PocketBase SDK 和 Lucide，离线环境需要后续改为本地资源打包。
