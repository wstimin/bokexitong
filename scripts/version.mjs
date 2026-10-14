import { execFileSync } from 'node:child_process'
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const semverPattern = /^\d+\.\d+\.\d+$/

const files = {
  rootPom: resolve(root, 'pom.xml'),
  backendPom: resolve(root, 'backend/pom.xml'),
  packageJson: resolve(root, 'frontend/package.json'),
  packageLock: resolve(root, 'frontend/package-lock.json'),
  updateService: resolve(root, 'backend/src/main/java/com/example/blog/service/UpdateService.java'),
  portalController: resolve(root, 'backend/src/main/java/com/example/blog/controller/PortalController.java'),
  adminLayout: resolve(root, 'frontend/src/views/admin/AdminLayout.vue'),
  portalFooter: resolve(root, 'frontend/src/components/PortalFooter.vue')
}

const read = (path) => readFileSync(path, 'utf8')
const write = (path, value) => writeFileSync(path, value.replace(/\r?\n/g, '\n'))

function pomVersion(content, artifactId) {
  const match = content.match(new RegExp(`<artifactId>${artifactId}<\\/artifactId>\\s*<version>([^<]+)<\\/version>`))
  if (!match) throw new Error(`无法读取 ${artifactId} 的版本号`)
  return match[1]
}

function sourceVersions() {
  const packageJson = JSON.parse(read(files.packageJson))
  const packageLock = JSON.parse(read(files.packageLock))
  const fallback = (path, pattern, label) => {
    const match = read(path).match(pattern)
    if (!match) throw new Error(`无法读取 ${label} 的版本号`)
    return match[1]
  }

  return {
    rootPom: pomVersion(read(files.rootPom), 'personal-blog-system'),
    backendPom: pomVersion(read(files.backendPom), 'blog-backend'),
    packageJson: packageJson.version,
    packageLock: packageLock.version,
    packageLockRoot: packageLock.packages?.['']?.version,
    updateService: fallback(files.updateService, /blog\.version:([\d.]+)/, 'UpdateService'),
    portalController: fallback(files.portalController, /blog\.version:([\d.]+)/, 'PortalController'),
    adminLayout: fallback(files.adminLayout, /const version = ref\('([\d.]+)'\)/, 'AdminLayout'),
    portalFooter: fallback(files.portalFooter, /const version = ref\('([\d.]+)'\)/, 'PortalFooter')
  }
}

function verify() {
  const versions = sourceVersions()
  const unique = [...new Set(Object.values(versions))]
  if (unique.length !== 1 || !semverPattern.test(unique[0])) {
    console.error('版本号不一致：')
    for (const [name, version] of Object.entries(versions)) console.error(`- ${name}: ${version}`)
    process.exit(1)
  }
  console.log(`版本校验通过：v${unique[0]}`)
  return unique[0]
}

function replaceOnce(content, pattern, replacement, label) {
  if (!pattern.test(content)) throw new Error(`无法更新 ${label}`)
  return content.replace(pattern, replacement)
}

function setVersion(next) {
  if (!semverPattern.test(next)) throw new Error(`非法版本号：${next}，必须使用 x.y.z`)

  write(files.rootPom, replaceOnce(
    read(files.rootPom),
    /(<artifactId>personal-blog-system<\/artifactId>\s*<version>)[^<]+(<\/version>)/,
    `$1${next}$2`,
    '根 pom.xml'
  ))
  write(files.backendPom, replaceOnce(
    read(files.backendPom),
    /(<artifactId>blog-backend<\/artifactId>\s*<version>)[^<]+(<\/version>)/,
    `$1${next}$2`,
    '后端 pom.xml'
  ))

  const packageJson = JSON.parse(read(files.packageJson))
  packageJson.version = next
  write(files.packageJson, `${JSON.stringify(packageJson, null, 2)}\n`)

  const packageLock = JSON.parse(read(files.packageLock))
  packageLock.version = next
  if (packageLock.packages?.['']) packageLock.packages[''].version = next
  write(files.packageLock, `${JSON.stringify(packageLock, null, 2)}\n`)

  for (const path of [files.updateService, files.portalController]) {
    write(path, replaceOnce(read(path), /blog\.version:[\d.]+/, `blog.version:${next}`, path))
  }
  for (const path of [files.adminLayout, files.portalFooter]) {
    write(path, replaceOnce(read(path), /const version = ref\('[\d.]+'\)/, `const version = ref('${next}')`, path))
  }

  console.log(`版本已更新：v${next}`)
  verify()
}

function compare(left, right) {
  const a = left.split('.').map(Number)
  const b = right.split('.').map(Number)
  for (let index = 0; index < 3; index += 1) {
    if (a[index] !== b[index]) return a[index] > b[index] ? 1 : -1
  }
  return 0
}

function versionAtRef(ref) {
  const content = execFileSync('git', ['show', `${ref}:pom.xml`], { cwd: root, encoding: 'utf8' })
  return pomVersion(content, 'personal-blog-system')
}

function checkRef(baseRef, headRef = 'HEAD') {
  const base = versionAtRef(baseRef)
  const head = versionAtRef(headRef)
  if (compare(head, base) <= 0) {
    console.error(`版本迭代校验失败：${headRef} 为 v${head}，必须高于 ${baseRef} 的 v${base}`)
    process.exit(1)
  }
  console.log(`版本迭代校验通过：v${base} -> v${head}`)
}

const [command = 'verify', argument, headRef] = process.argv.slice(2)
const current = () => verify()

if (command === 'verify') {
  verify()
} else if (command === 'set') {
  if (!argument) throw new Error('请提供目标版本，例如：node scripts/version.mjs set 1.2.0')
  setVersion(argument)
} else if (['patch', 'minor', 'major'].includes(command)) {
  const parts = current().split('.').map(Number)
  if (command === 'patch') parts[2] += 1
  if (command === 'minor') {
    parts[1] += 1
    parts[2] = 0
  }
  if (command === 'major') {
    parts[0] += 1
    parts[1] = 0
    parts[2] = 0
  }
  setVersion(parts.join('.'))
} else if (command === 'check-ref') {
  if (!argument) throw new Error('请提供对比基准 Git ref')
  checkRef(argument, headRef || 'HEAD')
} else {
  throw new Error(`未知命令：${command}`)
}
