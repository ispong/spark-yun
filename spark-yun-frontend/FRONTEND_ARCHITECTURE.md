# Frontend Architecture

This project keeps open-source code, shared runtime code, and edition-specific extension points separated by directory.

## Directory Roles

- `src/app`: application bootstrap and app-level APIs, such as login and tenant switching.
- `src/modules`: feature modules. Each module should own its `api`, `views`, `routes`, and local config.
- `src/shared`: reusable APIs, hooks, and utilities that are intentionally shared by multiple modules.
- `src/edition`: edition extension points. Open-source builds keep default implementations here; `spark-yun-vip` can override or extend them through aliases or file replacement.
- `src/services`: compatibility facade only. New code should import from `src/app`, `src/modules`, or `src/shared` instead.
- `src/views`: compatibility facade for migrated pages and remaining app-level pages such as login and system fallback pages.

## Import Rules

New feature code should prefer module-local imports:

```ts
import { workflowRoutes } from '@/modules/workflow'
import { GetWorkflowList } from '@/modules/workflow/api'
```

Use `@/shared` only for code that is intentionally reused by multiple modules:

```ts
import { GetDataSourceTables } from '@/shared/api'
```

Use `@/app` for app-level concerns:

```ts
import { LoginUserInfo } from '@/app/api'
```

Avoid adding new implementation code under `src/services` or migrated `src/views/*` directories. Those paths exist to keep older imports and VIP code working during the transition.

## Edition Split

The open-source project owns stable extension contracts in `src/edition`.

`spark-yun-vip` should put closed-source features behind the same contracts instead of importing private code directly from open-source modules. Prefer these extension points:

- `src/edition/features.ts` for feature flags, license checks, and menu visibility.
- `src/edition/routes.ts` for VIP-only routes.
- `@edition` alias for edition-level imports.
- `@shared` alias for shared public frontend utilities.

This keeps local development and production builds predictable: the open-source app can run with default edition stubs, while VIP builds can replace the edition layer without changing feature modules.

## Build And Local Debugging

The compatibility facades mean old imports still compile, so refactoring should not require a flag day for `spark-yun-vip`.

Use these checks after structure changes:

```bash
pnpm type-check
pnpm build
```

When adding a new module, export routes and APIs from the module root, then compose routes through `src/modules/workspace/routes.ts`, `src/modules/platform/routes.ts`, or `src/modules/admin/routes.ts`.
