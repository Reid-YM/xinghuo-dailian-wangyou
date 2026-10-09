// Screenshot every element with a data-shot attribute.
// node shot-elements.mjs <html> <outDir> <width> <height> [scale]
import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";
import { resolve, join } from "node:path";
import { mkdirSync } from "node:fs";
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PW_PATH || "playwright");

const [, , html, outDir, w, h, scaleArg] = process.argv;
const scale = parseFloat(scaleArg || "1");
mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch({ channel: process.env.PW_CHANNEL || "chrome" });
const page = await browser.newPage({
  viewport: { width: parseInt(w, 10), height: parseInt(h, 10) },
  deviceScaleFactor: scale,
});
await page.goto(pathToFileURL(resolve(html)).href, { waitUntil: "load" });
await page.waitForTimeout(400);

const names = await page.$$eval("[data-shot]", (els) => els.map((el) => el.getAttribute("data-shot")));
for (const name of names) {
  const el = await page.$(`[data-shot="${name}"]`);
  await el.screenshot({ path: join(outDir, `${name}.png`) });
  const box = await el.boundingBox();
  console.log(`${name}.png  ${Math.round(box.width)}x${Math.round(box.height)}`);
}
await browser.close();
console.log("done");
