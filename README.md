[![Language](https://img.shields.io/badge/Language-Java-blue.svg)](https://java.dev)
[![Version](https://img.shields.io/github/v/release/melvek/JavaSSH?include_prereleases)](https://github.com/melvek/JavaSSH/releases/latest)
![Supports](https://img.shields.io/badge/Supports-windows,%20Linux-orange)
[![LICENSE](https://img.shields.io/github/license/melvek/JavaSSH)](LICENSE)

# JavaSSH 使用说明

JavaSSH 是一个基于 JSch 开发的轻量级运维工具。

采用「Action + Flow」模型：Action 是原子能力（执行命令、上传文件等），Flow 是由若干 Action 组成的有序任务流程。
用户通过 YAML 清单文件定义服务器组、主机、认证信息、业务参数和任务流程，即可一键完成批量部署、文件推送与命令执行。

- 内置流程：`command` / `push`
- 自定义流程：在 YAML 的 `tasks` 段中按需编排，同名覆盖内置流程
- 扩展：通过实现 `TaskAction` 快速扩展指令

---

## 环境要求

- JDK 8 或更高版本
- 远程服务器需支持 SSH / SFTP
- 若使用 `-z` 压缩上传，远程服务器需支持 `unzip` 命令（开发中，敬请期待）

---

## 安装

从 [GitHub Releases 页面](https://github.com/melvek/JavaSSH/releases) 下载预编译的 JAR 文件及执行脚本 `deploy.bat`(windows)或者`deploy.sh`(linux)。

也可以通过编译安装：

```bash
git clone --depth 1 https://github.com/melvek/JavaSSH.git
cd JavaSSH
mvn clean package
```

构建完成后，在 `target/` 目录下会生成可执行 JAR 文件，例如 `jssh-x.x.x.jar`。

为其创建别名，便于使用：

```bash
alias javassh='java -jar /path/to/jssh-1.0.0.jar'
```

---

## 快速开始

### 1. 创建清单文件 `inventory.yaml`

```yaml
global_vars:
  port: 22
  username: deploy
  password: 2dO7ObeRBjqyuKkMpV6Xkg==
  command: "systemctl restart my-app"

servers:
  prod-trans:
    vars:
      service_path: /opt/trans/
      command: "sh /opt/trans/restart.sh"
    hosts:
      prod_trans_1: 192.168.1.1
      prod_trans_2:
        host: 192.168.1.2
        port: 2222
        username: root
        password: 2dO7ObeRBjqyuKkMpV6Xkg==
        remote_path: /data/app/

tasks:
  release:
    steps:
      - name: "上传新版本"
        action: push
        with:
          file: "./dist/${app_name}-${version}.jar"
          dest: "${service_path}"
      - name: "备份旧版本"
        action: command
        with:
          command: "cp ${service_path}app.jar ${service_path}app.jar.bak"
      - name: "替换文件"
        action: command
        with:
          command: "mv ${service_path}${app_name}-${version}.jar ${service_path}app.jar"
      - name: "重启服务"
        action: command
        with:
          command: "systemctl restart ${app_name}"
```

说明：

- `global_vars`：全局默认参数，会被服务器组/主机继承
- `servers`：服务器组定义
    - `vars`：该组的变量，会合并到组内所有主机
    - `hosts`：主机列表，支持两种写法
        - 简写形式：`主机名: IP`，直接使用全局参数
        - 完整形式：`主机名: { host, port, username, password, ... }`
- `tasks`：用户自定义任务流程，可覆盖内置流程，也可新增
- 除 `host`、`port`、`username`、`password` 外的所有字段都会进入 `extraFields`，可用于变量替换

### 2. 加密密码

明文密码存在安全风险，JavaSSH 使用 Jasypt 加密密码。运行 `EncryptTool` 的 `main` 方法生成密文，将输出结果填入 YAML。

注意：主密钥 `SEC_KEY` 硬编码在 `EncryptTool` 中，生产环境请改为从环境变量或配置中心读取。

---

## 命令总览

```
jssh <task> [hosts...] [options]
```

第一个参数即任务流程名称

### 全局选项

| 选项   | 长选项           | 参数       | 说明                             |
|------|---------------|----------|--------------------------------|
| `-i` | `--inventory` | `file`   | 指定服务器清单文件（默认 `inventory.yaml`） |
| `-l` | `--list`      |          | 仅显示服务器列表，不执行操作                 |
| `-P` | `--port`      | `int`    | 覆盖清单中的服务器连接端口                  |
| `-u` | `--user`      | `string` | 覆盖清单中的服务器用户名                   |
| `-p` | `--password`  | `string` | 覆盖清单中的服务器密码（不推荐）               |
| `-y` | `--yes`       |          | 跳过执行前确认                        |
| `-v` | `--version`   |          | 显示版本信息                         |
| `-h` | `--help`      |          | 显示帮助信息（列出所有可用 Action）          |

---

## 内置任务流程

内置流程由工具自带，无需在 YAML 中定义，直接使用。用户若在 `tasks` 中定义同名流程，则会覆盖内置流程。

### command — 执行远程命令

在目标服务器上执行指定命令。

| 选项   | 长选项         | 参数       | 说明                              |
|------|-------------|----------|---------------------------------|
| `-e` | `--execute` | `string` | 需要执行的命令（也可从清单中的 `command` 字段读取） |

示例：

```bash
# 在单台服务器上执行命令
jssh command web-server-01 -e "ls -la /opt"

# 对服务器组执行命令
jssh command prod-trans -i inventory.yaml

# 使用清单中的 command 字段
jssh command prod-trans
```

### push — 上传文件

将本地文件上传到远程服务器指定目录。

| 选项   | 长选项           | 参数               | 说明                              |
|------|---------------|------------------|---------------------------------|
| `-f` | `--files`     | `file or folder` | 需要上传的本地文件                       |
| `-d` | `--dest-path` | `path`           | 远程目标路径（也可从清单 `service_path` 读取） |
| `-F` | `--force`     |                  | 允许覆盖已存在的远程文件                    |
| `-z` | `--zip`       |                  | 压缩后上传（远程需支持 `unzip`）            |

示例：

```bash
# 上传到目录（自动拼接文件名）
jssh push app-server -f app.jar -d /opt/app/

# 上传到服务器组
jssh push prod-trans -f app.jar
```

上传行为说明：

1. 若 `-d` 以 `/` 结尾，视为目录，最终路径为 `目录 + 本地文件名`
2. 若目标已存在且为目录，则上传到该目录内
3. 若目标已存在且为文件，则备份原文件（追加时间戳）后上传新文件
4. 若父目录不存在，直接报错（不会自动创建目录）

### deploy — 部署应用，示例清单文件配置

上传文件并执行远程命令，等价于 `push` + `command` 的组合。

| 选项   | 长选项           | 参数               | 说明                              |
|------|---------------|------------------|---------------------------------|
| `-f` | `--files`     | `file or folder` | 部署包路径（也可从清单 `package_path` 读取）  |
| `-d` | `--dest-path` | `path`           | 远程部署路径（也可从清单 `service_path` 读取） |
| `-e` | `--execute`   | `string`         | 部署后执行的命令（也可从清单 `command` 读取）    |
| `-y` | `--yes`       |                  | 跳过服务器列表确认                       |

示例：

```bash
jssh deploy -i inventory.yaml prod-trans -f app.jar -y
```

---

## 自定义任务流程

在 `inventory.yaml` 的 `tasks` 段中定义。每个流程包含若干 `steps`，按声明顺序执行。

```yaml
tasks:
  release:
    steps:
      - name: "上传新版本"
        action: push
        with:
          file: "./dist/${app_name}-${version}.jar"
          dest: "${service_path}"

      - name: "备份旧版本"
        action: command
        with:
          command: "cp ${service_path}app.jar ${service_path}app.jar.bak"

      - name: "替换文件"
        action: command
        with:
          command: "mv ${service_path}${app_name}-${version}.jar ${service_path}app.jar"

      - name: "重启服务"
        action: command
        with:
          command: "systemctl restart ${app_name}"
```

字段说明：

| 字段       | 说明                            |
|----------|-------------------------------|
| `name`   | 步骤名，仅用于日志输出                   |
| `action` | 动作类型，对应 `jssh -h` 中列出的 Action |
| `with`   | 动作参数，键值对，值支持 `${var}` 变量替换    |

执行：

```bash
jssh release prod-trans -i inventory.yaml -y
```

同名覆盖内置流程：

```yaml
tasks:
  command:
    steps:
      - name: "先打印环境"
        action: command
        with:
          command: "echo $USER@$HOSTNAME"
      - name: "再执行用户命令"
        action: command
        with:
          command: "${command}"
```

此后 `jssh command ...` 将执行的是用户定义的任务流程。

---

## 变量替换

清单文件中定义的任意变量均可在路径、命令、参数中通过 `${key}` 引用。

### 变量来源与优先级

按优先级从低到高（高优先级覆盖低优先级）：

| 层级  | 来源                          | 说明          |
|-----|-----------------------------|-------------|
| 1   | `global_vars`               | 全局默认        |
| 2   | `servers.<group>.vars`      | 服务组         |
| 3   | `hosts.<host>.extraFields`  | 单台服务器       |
| 4   | `tasks.<name>.steps[].with` | 任务流程步骤定义    |
| 5   | CLI 参数（`-e` / `-f` / `-d`）  | 命令行指定，优先级最高 |

### 内置参数变量

| 参数             | 名称     | 说明                                      |
|----------------|--------|-----------------------------------------|
| `date`         | 日期     | 当前自然日期，格式 `yyyyMMdd`                    |
| `command`      | 默认执行命令 | 未指定 `-e` 参数时，使用清单文件中的 `command` 参数      |
| `package_path` | 本地文件路径 | 未指定 `-f` 参数时，使用清单文件中的 `package_path` 参数 |
| `service_path` | 远程服务路径 | 未指定 `-d` 参数时，使用清单文件中的 `service_path` 参数 |

---

运行 `jssh -h` 即可看到新的 Action 及其参数。

---

### 更新日志
[CHANGELOG](CHANGELOG.md)