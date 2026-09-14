[![Language](https://img.shields.io/badge/Language-Java-blue.svg)](https://java.dev)
[![Version](https://img.shields.io/badge/release-1.0.0-blue)](https://github.com/windvalley/gossh/releases)
![Supports](https://img.shields.io/badge/Supports-windows,%20Linux-orange)
[![LICENSE](https://img.shields.io/github/license/melvek/JavaSSH)](LICENSE)

# JavaSSH 使用说明

JavaSSH 是一个基于 JSch 的轻量级运维工具，用于批量化上传文件到远程服务器，以及在多台服务器上执行远程命令。

通过 YAML 清单文件定义服务器组、主机、认证信息及业务参数，即可一键完成批量部署、文件推送与命令执行。

---

## 环境要求

- JDK 8 或更高版本
- 远程服务器需支持 SSH / SFTP
- 若使用 `-z` 压缩上传，远程服务器需支持 `unzip` 命令

---

## 安装

```bash
git clone https://github.com/melvek/JavaSSH.git
cd JavaSSH
mvn clean package
```

构建完成后，在 `target/` 目录下会生成可执行 JAR 文件，例如 `javassh-1.0.0.jar`。

为其创建别名，便于使用：

```bash
alias javassh='java -jar /path/to/javassh-1.0.0.jar'
```

---

## 快速开始

### 1. 创建清单文件 `inventory.yaml`

```yaml
global_vars:
  port: 22
  username: deploy
  password: "加密后的密码"
  service_path: /opt/app/
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
        password: "另一个密码"
        remote_path: /data/app/
```

说明：

- `global_vars`：全局默认参数，会被服务器组/主机继承
- `servers`：服务器组定义
    - `vars`：该组的变量，会合并到组内所有主机
    - `hosts`：主机列表，支持两种写法
        - 简写形式：`主机名: IP`，直接使用全局参数
        - 完整形式：`主机名: { host, port, username, password, ... }`
- 除 `host`、`port`、`username`、`password` 外的所有字段都会进入 `extraFields`，可用于变量替换

### 2. 加密密码

明文密码存在安全风险，JavaSSH 使用 Jasypt 加密密码。运行 `EncryptTool` 的 `main` 方法生成密文：

将输出结果以 `(密文)` 的形式填入 YAML。

注意：主密钥 `SEC_KEY` 硬编码在 `EncryptTool` 中，生产环境请改为从环境变量或配置中心读取。

---

## 命令总览

```
jssh <command> [hosts...] [options]
```

### 全局选项

| 选项 | 长选项 | 参数 | 说明 |
|------|--------|------|------|
| `-i` | `--inventory` | `file` | 指定服务器清单文件（默认 `inventory.yaml`）|
| `-l` | `--list` | | 仅显示服务器列表，不执行操作 |
| `-P` | `--port` | `int` | 覆盖清单中的服务器连接端口 |
| `-u` | `--user` | `string` | 覆盖清单中的服务器用户名 |
| `-p` | `--password` | `string` | 覆盖清单中的服务器密码（不推荐）|
| `-v` | `--version` | | 显示版本信息 |
| `-h` | `--help` | | 显示帮助信息 |

---

## 命令 1：command — 执行远程命令

在目标服务器上执行指定命令。

| 选项 | 长选项 | 参数 | 说明 |
|------|--------|------|------|
| `-e` | `--execute` | `string` | 需要执行的命令（也可从清单中的 `command` 字段读取）|
| `-h` | `--help` | | 显示命令帮助 |

示例：

```bash
# 在单台服务器上执行命令
jssh command web-server-01 -e "ls -la /opt"

# 对服务器组执行命令
jssh command prod-trans -i inventory.yaml

# 使用清单中的 command 字段
jssh command prod-trans
```

---

## 命令 2：push — 上传文件

将本地文件上传到远程服务器指定目录，行为模拟 `cp` 命令。

| 选项 | 长选项 | 参数 | 说明 |
|------|--------|------|------|
| `-f` | `--files` | `file or folder` | 需要上传的本地文件 |
| `-d` | `--dest-path` | `path` | 远程目标路径（也可从清单 `service_path` 读取）|
| `-F` | `--force` | | 允许覆盖已存在的远程文件 |
| `-z` | `--zip` | | 压缩后上传（远程需支持 `unzip`）|
| `-h` | `--help` | | 显示命令帮助 |

示例：

```bash
# 上传到目录（自动拼接文件名）
jssh push app-server -f app.jar -d /opt/app/

# 上传到服务器组
jssh push prod-trans -f app.jar
```

上传行为说明（模拟 `cp`）：

1. 若 `-d` 以 `/` 结尾，视为目录，最终路径为 `目录 + 本地文件名`
2. 若目标已存在且为目录，则上传到该目录内
3. 若目标已存在且为文件，则备份原文件（追加时间戳）后上传新文件
4. 若父目录不存在，直接报错（不会自动创建目录）

---

## 命令 3：deploy — 部署应用

上传文件并执行远程命令，适用于发布场景。

| 选项 | 长选项 | 参数 | 说明 |
|------|--------|------|------|
| `-f` | `--files` | `file or folder` | 部署包路径（也可从清单 `package_path` 读取）|
| `-d` | `--dest-path` | `path` | 远程部署路径（也可从清单 `service_path` 读取）|
| `-e` | `--execute` | `string` | 部署后执行的命令（也可从清单 `command` 读取）|
| `-y` | `--yes` | | 跳过服务器列表确认 |
| `-h` | `--help` | | 显示命令帮助 |

示例：

```bash
jssh deploy -i inventory.yaml prod-trans -f app.jar -y
```

---

## 变量替换

清单文件中定义的任意 `extraFields` 均可在路径、命令中通过 `${key}` 引用：

```yaml
global_vars:
  date: ${date}   # date 由工具自动注入（yyyyMMdd）
  app_name: my-app

servers:
  prod:
    vars:
      service_path: /opt/${app_name}/
      command: "systemctl restart ${app_name}"
```

`JavaSSHCommand.replace()` 方法会递归替换所有 `${key}` 占位符。若某个 key 不存在，则保留原样。

---

## 执行摘要示例

```
=== Execution Summary ===
  Total tasks: 3
  Succeeded: 2
  Failed: 1
  ↳ Failed hosts: prod_trans_3
  Completion time: 2024-05-20 15:32:11
```