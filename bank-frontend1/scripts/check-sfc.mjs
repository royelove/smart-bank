/**
 * Static SFC sanity check.
 *
 * `vite build` cannot run under the agent sandbox (esbuild needs piped child
 * stdio, which is blocked), so this script does the same first-pass work the
 * build would: parse every .vue file and compile its <template> and
 * <script setup> blocks, reporting the exact file:line of any failure.
 *
 * Usage: node scripts/check-sfc.mjs [dir]
 */
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const target = resolve(root, process.argv[2] ?? 'src')

const { parse, compileScript, compileTemplate } = await import('@vue/compiler-sfc')

function walk(dir, out = []) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) walk(full, out)
    else if (entry.endsWith('.vue')) out.push(full)
  }
  return out
}

const files = walk(target)
const problems = []

for (const file of files) {
  const rel = relative(root, file).replace(/\\/g, '/')
  const source = readFileSync(file, 'utf8')

  const { descriptor, errors } = parse(source, { filename: file })
  for (const err of errors) problems.push(`${rel}: parse error: ${err.message}`)

  if (!descriptor.template) {
    problems.push(`${rel}: no <template> block`)
  }

  let bindings
  try {
    const compiled = compileScript(descriptor, { id: rel })
    bindings = compiled.bindings
  } catch (err) {
    problems.push(`${rel}: <script setup> compile error: ${err.message}`)
  }

  if (descriptor.template) {
    const result = compileTemplate({
      source: descriptor.template.content,
      filename: file,
      id: rel,
      compilerOptions: { bindingMetadata: bindings },
    })
    for (const err of result.errors ?? []) {
      const message = typeof err === 'string' ? err : err.message
      problems.push(`${rel}: template error: ${message}`)
    }
  }
}

if (problems.length) {
  console.error(`FAIL - ${problems.length} problem(s) in ${files.length} SFC(s):\n`)
  for (const problem of problems) console.error(`  - ${problem}`)
  process.exit(1)
}

console.log(`OK - ${files.length} SFC(s) parsed and compiled cleanly`)
