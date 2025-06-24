# Spark-Yun 多系统安装脚本

这是一个优化后的 Spark-Yun 安装脚本，支持多个操作系统和架构。

## 🚀 特性

### 多系统支持
- **Linux**: Ubuntu, CentOS, RHEL, Fedora, Arch Linux, openSUSE
- **macOS**: Intel 和 Apple Silicon (M1/M2)
- **Windows**: WSL, Cygwin, MSYS2

### 多架构支持
- **x86_64** (AMD64)
- **ARM64** (AArch64)
- **ARMv7** (32位ARM)

### 智能检测
- 自动检测操作系统和CPU架构
- 自动选择合适的包管理器
- 自动选择下载工具 (curl/wget)
- 智能依赖检查和安装建议

### 增强功能
- **彩色输出**: 清晰的状态显示
- **进度显示**: 实时下载进度
- **错误重试**: 自动重试失败的下载
- **断点续传**: 支持已下载文件的跳过
- **批量处理**: 高效的批量下载
- **详细日志**: 完整的安装过程记录

## 📋 系统要求

### 必需命令
- `tar` - 解压缩工具
- `java` - Java运行环境 (JDK 11+)
- `node` - Node.js运行环境
- `curl` 或 `wget` - 下载工具

### 可选工具
- `pnpm` - 包管理器 (脚本会自动安装)

## 🛠️ 使用方法

### 基本使用
```bash
# 给脚本执行权限
chmod +x install.sh

# 运行安装脚本
./install.sh
```

### 检查系统兼容性
脚本会自动检测系统环境并提供安装建议：

#### Ubuntu/Debian
```bash
sudo apt update && sudo apt install tar openjdk-11-jdk nodejs npm
```

#### CentOS/RHEL/Fedora
```bash
sudo yum install tar java-11-openjdk-devel nodejs npm
# 或者使用 dnf
sudo dnf install tar java-11-openjdk-devel nodejs npm
```

#### macOS
```bash
# 安装 Homebrew (如果未安装)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装依赖
brew install openjdk@11 node
```

## 📁 目录结构

安装完成后，项目目录结构如下：

```
spark-yun/
├── install.sh                    # 安装脚本
├── install-config.json          # 配置文件
├── resources/
│   ├── tmp/                     # 临时下载目录
│   ├── jdbc/system/             # 数据库驱动
│   └── libs/                    # 项目依赖
├── spark-yun-dist/
│   └── spark-min/               # Spark精简版
└── spark-yun-backend/
    └── spark-yun-main/src/main/resources/  # PRQL二进制文件
```

## 🔧 配置文件

`install-config.json` 包含所有下载配置，可以根据需要修改：

- **下载URL**: 修改 `download_config.base_url`
- **Spark版本**: 修改 `download_config.spark`
- **依赖列表**: 修改各个数组中的文件列表
- **系统要求**: 修改 `system_requirements`

## 🐛 故障排除

### 常见问题

1. **权限错误**
   ```bash
   chmod +x install.sh
   ```

2. **下载失败**
   - 检查网络连接
   - 脚本会自动重试3次
   - 可以重新运行脚本继续下载

3. **Java版本问题**
   ```bash
   # 检查Java版本
   java -version
   
   # 应该是Java 11或更高版本
   ```

4. **Node.js版本问题**
   ```bash
   # 检查Node.js版本
   node --version
   
   # 建议使用Node.js 16+
   ```

### 日志分析

脚本提供详细的彩色输出：
- 🔵 **[INFO]**: 信息提示
- 🟢 **[SUCCESS]**: 成功操作
- 🟡 **[WARNING]**: 警告信息
- 🔴 **[ERROR]**: 错误信息

### 手动安装

如果自动安装失败，可以手动下载文件：

1. 从 `https://isxcode.oss-cn-shanghai.aliyuncs.com/zhiqingyun/install/` 下载所需文件
2. 将文件放置到对应目录
3. 重新运行脚本

## 🔄 更新和维护

### 更新依赖版本
1. 修改 `install-config.json` 中的版本号
2. 重新运行安装脚本

### 添加新的数据库驱动
1. 在 `install-config.json` 的 `jdbc_drivers` 数组中添加新文件
2. 重新运行脚本

### 支持新的操作系统
1. 在脚本的 `detect_os()` 函数中添加新的系统检测
2. 在 `detect_package_manager()` 中添加包管理器支持
3. 更新配置文件中的系统要求

## 📞 技术支持

如果遇到问题，请提供以下信息：
- 操作系统和版本
- CPU架构
- 错误日志
- 网络环境

## 📄 许可证

本脚本遵循项目的开源许可证。
