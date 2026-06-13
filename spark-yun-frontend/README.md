#### 构建指定分支环境

- https://github.com/isxcode/spark-yun/actions/workflows/deploy-dev.yml

#### 远程环境配置

```bash
code .env.local
```

```bash
VITE_VUE_APP_BASE_DOMAIN=http://localhost:8080
#替换
VITE_VUE_APP_BASE_DOMAIN=http://101.132.73.185:8080
```

#### 启动项目

```bash
pnpm run dev
```

访问 http://localhost:5173

#### 前端项目结构开发手册

前端采用“基础应用 + 可移除业务模块 + 版本扩展”的企业级分层结构。

```txt
src/
  app/                         # 基础应用层，不能静态依赖 modules
    api/                       # 登录、认证等基础接口
    assets/                    # 全局样式、图片、字体等静态资源
    components/                # 全局通用组件，只能依赖 app/shared/utils 等基础能力
    hooks/                     # 全局通用 hooks
    layout/                    # 页面布局、顶部栏、菜单、面包屑
    lib/                       # 通用前端库或可复用引擎
    management/                # 平台内置管理功能：租户、用户、角色、组织、授权、SSO 等
    plugins/                   # HTTP 请求插件等基础插件
    router/                    # 基础路由、路由守卫、模块路由自动发现
    routes/                    # 平台、管理员、工作区管理等基础路由分组
    shared/                    # 跨业务模块共享的稳定能力
      api/                     # 共享接口 facade，例如资源查询、用户、授权等
      components/              # 跨业务模块复用的通用组件
      hooks/                   # 跨业务模块复用 hooks
      utils/                   # 跨业务模块复用工具
    store/                     # 全局状态
    utils/                     # 基础工具、http、事件总线、格式化等
    views/                     # 登录、首页、系统错误页等基础页面
  edition/                     # 开源版本默认实现和闭源扩展契约
  modules/                     # 业务模块层，可移除；删除后基础 app 仍需可编译
    access-rule/
    computer-group/
    custom-form/
    custom-func/
    data-planning/
    datasource/
    driver-management/
    file-center/
    global-variables/
    home-overview/
    lib-package/
    message-center/
    metadata-page/
    realtime-computing/
    report/
    schedule/
    spark-container/
    workflow/
```

闭源前端代码位于相邻仓库目录：

```txt
../spark-yun-vip/spark-yun-frontend/
  src/
    edition/                   # VIP 对开源 edition 契约的实现
    modules/                   # VIP 私有业务模块
```

##### 分层职责

- `src/app` 是基础平台层，放应用启动、布局、基础路由、全局 store、通用组件、通用工具、平台管理和 shared 能力。
- `src/modules` 是业务模块层，每个业务模块拥有自己的 `api`、`views`、`routes.ts`、`index.ts`，可按需提供 `components`、`config.ts`、`share-routes.ts`。
- `src/edition` 是版本扩展契约，开源代码只依赖契约，不直接依赖 VIP 私有实现。
- `../spark-yun-vip/spark-yun-frontend` 放闭源实现，VIP 代码通过 `@edition`、`@/app/shared`、模块 public surface 扩展开源项目。

##### 模块标准结构

新增业务模块时，推荐使用以下结构：

```txt
src/modules/<module-name>/
  api/                         # 模块自己的业务接口
  views/                       # 模块页面
  components/                  # 模块公开或内部组件
  config.ts                    # 可被其他模块复用的稳定配置，可选
  routes.ts                    # 工作区路由，必需
  share-routes.ts              # 分享页/公开页路由，可选
  index.ts                     # 模块 public export，必需
```

##### Import 规则

基础层规则：

- `src/app` 不能静态 import `@/modules`。
- `src/app/components` 必须保持通用，不能依赖具体业务模块。
- 基础层需要业务模块路由时，由 `src/app/router/module-routes.ts` 自动发现 `src/modules/*/routes.ts` 和 `src/modules/*/share-routes.ts`。

业务模块规则：

- 模块内部优先使用相对路径或本模块 public surface。
- 跨模块只能导入对方 public surface：

```ts
import { GetWorkflowList } from '@/modules/workflow/api'
import { TypeList } from '@/modules/workflow/config'
import { ZqyFlow } from '@/modules/workflow/components'
import { workflowRoutes } from '@/modules/workflow'
```

- 禁止跨模块 deep import 对方内部页面或私有路径：

```ts
// 禁止
import WorkItem from '@/modules/workflow/views/work-item/index.vue'
```

- 多个模块复用的稳定资源查询放到 `src/app/shared/api/resources.ts`：

```ts
import { GetDatasourceList, GetComputerGroupList } from '@/app/shared/api/resources'
```

- 业务模块不要直接 import `src/app/management` 下的接口。需要用户、授权、租户等平台数据时，先在 `src/app/shared/api` 暴露 facade：

```ts
import { GetUserList } from '@/app/shared/api/user'
import { CheckLicenseStatus } from '@/app/shared/api/license'
```

VIP 规则：

- 开源代码不能直接 import `spark-yun-vip` 中的源码。
- VIP 功能优先通过 `@edition`、`src/app/shared`、模块 public surface 接入。
- 开源构建不依赖 VIP 代码；如果存在 `../spark-yun-vip/spark-yun-frontend/src/edition`，构建会自动通过 `@edition` 接入闭源扩展。

##### 架构检查

结构变更后至少运行：

```bash
pnpm check:architecture
pnpm build
```

涉及模块边界、路由发现、VIP 隔离时运行完整检查：

```bash
pnpm check:architecture:full
pnpm build
```

查看跨模块 public surface 报告：

```bash
pnpm check:architecture:report
```

当前架构检查会阻断以下问题：

- `src/app` 静态依赖 `src/modules`
- `src/modules` 直接依赖 `src/app/management`
- 开源代码直接引用 VIP 源码
- 跨模块 deep import 对方 `views` 或私有路径
- 模块缺少必需的 `index.ts` 或 `routes.ts`
- 删除 `src/modules` 后基础应用无法构建

#### 查看swagger

- http://101.132.73.185:8080/swagger-ui/index.html
- username: admin
- password: admin123

#### 查看数据库

- http://101.132.73.185:8080/h2-console
- jdbc: jdbc:h2:/data/zhiqingyun/GH-2092/h2/data;AUTO_SERVER=TRUE
- username: root
- password: root123
