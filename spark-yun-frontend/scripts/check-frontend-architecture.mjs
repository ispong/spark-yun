#!/usr/bin/env node
import { existsSync, mkdirSync, readdirSync, renameSync, rmSync, statSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const srcRoot = path.join(root, 'src')
const modulesRoot = path.join(srcRoot, 'modules')
const appRoot = path.join(srcRoot, 'app')
const appManagementRoot = path.join(appRoot, 'management')
const vipRoot = path.resolve(root, '../spark-yun-vip/spark-yun-frontend')
const vipSrcRoot = path.join(vipRoot, 'src')
const sourceExtensions = new Set(['.ts', '.tsx', '.js', '.jsx', '.vue'])
const verifyNoModulesBuild = process.argv.includes('--verify-no-modules-build')
const reportCrossModuleInternals = process.argv.includes('--report-cross-module-internals')

const importPattern =
    /\bimport\s+(?:type\s+)?(?:[\s\S]*?\s+from\s+)?['"]([^'"]+)['"]|\bexport\s+(?:type\s+)?(?:[\s\S]*?\s+from\s+)?['"]([^'"]+)['"]|\bimport\s*\(\s*['"]([^'"]+)['"]\s*\)|\brequire\s*\(\s*['"]([^'"]+)['"]\s*\)/g

function toPosix(filePath) {
    return filePath.split(path.sep).join('/')
}

function relative(filePath) {
    return toPosix(path.relative(root, filePath))
}

function walk(dir) {
    if (!existsSync(dir)) {
        return []
    }

    const files = []
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
        if (entry.name === 'node_modules' || entry.name === 'dist') {
            continue
        }
        const fullPath = path.join(dir, entry.name)
        if (entry.isDirectory()) {
            files.push(...walk(fullPath))
            continue
        }
        if (entry.isFile() && sourceExtensions.has(path.extname(entry.name))) {
            files.push(fullPath)
        }
    }
    return files
}

function resolveImport(fromFile, specifier) {
    if (specifier.startsWith('@/')) {
        return path.join(srcRoot, specifier.slice(2))
    }
    if (specifier === '@shared' || specifier.startsWith('@shared/')) {
        return path.join(appRoot, 'shared', specifier.slice('@shared'.length))
    }
    if (specifier === '@edition' || specifier.startsWith('@edition/')) {
        return path.join(srcRoot, 'edition', specifier.slice('@edition'.length))
    }
    if (specifier.startsWith('.')) {
        return path.resolve(path.dirname(fromFile), specifier)
    }
    if (specifier.includes('spark-yun-vip')) {
        return path.resolve(root, specifier)
    }
    return null
}

function isInside(target, container) {
    const relativePath = path.relative(container, target)
    return relativePath === '' || (!relativePath.startsWith('..') && !path.isAbsolute(relativePath))
}

async function collectImports(files) {
    const imports = []
    for (const file of files) {
        const source = await readFile(file, 'utf8')
        for (const match of source.matchAll(importPattern)) {
            const specifier = match[1] || match[2] || match[3] || match[4]
            imports.push({
                file,
                specifier,
                resolved: resolveImport(file, specifier)
            })
        }
    }
    return imports
}

function addViolation(violations, rule, item) {
    violations.push({
        rule,
        file: relative(item.file),
        import: item.specifier
    })
}

function getModuleName(filePath) {
    for (const moduleRoot of [modulesRoot, path.join(vipSrcRoot, 'modules')]) {
        const relativePath = toPosix(path.relative(moduleRoot, filePath))
        if (relativePath === '' || relativePath.startsWith('..') || path.isAbsolute(relativePath)) {
            continue
        }
        return relativePath.split('/')[0] || null
    }
    return null
}

function getImportedModule(specifier) {
    const match = specifier.match(/^@\/modules\/([^/]+)(?:\/(.*))?$/)
    if (!match) {
        return null
    }
    return {
        name: match[1],
        subpath: match[2] || ''
    }
}

function getImportedModuleFromResolvedPath(resolvedPath) {
    if (!resolvedPath) {
        return null
    }

    for (const moduleRoot of [modulesRoot, path.join(vipSrcRoot, 'modules')]) {
        const relativePath = toPosix(path.relative(moduleRoot, resolvedPath))
        if (relativePath === '' || relativePath.startsWith('..') || path.isAbsolute(relativePath)) {
            continue
        }
        const [name, ...rest] = relativePath.split('/')
        return {
            name,
            subpath: rest.join('/')
        }
    }

    return null
}

function isPublicModuleImport(moduleImport) {
    if (!moduleImport) {
        return true
    }
    if (!moduleImport.subpath) {
        return true
    }
    return (
        moduleImport.subpath === 'api' ||
        moduleImport.subpath === 'config' ||
        moduleImport.subpath === 'components' ||
        moduleImport.subpath.startsWith('components/') ||
        moduleImport.subpath === 'index' ||
        moduleImport.subpath === 'routes' ||
        moduleImport.subpath === 'share-routes'
    )
}

function checkModuleStructure(moduleRoot) {
    if (!existsSync(moduleRoot)) {
        return []
    }

    const violations = []
    for (const entry of readdirSync(moduleRoot, { withFileTypes: true })) {
        if (!entry.isDirectory()) {
            continue
        }

        const modulePath = path.join(moduleRoot, entry.name)
        for (const requiredFile of ['index.ts', 'routes.ts']) {
            const target = path.join(modulePath, requiredFile)
            if (!existsSync(target)) {
                violations.push({
                    rule: `module "${entry.name}" must expose ${requiredFile}`,
                    file: relative(modulePath),
                    import: ''
                })
            }
        }
    }
    return violations
}

async function checkStaticBoundaries() {
    const openSourceFiles = walk(srcRoot)
    const imports = await collectImports(openSourceFiles)
    const violations = [
        ...checkModuleStructure(modulesRoot),
        ...checkModuleStructure(path.join(vipSrcRoot, 'modules'))
    ]

    for (const item of imports) {
        const resolved = item.resolved

        if (isInside(item.file, appRoot) && resolved && isInside(resolved, modulesRoot)) {
            addViolation(violations, 'src/app must not import src/modules', item)
        }

        if (isInside(item.file, modulesRoot) && resolved && isInside(resolved, appManagementRoot)) {
            addViolation(violations, 'src/modules must not import src/app/management directly', item)
        }

        if (resolved && isInside(resolved, vipSrcRoot)) {
            addViolation(violations, 'open-source src must not import VIP source directly', item)
        }

        if (item.specifier.includes('spark-yun-vip')) {
            addViolation(violations, 'open-source src must not reference spark-yun-vip directly', item)
        }
    }

    return violations
}

async function collectCrossModuleInternalImports(imports) {
    const findings = []

    for (const item of imports) {
        const sourceModule = getModuleName(item.file)
        const importedModule = getImportedModule(item.specifier) || getImportedModuleFromResolvedPath(item.resolved)
        if (!sourceModule || !importedModule || sourceModule === importedModule.name) {
            continue
        }
        if (isPublicModuleImport(importedModule)) {
            continue
        }
        findings.push({
            rule: 'cross-module imports should use another module public surface',
            file: relative(item.file),
            import: item.specifier
        })
    }

    return findings
}

async function checkModuleBoundaries() {
    const files = [...walk(modulesRoot), ...walk(path.join(vipSrcRoot, 'modules'))]
    const imports = await collectImports(files)

    return collectCrossModuleInternalImports(imports)
}

function checkNoModulesBuild() {
    if (!existsSync(modulesRoot)) {
        return { ok: true }
    }

    const tempRoot = path.join(root, 'node_modules', '.cache', 'architecture-check')
    const tempModules = path.join(tempRoot, `modules-${Date.now()}`)
    mkdirSync(tempRoot, { recursive: true })

    try {
        renameSync(modulesRoot, tempModules)
        const result = spawnSync('pnpm', ['build'], {
            cwd: root,
            stdio: 'inherit',
            env: process.env
        })
        return { ok: result.status === 0, status: result.status }
    } finally {
        if (!existsSync(modulesRoot) && existsSync(tempModules) && statSync(tempModules).isDirectory()) {
            renameSync(tempModules, modulesRoot)
        }
        rmSync(tempRoot, { recursive: true, force: true })
    }
}

function printViolations(violations) {
    for (const violation of violations) {
        console.error(`- ${violation.rule}`)
        console.error(`  file: ${violation.file}`)
        console.error(`  import: ${violation.import}`)
    }
}

const violations = [...(await checkStaticBoundaries()), ...(await checkModuleBoundaries())]

if (violations.length > 0) {
    console.error('\nFrontend architecture check failed:\n')
    printViolations(violations)
    process.exit(1)
}

if (verifyNoModulesBuild) {
    const result = checkNoModulesBuild()
    if (!result.ok) {
        console.error(`\nFrontend architecture check failed: build without src/modules exited with ${result.status}.`)
        process.exit(result.status || 1)
    }
}

if (reportCrossModuleInternals) {
    const findings = await checkModuleBoundaries()
    if (findings.length > 0) {
        console.warn('\nCross-module internal import report:\n')
        printViolations(findings)
        console.warn(`\n${findings.length} cross-module internal imports found.`)
    } else {
        console.log('No cross-module internal imports found.')
    }
}

console.log(
    verifyNoModulesBuild
        ? 'Frontend architecture check passed, including build without src/modules.'
        : 'Frontend architecture check passed.'
)
