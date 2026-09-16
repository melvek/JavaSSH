# 更新日志

## [1.1.0] - 2026-09-16

### Add

- **任务流程（task）机制**：支持将多个原子操作串联为一条有序的任务流程，按声明顺序依次执行
- **Action 抽象层**：将原子能力抽象为 `TaskAction`，当前内置 `command`、`push` 两个 Action
- **内置流程**：
    - `command` — 执行远程命令
    - `push` — 上传文件到远程服务器
    - `deploy` — 上传文件并执行远程命令（等价于 push + command）
- **自定义流程**：在 `inventory.yaml` 的 `flows` 段中按需定义任务流程，同名可覆盖内置流程
- **CLI 入口统一**：第一个参数即流程名，`jssh <task> [hosts...] [options]`
- **变量递归替换**：`${key}` 支持递归展开，最多 4 层，覆盖 global / group / host / step / CLI 五个变量来源
- **CLI 变量注入**：`-e` / `-f` / `-d` 等参数自动注入变量池，供流程中 `${...}` 引用
- **Action 帮助信息**：`jssh -h` 列出所有已实现的 Action 及其参数，便于编写自定义流程
- **统一异常 `JsshException`**：携带主机名与步骤名，便于定位失败位置
- **执行中断语义**：任一 Action 抛异常即中断当前主机剩余步骤，其他主机继续执行
- **步骤进度提示**：`[STEP x/y]` 显示当前步骤在流程中的位置

### Changed

- **CLI 结构重构**：由「命令 + 参数」改为「流程名 + 参数」
- **`command` / `push` / `deploy` 从命令降级为内置流程**，调用方式保持不变，兼容旧用法
- **帮助信息**：不再列出流程列表，改为列出所有可用 Action 及其参数
- **密码加密**：继续使用 Jasypt，主密钥仍硬编码于 `EncryptTool`，生产环境建议改为环境变量读取

### Removed

- `DeployCommand` 等独立命令类（功能由内置流程 `deploy` 提供）
- `JavaSSHCommand` 抽象基类（职责由 `CommandDispatcher` + `TaskExecutor` 承接）


---

## [1.0.0] - 2024-05-20

### 初始版本

- 基于 JSch 的轻量级 SSH 运维工具
- 支持批量上传文件（`push`）
- 支持批量执行远程命令（`command`）
- 支持批量部署（`deploy`）
- 支持 YAML 清单文件定义服务器组与主机
- 支持变量替换（`${key}`）
- 支持密码加密（Jasypt）
- 支持执行摘要输出