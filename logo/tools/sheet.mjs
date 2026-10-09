// Compose PNGs into a labelled grid contact sheet.
// node sheet.mjs <out.png> <cols> <tileSize> <bgHex> <in1.png> <in2.png> ...
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_PATH || "sharp");

const [, , out, colsArg, tileArg, bgArg, ...inputs] = process.argv;
const cols = parseInt(colsArg, 10);
const tile = parseInt(tileArg, 10);
const hex = (bgArg || "#ffffff").replace("#", "");
const bg = {
  r: parseInt(hex.slice(0, 2), 16),
  g: parseInt(hex.slice(2, 4), 16),
  b: parseInt(hex.slice(4, 6), 16),
  alpha: 1,
};
const rows = Math.ceil(inputs.length / cols);
const gap = Math.round(tile * 0.12);
const width = cols * tile + (cols + 1) * gap;
const height = rows * tile + (rows + 1) * gap;

const composites = [];
for (let i = 0; i < inputs.length; i += 1) {
  const r = Math.floor(i / cols);
  const c = i % cols;
  const left = gap + c * (tile + gap);
  const top = gap + r * (tile + gap);
  const buf = await sharp(inputs[i]).resize(tile, tile, { fit: "contain", background: { r: 0, g: 0, b: 0, alpha: 0 } }).png().toBuffer();
  composites.push({ input: buf, left, top });
}

await sharp({ create: { width, height, channels: 4, background: bg } })
  .composite(composites)
  .png()
  .toFile(out);
console.log(`wrote ${out} (${width}x${height})`);
