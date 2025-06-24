#!/bin/bash

# =============================================================================
# Spark-Yun 多系统安装脚本
# 支持 Linux、macOS、Windows (WSL/Cygwin)
# 支持 x86_64、arm64、aarch64 架构
# =============================================================================

set -euo pipefail  # 严格模式：遇到错误立即退出

# 颜色定义
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# 全局变量
DETECTED_OS=""
DETECTED_ARCH=""
DOWNLOAD_TOOL=""
PACKAGE_MANAGER=""

# =============================================================================
# 工具函数
# =============================================================================

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检测操作系统
detect_os() {
    case "$(uname -s)" in
        Linux*)
            DETECTED_OS="linux"
            ;;
        Darwin*)
            DETECTED_OS="macos"
            ;;
        CYGWIN*|MINGW*|MSYS*)
            DETECTED_OS="windows"
            ;;
        *)
            print_error "不支持的操作系统: $(uname -s)"
            exit 1
            ;;
    esac
    print_info "检测到操作系统: $DETECTED_OS"
}

# 检测CPU架构
detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64)
            DETECTED_ARCH="x86_64"
            ;;
        arm64|aarch64)
            DETECTED_ARCH="arm64"
            ;;
        armv7l)
            DETECTED_ARCH="armv7"
            ;;
        *)
            print_error "不支持的CPU架构: $(uname -m)"
            exit 1
            ;;
    esac
    print_info "检测到CPU架构: $DETECTED_ARCH"
}

# 检测下载工具
detect_download_tool() {
    if command -v curl &>/dev/null; then
        DOWNLOAD_TOOL="curl"
    elif command -v wget &>/dev/null; then
        DOWNLOAD_TOOL="wget"
    else
        print_error "未找到下载工具 (curl 或 wget)"
        print_info "请安装 curl 或 wget 后重试"
        exit 1
    fi
    print_info "使用下载工具: $DOWNLOAD_TOOL"
}

# 检测包管理器
detect_package_manager() {
    case "$DETECTED_OS" in
        linux)
            if command -v apt &>/dev/null; then
                PACKAGE_MANAGER="apt"
            elif command -v yum &>/dev/null; then
                PACKAGE_MANAGER="yum"
            elif command -v dnf &>/dev/null; then
                PACKAGE_MANAGER="dnf"
            elif command -v pacman &>/dev/null; then
                PACKAGE_MANAGER="pacman"
            elif command -v zypper &>/dev/null; then
                PACKAGE_MANAGER="zypper"
            else
                print_warning "未检测到已知的包管理器"
                PACKAGE_MANAGER="unknown"
            fi
            ;;
        macos)
            if command -v brew &>/dev/null; then
                PACKAGE_MANAGER="brew"
            else
                print_warning "未检测到 Homebrew，建议安装以便管理依赖"
                PACKAGE_MANAGER="unknown"
            fi
            ;;
        windows)
            if command -v choco &>/dev/null; then
                PACKAGE_MANAGER="choco"
            elif command -v scoop &>/dev/null; then
                PACKAGE_MANAGER="scoop"
            else
                print_warning "未检测到 Chocolatey 或 Scoop"
                PACKAGE_MANAGER="unknown"
            fi
            ;;
    esac
    print_info "检测到包管理器: $PACKAGE_MANAGER"
}

# 通用下载函数
download_file() {
    local url="$1"
    local output_path="$2"
    local description="${3:-文件}"
    local max_retries=3
    local retry_count=0

    print_info "开始下载 $description"

    while [ $retry_count -lt $max_retries ]; do
        case "$DOWNLOAD_TOOL" in
            curl)
                if curl -fsSL --progress-bar "$url" -o "$output_path"; then
                    print_success "$description 下载成功"
                    return 0
                fi
                ;;
            wget)
                if wget --progress=bar:force:noscroll -O "$output_path" "$url"; then
                    print_success "$description 下载成功"
                    return 0
                fi
                ;;
        esac

        retry_count=$((retry_count + 1))
        if [ $retry_count -lt $max_retries ]; then
            print_warning "$description 下载失败，正在重试 ($retry_count/$max_retries)"
            sleep 2
        fi
    done

    print_error "$description 下载失败，已重试 $max_retries 次"
    return 1
}

# 检查必需的命令
check_required_commands() {
    local missing_commands=()

    # 检查tar命令
    if ! command -v tar &>/dev/null; then
        missing_commands+=("tar")
    fi

    # 检查java命令
    if ! command -v java &>/dev/null; then
        missing_commands+=("java")
    fi

    # 检查node命令
    if ! command -v node &>/dev/null; then
        missing_commands+=("node")
    fi

    if [ ${#missing_commands[@]} -gt 0 ]; then
        print_error "缺少必需的命令: ${missing_commands[*]}"
        print_info "请安装缺少的命令后重试"

        # 提供安装建议
        case "$DETECTED_OS" in
            linux)
                case "$PACKAGE_MANAGER" in
                    apt)
                        print_info "Ubuntu/Debian 安装命令:"
                        for cmd in "${missing_commands[@]}"; do
                            case "$cmd" in
                                tar) echo "  sudo apt update && sudo apt install tar" ;;
                                java) echo "  sudo apt update && sudo apt install openjdk-11-jdk" ;;
                                node) echo "  sudo apt update && sudo apt install nodejs npm" ;;
                            esac
                        done
                        ;;
                    yum|dnf)
                        print_info "CentOS/RHEL/Fedora 安装命令:"
                        for cmd in "${missing_commands[@]}"; do
                            case "$cmd" in
                                tar) echo "  sudo $PACKAGE_MANAGER install tar" ;;
                                java) echo "  sudo $PACKAGE_MANAGER install java-11-openjdk-devel" ;;
                                node) echo "  sudo $PACKAGE_MANAGER install nodejs npm" ;;
                            esac
                        done
                        ;;
                esac
                ;;
            macos)
                print_info "macOS 安装命令:"
                for cmd in "${missing_commands[@]}"; do
                    case "$cmd" in
                        tar) echo "  tar 通常已预装在 macOS 中" ;;
                        java) echo "  brew install openjdk@11" ;;
                        node) echo "  brew install node" ;;
                    esac
                done
                ;;
        esac
        exit 1
    fi

    print_success "所有必需命令检查通过"
}

# 安装或检查pnpm
install_pnpm() {
    if command -v pnpm &>/dev/null; then
        print_success "pnpm 已安装"
        return 0
    fi

    print_info "未检测到 pnpm，正在安装..."

    # 尝试使用npm安装pnpm
    if command -v npm &>/dev/null; then
        if npm install pnpm@9.0.6 -g; then
            print_success "pnpm 安装成功"
            return 0
        else
            print_warning "使用 npm 安装 pnpm 失败"
        fi
    fi

    # 尝试使用包管理器安装
    case "$PACKAGE_MANAGER" in
        brew)
            if brew install pnpm; then
                print_success "pnpm 通过 Homebrew 安装成功"
                return 0
            fi
            ;;
        apt)
            print_info "尝试通过 apt 安装 pnpm..."
            if curl -fsSL https://get.pnpm.io/install.sh | sh -; then
                print_success "pnpm 安装成功"
                return 0
            fi
            ;;
    esac

    print_error "pnpm 安装失败，请手动安装"
    exit 1
}

# =============================================================================
# 主要安装逻辑
# =============================================================================

# 初始化环境
init_environment() {
    print_info "=== Spark-Yun 安装脚本开始 ==="

    # 检测系统环境
    detect_os
    detect_arch
    detect_download_tool
    detect_package_manager

    # 检查必需命令
    check_required_commands

    # 安装pnpm
    install_pnpm

    print_success "环境检测完成"
}

# 设置路径和变量
setup_paths() {
    # 进入项目目录
    readonly BASE_PATH=$(cd "$(dirname "$0")" || exit ; pwd)
    cd "${BASE_PATH}" || exit

    # 定义路径
    readonly TMP_DIR="${BASE_PATH}/resources/tmp"
    readonly SPARK_MIN_DIR="${BASE_PATH}/spark-yun-dist/spark-min"
    readonly JDBC_DIR="${BASE_PATH}/resources/jdbc/system"
    readonly LIBS_DIR="${BASE_PATH}/resources/libs"

    # 定义下载配置
    readonly OSS_DOWNLOAD_URL="https://isxcode.oss-cn-shanghai.aliyuncs.com/zhiqingyun/install"
    readonly SPARK_MIN_FILE="spark-3.4.1-bin-hadoop3.tgz"
    readonly SPARK_MIN_DOWNLOAD_URL="${OSS_DOWNLOAD_URL}/${SPARK_MIN_FILE}"

    print_info "项目路径: $BASE_PATH"
}

# 创建必要的目录
create_directories() {
    local dirs=("$TMP_DIR" "$SPARK_MIN_DIR" "$JDBC_DIR" "$LIBS_DIR")

    for dir in "${dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            print_info "创建目录: $dir"
        fi
    done
}

# 下载并解压Spark
install_spark() {
    print_info "=== 开始安装 Spark ==="

    # 下载spark二进制文件
    if [ ! -f "${TMP_DIR}/${SPARK_MIN_FILE}" ]; then
        print_info "Spark 3.4.1 二进制文件开始下载，请耐心等待..."
        if ! download_file "$SPARK_MIN_DOWNLOAD_URL" "${TMP_DIR}/${SPARK_MIN_FILE}" "Spark 3.4.1"; then
            print_error "Spark 下载失败"
            exit 1
        fi
    else
        print_success "Spark 文件已存在，跳过下载"
    fi

    # 解压spark程序，并删除不需要的文件
    if [ ! -f "${SPARK_MIN_DIR}/README.md" ]; then
        print_info "正在解压 Spark..."

        # 解压文件
        if tar vzxf "${TMP_DIR}/${SPARK_MIN_FILE}" --strip-components=1 -C "${SPARK_MIN_DIR}"; then
            print_success "Spark 解压成功"
        else
            print_error "Spark 解压失败"
            exit 1
        fi

        # 删除不需要的文件和目录
        print_info "清理不需要的文件..."
        local cleanup_items=(
            "${SPARK_MIN_DIR}/data"
            "${SPARK_MIN_DIR}/examples"
            "${SPARK_MIN_DIR}/licenses"
            "${SPARK_MIN_DIR}/R"
            "${SPARK_MIN_DIR}/LICENSE"
            "${SPARK_MIN_DIR}/NOTICE"
            "${SPARK_MIN_DIR}/RELEASE"
        )

        for item in "${cleanup_items[@]}"; do
            if [ -e "$item" ]; then
                rm -rf "$item"
                print_info "删除: $(basename "$item")"
            fi
        done

        print_success "Spark 安装完成"
    else
        print_success "Spark 已安装，跳过解压"
    fi
}

# 批量下载文件的通用函数
download_files_batch() {
    local target_dir="$1"
    local description="$2"
    shift 2
    local files=("$@")

    print_info "=== 开始下载 $description ==="

    local total=${#files[@]}
    local current=0
    local failed_files=()

    for file in "${files[@]}"; do
        current=$((current + 1))
        local file_path="${target_dir}/${file}"

        if [ -f "$file_path" ]; then
            print_success "[$current/$total] $file 已存在，跳过下载"
            continue
        fi

        print_info "[$current/$total] 正在下载 $file..."
        if download_file "${OSS_DOWNLOAD_URL}/${file}" "$file_path" "$file"; then
            print_success "[$current/$total] $file 下载完成"
        else
            print_error "[$current/$total] $file 下载失败"
            failed_files+=("$file")
        fi
    done

    if [ ${#failed_files[@]} -eq 0 ]; then
        print_success "$description 全部下载完成"
        return 0
    else
        print_error "$description 中有 ${#failed_files[@]} 个文件下载失败:"
        for file in "${failed_files[@]}"; do
            print_error "  - $file"
        done
        return 1
    fi
}

# 下载Spark JAR依赖
install_spark_jars() {
    local spark_jar_dir="${SPARK_MIN_DIR}/jars"

    # Spark JAR依赖列表
    local spark_jars=(
        "spark-sql-kafka-0-10_2.12-3.4.1.jar"
        "spark-streaming-kafka-0-10_2.12-3.4.1.jar"
        "spark-token-provider-kafka-0-10_2.12-3.4.1.jar"
        "commons-pool2-2.11.1.jar"
        "kafka-clients-3.1.2.jar"
        "bcpkix-jdk18on-1.78.1.jar"
        "bcprov-jdk18on-1.78.1.jar"
        "commons-dbutils-1.7.jar"
        "HikariCP-4.0.3.jar"
    )

    download_files_batch "$spark_jar_dir" "Spark JAR 依赖" "${spark_jars[@]}"
}

# 下载数据库驱动
install_jdbc_drivers() {
    # 数据库驱动列表
    local jdbc_drivers=(
        "mysql-connector-j-8.1.0.jar"
        "postgresql-42.6.0.jar"
        "Dm8JdbcDriver18-8.1.1.49.jar"
        "clickhouse-jdbc-0.5.0.jar"
        "ngdbc-2.18.13.jar"
        "mysql-connector-java-5.1.49.jar"
        "mssql-jdbc-12.4.2.jre8.jar"
        "hive-jdbc-3.1.3-standalone.jar"
        "hive-jdbc-uber-2.6.3.0-235.jar"
        "ojdbc8-19.23.0.0.jar"
        "oceanbase-client-2.4.6.jar"
        "jcc-11.5.8.0.jar"
        "gbase-connector-java-9.5.0.7-build1-bin.jar"
        "jconn4-16.0.jar"
        "h2-2.2.224.jar"
    )

    download_files_batch "$JDBC_DIR" "数据库驱动" "${jdbc_drivers[@]}"
}

# 下载项目依赖
install_project_libs() {
    # 项目依赖列表
    local project_libs=(
        "prql-java-0.5.2.jar"
    )

    download_files_batch "$LIBS_DIR" "项目依赖" "${project_libs[@]}"
}

# 根据系统和架构下载PRQL二进制文件
install_prql_binaries() {
    print_info "=== 开始下载 PRQL 二进制文件 ==="

    local prql_resource_dir="${BASE_PATH}/spark-yun-backend/spark-yun-main/src/main/resources"
    local prql_files=()

    # 根据系统和架构确定需要下载的文件
    case "$DETECTED_OS" in
        macos)
            case "$DETECTED_ARCH" in
                arm64)
                    prql_files+=("libprql_java-osx-arm64.dylib")
                    ;;
                x86_64)
                    prql_files+=("libprql_java-osx-x64.dylib")
                    ;;
            esac
            ;;
        linux)
            case "$DETECTED_ARCH" in
                x86_64)
                    prql_files+=("libprql_java-linux64.so")
                    ;;
                arm64)
                    prql_files+=("libprql_java-linux-arm64.so")
                    ;;
            esac
            ;;
        windows)
            case "$DETECTED_ARCH" in
                x86_64)
                    prql_files+=("prql_java-win64.dll")
                    ;;
            esac
            ;;
    esac

    # 总是下载常用的二进制文件以支持跨平台部署
    local common_prql_files=(
        "libprql_java-osx-arm64.dylib"
        "libprql_java-linux64.so"
    )

    # 合并文件列表并去重
    local all_prql_files=()

    # 添加特定平台的文件
    for file in "${prql_files[@]}"; do
        if [[ ! " ${all_prql_files[*]:-} " =~ " ${file} " ]]; then
            all_prql_files+=("$file")
        fi
    done

    # 添加通用文件
    for file in "${common_prql_files[@]}"; do
        if [[ ! " ${all_prql_files[*]:-} " =~ " ${file} " ]]; then
            all_prql_files+=("$file")
        fi
    done

    # 创建资源目录
    if [ ! -d "$prql_resource_dir" ]; then
        mkdir -p "$prql_resource_dir"
        print_info "创建目录: $prql_resource_dir"
    fi

    # 下载PRQL二进制文件
    local failed_files=()
    for file in "${all_prql_files[@]}"; do
        local file_path="${prql_resource_dir}/${file}"

        if [ -f "$file_path" ]; then
            print_success "$file 已存在，跳过下载"
            continue
        fi

        print_info "正在下载 $file..."
        if download_file "${OSS_DOWNLOAD_URL}/${file}" "$file_path" "$file"; then
            print_success "$file 下载完成"
        else
            print_warning "$file 下载失败，可能不影响当前平台使用"
            failed_files+=("$file")
        fi
    done

    if [ ${#failed_files[@]} -eq 0 ]; then
        print_success "PRQL 二进制文件下载完成"
    else
        print_warning "部分 PRQL 二进制文件下载失败，但不影响当前平台使用"
    fi
}

# =============================================================================
# 主函数
# =============================================================================

main() {
    # 初始化环境
    init_environment

    # 设置路径
    setup_paths

    # 创建目录
    create_directories

    # 安装各个组件
    install_spark
    install_spark_jars
    install_jdbc_drivers
    install_project_libs
    install_prql_binaries

    # 完成安装
    print_success "=== 项目依赖安装成功 ==="
    print_info "系统信息: $DETECTED_OS ($DETECTED_ARCH)"
    print_info "项目路径: $BASE_PATH"
    print_info "安装完成时间: $(date)"
}

# 执行主函数
main "$@"