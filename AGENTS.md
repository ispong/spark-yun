# Agent Notes

## 前端列表规范

后台管理类和资源管理类列表优先参考用户中心：

- 参考文件：`spark-yun-frontend/src/app/management/user-center/views/index.vue`
- 配置参考：`spark-yun-frontend/src/app/management/user-center/views/user-center.config.ts`
- 表格组件：`spark-yun-frontend/src/app/components/block-table/index.vue`

### 基础布局

- 列表页使用 `Breadcrumb` + `.zqy-seach-table` + `.zqy-table-top` + `LoadingPage` + `BlockTable` 的结构。
- 顶部左侧放主新增按钮，右侧放搜索框。
- 搜索框使用 `clearable`、`maxlength="200"`，清空时刷新列表，回车触发查询。
- 分页统一使用 `pageSizes: [10, 20, 50, 100]`。
- 切换 pageSize 时将 `currentPage` 重置为 `1`。

### BlockTable 配置

- 列表默认使用 `BlockTable`，避免直接写裸 `el-table` 或 `vxe-table`。
- 第一业务列默认固定在左侧，操作列固定在右侧。
- 操作列宽度参考用户中心，通常使用 `width: 120`。
- 需要多选时设置 `checkbox: true`，并监听 `@checkbox-change`。
- 如果列表已经可以横向滚动，且不希望用户拖拽改变列宽，设置 `columnResizable: false`。
- 禁用列缩放时仍要保留表头分割线，但不要在第一列、名称固定列边界处产生额外分割线。

### 列展示

- 名称、账号等主识别列使用 `name-click` 样式，并作为主入口跳转详情或打开编辑。
- 状态优先使用 `ZStatusTag`；布尔值或角色类信息使用 `el-tag`。
- 标签列要给足 `minWidth`，长标签例如 `Kubernetes` 不应被裁切。
- 标签文本保持单行展示，必要时补 `white-space: nowrap`。
- 时间、备注、长文本列开启 `showOverflowTooltip: true`。
- 固定宽度和最小宽度要服务于横向滚动，不要为了塞进当前屏幕压缩到遮挡内容。

### 操作列

- 操作列参考用户中心：一个高频主操作放在外面，其余放入“更多”下拉。
- 外部主操作用普通文字按钮样式，不使用额外实心按钮。
- `el-dropdown` 需要设置页面专属 `popper-class`，并对齐用户中心下拉样式：
  - `.el-dropdown-menu { padding: 4px 0; }`
  - `.el-dropdown-menu__item { height: 26px; line-height: 26px; font-size: getCssVar('font-size', 'extra-small'); }`
- 操作项组使用居中布局，间距参考用户中心：`gap: 16px`。
- 下拉中的异步操作要支持 loading/disabled 状态，避免重复点击。

### 多选与批量操作

- 多选列表的批量操作条参考用户中心。
- 选中行后，在 `.zqy-table-top` 内用 `Transition` 展示批量操作遮罩。
- 批量操作条覆盖顶部工具区，使用白底，保留原页面高度不抖动。
- 批量按钮样式参考用户中心：
  - 普通批量动作：白底、主色边框、主色文字，hover/focus 变主色底白字。
  - 取消选择：白底、普通边框、常规文字色，hover/focus 不变成主色按钮。
- 批量操作结束后刷新列表，并清空 `selectedRows`。
- 取消选择时清空 `selectedRows`，必要时通过重新赋值 `tableConfig.tableData = [...tableConfig.tableData]` 触发 `BlockTable` 内部选择状态重置。

### 样式作用域

- 页面级列表样式挂在页面专属类名下，例如 `.zqy-seach-table.user-center-page`、`.zqy-seach-table.computer-group-page`。
- 下拉弹层样式因 popper 挂载位置不同，使用页面专属 popper class 放在页面样式末尾。
- 修改公共 `BlockTable` 时必须保持默认行为不变，新增能力优先做成可选配置。

### 验证

- 修改前端列表后至少运行：

```bash
cd spark-yun-frontend
pnpm build
```

- 构建中的大 chunk 警告是既有 Vite 提示，不等同于本次列表改动失败。
