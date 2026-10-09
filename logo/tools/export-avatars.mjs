// Downscale logo/avatars/png512 -> logo/avatars/png128 for platform upload.
import { createRequire } from "node:module";
import { readdirSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_PATH || "sharp");
const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const src = join(root, "avatars/png512");
const out = join(root, "avatars/png128");
mkdirSync(out, { recursive: true });
for (const f of readdirSync(src).filter((n) => n.endsWith(".png"))) {
  await sharp(join(src, f)).resize(128, 128).png().toFile(join(out, f));
  console.log(`png128/${f}`);
}
console.log("done");
