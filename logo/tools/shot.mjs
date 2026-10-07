// Screenshot an HTML file at a real viewport (Playwright + installed Chrome).
// node shot.mjs <html> <out.png> <width> <height> [fullPage]
import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PW_PATH || "playwright");

const [, , html, out, w, h, full] = process.argv;
const browser = await chromium.launch({ channel: process.env.PW_CHANNEL || "chrome" });
const page = await browser.newPage({
  viewport: { width: parseInt(w, 10), height: parseInt(h, 10) },
  deviceScaleFactor: 2,
});
await page.goto(pathToFileURL(resolve(html)).href, { waitUntil: "load" });
await page.waitForTimeout(300);
const metrics = await page.evaluate(() => {
  const de = document.documentElement;
  const wide = [];
  document.querySelectorAll("body *").forEach((el) => {
    const r = el.getBoundingClientRect();
    if (r.width > de.clientWidth + 1) wide.push(`${el.tagName}.${el.className || "-"} = ${Math.round(r.width)}px`);
  });
  return { clientWidth: de.clientWidth, scrollWidth: de.scrollWidth, wide: wide.slice(0, 8) };
});
console.log(JSON.stringify(metrics, null, 2));
await page.screenshot({ path: out, fullPage: full === "full" });
await browser.close();
console.log(`wrote ${out}`);
