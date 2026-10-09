// Build the delivered PNG / icon set from the master SVGs.
import { createRequire } from "node:module";
import { readFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_PATH || "sharp");
const root = join(dirname(fileURLToPath(import.meta.url)), "..");

const render = async (svgPath, outPath, size) => {
  await sharp(readFileSync(join(root, svgPath)), { density: 384 })
    .resize(size, size, { fit: "contain", background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toFile(join(root, outPath));
  console.log(`${outPath} (${size}px)`);
};

mkdirSync(join(root, "png"), { recursive: true });
mkdirSync(join(root, "favicon"), { recursive: true });

for (const size of [1024, 512, 256, 128, 64, 32, 16]) {
  await render("svg/logo-icon.svg", `png/logo-${size}.png`, size);
}

await render("favicon/favicon.svg", "favicon/favicon-32.png", 32);
await render("favicon/favicon.svg", "favicon/favicon-16.png", 16);
await render("favicon/favicon.svg", "favicon/apple-touch-icon.png", 180);

// Extra: wide lockups rasterised for slide decks / docs.
const wide = async (svgPath, outPath, width) => {
  const svg = readFileSync(join(root, svgPath));
  await sharp(svg, { density: 384 })
    .resize({ width })
    .png()
    .toFile(join(root, outPath));
  console.log(`${outPath} (${width}px wide)`);
};
await wide("svg/logo-horizontal.svg", "png/logo-horizontal-2048.png", 2048);
await wide("svg/logo-primary.svg", "png/logo-primary-2048.png", 2048);
await wide("svg/logo-stacked.svg", "png/logo-stacked-1024.png", 1024);

console.log("done");
