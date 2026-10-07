// True-size ladder: rows are SVGs, columns are the given pixel sizes.
// node size-ladder.mjs <out.png> <bgHex> <sizes csv> <svg1> <svg2> ...
import { createRequire } from "node:module";
import { readFileSync } from "node:fs";
const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_PATH || "sharp");

const [, , out, bgArg, sizesCsv, ...svgs] = process.argv;
const sizes = sizesCsv.split(",").map((s) => parseInt(s, 10));
const hex = (bgArg || "#ffffff").replace("#", "");
const bg = {
  r: parseInt(hex.slice(0, 2), 16),
  g: parseInt(hex.slice(2, 4), 16),
  b: parseInt(hex.slice(4, 6), 16),
  alpha: 1,
};

const cell = Math.max(...sizes) + 24;
const gap = 12;
const leftPad = 170;
const width = leftPad + sizes.length * (cell + gap) + gap;
const height = svgs.length * (cell + gap) + gap;
const composites = [];

for (let r = 0; r < svgs.length; r += 1) {
  const top = gap + r * (cell + gap);
  const src = readFileSync(svgs[r]);
  for (let c = 0; c < sizes.length; c += 1) {
    const size = sizes[c];
    const png = await sharp(src, { density: 384 })
      .resize(size, size, { fit: "contain", background: { r: 0, g: 0, b: 0, alpha: 0 } })
      .png()
      .toBuffer();
    const left = leftPad + c * (cell + gap) + Math.round((cell - size) / 2);
    composites.push({ input: png, left, top: top + Math.round((cell - size) / 2) });
  }
}

await sharp({ create: { width, height, channels: 4, background: bg } })
  .composite(composites)
  .png()
  .toFile(out);
console.log(`wrote ${out} (${width}x${height})`);
