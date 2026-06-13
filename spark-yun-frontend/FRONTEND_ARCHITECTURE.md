# Frontend Architecture

The frontend is split into a base application and removable business modules.

## Directory Roles

- `src/app`: base application runtime. Keep bootstrap, router, store, layout, common components, common utilities, assets, plugins, shared APIs, and app-level pages here.
- `src/app/management`: built-in platform, admin, account, tenant, role, organization, license, and SSO management pages.
- `src/app/routes`: base route groups for platform, admin, and workspace management.
- `src/app/shared`: APIs, hooks, and utilities intentionally shared by multiple modules.
- `src/modules`: removable business modules. Each module owns its `api`, `views`, `routes`, optional `share-routes`, local components, and local config.

## Import Rules

Feature code should prefer module-local imports:

```ts
import { workflowRoutes } from '@/modules/workflow'
import { GetWorkflowList } from '@/modules/workflow/api'
```

Use `src/app/shared` only for code intentionally reused by multiple modules:

```ts
import { GetDataSourceTables } from '@/app/shared/api'
```

Stable resource selectors that are reused by multiple modules should live in `src/app/shared/api/resources.ts`, even when the resource also has a full management module. Keep module APIs focused on module-owned CRUD and detail operations:

```ts
import { GetDatasourceList, GetComputerGroupList } from '@/app/shared/api/resources'
```

Business modules should not import APIs from `src/app/management` directly. If a module needs account, tenant, license, or other base-platform data, expose a small facade from `src/app/shared/api`:

```ts
import { GetUserList } from '@/app/shared/api/user'
import { CheckLicenseStatus } from '@/app/shared/api/license'
```

Use `@/app` for app-level concerns:

```ts
import { LoginUserInfo } from '@/app/api'
```

Base app code must not statically import a concrete business module. `src/app/router/module-routes.ts` discovers optional module routes with Vite glob imports. If `src/modules` is empty or removed, the base app should still compile and keep login, platform, admin, workspace-management, and system pages available.

Common components under `src/app/components` must not import from `src/modules`. If a component needs a workflow, schedule, report, or other business API, keep that component inside the owning module.

Cross-module imports are allowed for now, but keep them intentional. Prefer importing another module's public `api` or exported local component. If three or more modules use the same API, move the stable surface into `src/app/shared/api`.

`src/services` and `src/views` have been removed. Do not recreate them. App-level pages live under `src/app/views`, and feature pages live under `src/modules/*/views`.

## Build And Local Debugging

Use these checks after structure changes:

```bash
pnpm check:architecture
pnpm build
```

For boundary-sensitive refactors, run the full architecture check. It temporarily removes `src/modules`, runs a base build, and restores the directory afterward:

```bash
pnpm check:architecture:full
```

`pnpm check:architecture` blocks cross-module deep imports by default. To print the current cross-module public-surface report explicitly, run:

```bash
pnpm check:architecture:report
```

Cross-module imports should target another module's public surface, such as `@/modules/<module>/api`, `@/modules/<module>/config`, `@/modules/<module>/components`, or `@/modules/<module>`. Avoid importing another module's `views` or private internal paths; move reusable platform UI to `src/app/shared/components` or expose a small public module component when the dependency is intentionally business-specific.

When adding a module, put workspace routes in `src/modules/<module>/routes.ts`. Public share pages should live in `src/modules/<module>/share-routes.ts`. The base router discovers these files automatically.
