// Rasterise an SVG to PNG with sharp:  node render.mjs <in.svg> <out.png> <size> [bg|none] [padRatio]
// sharp is resolved from SHARP_PATH (default "sharp") so the bundled runtime can be reused.
import { createRequire } from "node:module";
import { readFileSync } from "node:fs";

const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_PATH || "sharp");

const [, , inSvg, outPng, sizeArg, bgArg = "none", padArg = "0"] = process.argv;
const size = parseInt(sizeArg, 10) || 512;
const pad = parseFloat(padArg) || 0;
const svg = readFileSync(inSvg);

const inner = Math.max(1, Math.round(size * (1 - pad * 2)));

let img = sharp(svg, { density: 384 }).resize(inner, inner, {
  fit: "contain",
  background: { r: 0, g: 0, b: 0, alpha: 0 },
});

if (bgArg && bgArg !== "none") {
  const hex = bgArg.replace("#", "");
  const r = parseInt(hex.slice(0, 2), 16);
  const g = parseInt(hex.slice(2, 4), 16);
  const b = parseInt(hex.slice(4, 6), 16);
  img = sharp(await img.png().toBuffer())
    .resize(size, size, { fit: "contain", background: { r, g, b, alpha: 1 } })
    .flatten({ background: { r, g, b } });
} else if (pad > 0) {
  img = sharp(await img.png().toBuffer()).resize(size, size, {
    fit: "contain",
    background: { r: 0, g: 0, b: 0, alpha: 0 },
  });
} else {
  img = img.resize(size, size, { fit: "contain", background: { r: 0, g: 0, b: 0, alpha: 0 } });
}

await img.png().toFile(outPng);
console.log(`wrote ${outPng} (${size}px)`);
