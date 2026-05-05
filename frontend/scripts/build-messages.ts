/**
 * AI-generated. Do not edit by hand.
 */

import { type Locale, locales } from "@/shared/i18n";
import { watch } from "chokidar";
import { debounce, isPlainObject, mapValues, pickBy } from "lodash-es";
import { readdir, readFile, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { parse as parseYaml } from "yaml";

const SRC_DIR = resolve(process.cwd(), "messages/src");
const OUT_DIR = resolve(process.cwd(), "messages");
const WATCH_DEBOUNCE_MS = 100;

type LocalizedLeaf = Partial<Record<Locale, string>>;
type SourceTree = { [key: string]: SourceTree | LocalizedLeaf };
type MessageTree = { [key: string]: MessageTree | string };

function isLocaleKey(key: string): key is Locale {
  return (locales as readonly string[]).includes(key);
}

function isLocalizedLeaf(node: unknown): node is LocalizedLeaf {
  if (!isPlainObject(node)) {
    return false;
  }
  const keys = Object.keys(node as object);
  return keys.length > 0 && keys.every(isLocaleKey);
}

async function loadSourceTree(): Promise<SourceTree> {
  const dir = await readdir(SRC_DIR).catch(() => [] as string[]);
  const files = dir.filter((f) => /\.ya?ml$/.test(f)).sort();

  const merged: SourceTree = {};
  for (const file of files) {
    const namespace = file.replace(/\.ya?ml$/, "");
    if (namespace in merged) {
      throw new Error(`Duplicate namespace "${namespace}" (from ${file})`);
    }
    const parsed = parseYaml(await readFile(join(SRC_DIR, file), "utf-8"));
    if (!isPlainObject(parsed)) {
      continue;
    }
    merged[namespace] = parsed as SourceTree | LocalizedLeaf;
  }
  return merged;
}

function extractLocale(source: SourceTree, locale: Locale): MessageTree {
  const mapped = mapValues(source, (value) => {
    if (isLocalizedLeaf(value)) {
      return value[locale];
    }
    if (isPlainObject(value)) {
      return extractLocale(value as SourceTree, locale);
    }
    return undefined;
  });
  return pickBy(mapped, (v) => v !== undefined) as MessageTree;
}

function findMissingTranslations(source: SourceTree, path: string[] = []): string[] {
  return Object.entries(source).flatMap(([key, value]) => {
    const currentPath = [...path, key];
    if (isLocalizedLeaf(value)) {
      return locales
        .filter((l) => typeof value[l] !== "string")
        .map((l) => `${currentPath.join(".")} [${l}]`);
    }
    if (isPlainObject(value)) {
      return findMissingTranslations(value as SourceTree, currentPath);
    }
    return [`${currentPath.join(".")} (invalid value)`];
  });
}

async function build() {
  const source = await loadSourceTree();
  const missing = findMissingTranslations(source);

  if (missing.length > 0) {
    console.error("Missing translations:");
    for (const path of missing) {
      console.error(`  - ${path}`);
    }
    throw new Error(`${missing.length} translation(s) missing`);
  }

  await Promise.all(
    locales.map((locale) => {
      const messages = extractLocale(source, locale);
      return writeFile(join(OUT_DIR, `${locale}.json`), JSON.stringify(messages, null, 2) + "\n");
    }),
  );
  console.log(`✓ messages built (${locales.join(", ")})`);
}

function logError(error: unknown) {
  console.error(error instanceof Error ? error.message : error);
}

async function main() {
  const isWatch = process.argv.includes("--watch");
  try {
    await build();
  } catch (error) {
    logError(error);
    if (!isWatch) {
      process.exit(1);
    }
  }
  if (!isWatch) {
    return;
  }
  console.log(`watching ${SRC_DIR}…`);
  const rebuild = debounce(() => build().catch(logError), WATCH_DEBOUNCE_MS);
  watch(SRC_DIR, { ignoreInitial: true }).on("all", rebuild);
}

main();
