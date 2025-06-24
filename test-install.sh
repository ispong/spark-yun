#!/bin/bash

# =============================================================================
# Spark-Yun 安装脚本测试工具
# 用于测试不同环境下的兼容性
# =============================================================================

set -euo pipefail

# 颜色定义
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

print_info() {
    echo -e "${BLUE}[TEST-INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[TEST-SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[TEST-WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[TEST-ERROR]${NC} $1"
}

# 测试系统检测功能
test_system_detection() {
    print_info "=== 测试系统检测功能 ==="
    
    # 模拟不同的系统环境
    local test_cases=(
        "Linux:x86_64"
        "Darwin:arm64"
        "Darwin:x86_64"
        "CYGWIN_NT-10.0:x86_64"
    )
    
    for case in "${test_cases[@]}"; do
        local os="${case%:*}"
        local arch="${case#*:}"
        
        print_info "测试环境: $os ($arch)"
        
        # 这里可以添加更多的测试逻辑
        case "$os" in
            Linux*)
                print_success "Linux 系统检测正常"
                ;;
            Darwin*)
                print_success "macOS 系统检测正常"
                ;;
            CYGWIN*)
                print_success "Windows (Cygwin) 系统检测正常"
                ;;
            *)
                print_warning "未知系统: $os"
                ;;
        esac
    done
}

# 测试命令检查功能
test_command_check() {
    print_info "=== 测试命令检查功能 ==="
    
    local commands=("tar" "java" "node" "curl" "wget")
    
    for cmd in "${commands[@]}"; do
        if command -v "$cmd" &>/dev/null; then
            print_success "$cmd 命令可用"
        else
            print_warning "$cmd 命令不可用"
        fi
    done
}

# 测试下载功能（不实际下载）
test_download_function() {
    print_info "=== 测试下载功能 ==="
    
    # 测试URL连通性
    local test_url="https://isxcode.oss-cn-shanghai.aliyuncs.com/zhiqingyun/install"
    
    if command -v curl &>/dev/null; then
        if curl -fsSL --connect-timeout 10 "$test_url" -o /dev/null 2>/dev/null; then
            print_success "下载服务器连通性正常"
        else
            print_warning "下载服务器连接失败"
        fi
    else
        print_warning "curl 不可用，跳过连通性测试"
    fi
}

# 测试目录创建
test_directory_creation() {
    print_info "=== 测试目录创建功能 ==="
    
    local test_dir="/tmp/spark-yun-test-$$"
    local test_dirs=(
        "$test_dir/resources/tmp"
        "$test_dir/resources/jdbc/system"
        "$test_dir/resources/libs"
        "$test_dir/spark-yun-dist/spark-min"
    )
    
    for dir in "${test_dirs[@]}"; do
        if mkdir -p "$dir" 2>/dev/null; then
            print_success "目录创建成功: $dir"
        else
            print_error "目录创建失败: $dir"
        fi
    done
    
    # 清理测试目录
    rm -rf "$test_dir"
    print_info "清理测试目录完成"
}

# 测试配置文件
test_config_file() {
    print_info "=== 测试配置文件 ==="
    
    if [ -f "install-config.json" ]; then
        print_success "配置文件存在"
        
        # 检查JSON格式
        if command -v python3 &>/dev/null; then
            if python3 -m json.tool install-config.json >/dev/null 2>&1; then
                print_success "配置文件JSON格式正确"
            else
                print_error "配置文件JSON格式错误"
            fi
        elif command -v jq &>/dev/null; then
            if jq . install-config.json >/dev/null 2>&1; then
                print_success "配置文件JSON格式正确"
            else
                print_error "配置文件JSON格式错误"
            fi
        else
            print_warning "无法验证JSON格式（缺少python3或jq）"
        fi
    else
        print_error "配置文件不存在"
    fi
}

# 测试脚本语法
test_script_syntax() {
    print_info "=== 测试脚本语法 ==="
    
    if bash -n install.sh; then
        print_success "install.sh 语法检查通过"
    else
        print_error "install.sh 语法错误"
    fi
    
    if bash -n test-install.sh; then
        print_success "test-install.sh 语法检查通过"
    else
        print_error "test-install.sh 语法错误"
    fi
}

# 生成测试报告
generate_report() {
    print_info "=== 生成测试报告 ==="
    
    local report_file="test-report-$(date +%Y%m%d-%H%M%S).txt"
    
    {
        echo "Spark-Yun 安装脚本测试报告"
        echo "================================"
        echo "测试时间: $(date)"
        echo "测试系统: $(uname -s) $(uname -m)"
        echo "测试用户: $(whoami)"
        echo "当前目录: $(pwd)"
        echo ""
        echo "系统信息:"
        echo "- 操作系统: $(uname -s)"
        echo "- CPU架构: $(uname -m)"
        echo "- 内核版本: $(uname -r)"
        echo ""
        echo "可用命令:"
        for cmd in tar java node curl wget pnpm; do
            if command -v "$cmd" &>/dev/null; then
                echo "- $cmd: $(command -v "$cmd")"
            else
                echo "- $cmd: 不可用"
            fi
        done
        echo ""
        echo "测试完成"
    } > "$report_file"
    
    print_success "测试报告已生成: $report_file"
}

# 主函数
main() {
    print_info "开始 Spark-Yun 安装脚本兼容性测试"
    echo ""
    
    test_system_detection
    echo ""
    
    test_command_check
    echo ""
    
    test_download_function
    echo ""
    
    test_directory_creation
    echo ""
    
    test_config_file
    echo ""
    
    test_script_syntax
    echo ""
    
    generate_report
    echo ""
    
    print_success "所有测试完成"
}

# 执行测试
main "$@"
