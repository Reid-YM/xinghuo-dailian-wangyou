# Spark Team · Logo 资产包

依你指定的**双箭头上升**标记做成的完整 Logo 套装：标记、手工字标、横版/竖版组合、单色版本、favicon。

---

## 1. 结构说明

**标记（Mark）**：两条上升箭头，统一 45°。上条厚、下条薄，形成"向上推进"的层次；用两级金色（上亮 `#FFD24A` / 下深 `#D89A00`）做出金属折面的感觉。纯平色，不是渐变。

**字标（Wordmark）**：`SPARK TEAM` 为**手绘几何字形**，不是现成字体——字高 100、笔画 16、圆角接头，字距 22（词距 30）。因为是自己画的，**任何人都无法用字体直接打出来**，具备防伪性。

**组合**：标记高 ≈ 1.5 倍字高，与字标垂直居中对齐。

---

## 2. 文件

```
logo/spark-team/
├── svg/
│   ├── spark-team-icon.svg              标记 · 双色金（深底主用）
│   ├── spark-team-icon-flat.svg         标记 · 单色金
│   ├── spark-team-icon-mono.svg         标记 · 单色墨黑
│   ├── spark-team-wordmark.svg          字标（currentColor）
│   ├── spark-team-horizontal.svg        横向组合 · 深色背景
│   ├── spark-team-horizontal-light.svg  横向组合 · 浅色背景
│   ├── spark-team-stacked.svg           堆叠组合 · 深色背景
│   ├── spark-team-black.svg             单色黑
│   └── spark-team-white.svg             单色白
├── png/                                 1024 / 512 / 256 / 128 / 64 / 32 / 16
│                                        + 横向组合 2048、堆叠组合 1024
└── favicon/
    ├── favicon.svg
    ├── favicon-32.png
    ├── favicon-16.png
    └── apple-touch-icon.png             180×180
```

---

## 3. 颜色

| 名称 | HEX | 用途 |
|---|---|---|
| 亮金 Highlight | `#FFD24A` | 上箭头（受光面） |
| 深金 Deep | `#D89A00` | 下箭头（背光面） |
| 星火金 Base | `#F5B700` | 单色金版本、强调色 |
| 墨黑 Ink | `#0A0C0F` | 单色黑、背景 |

**背景规则**

- 深色底：双色金标记 + 白色字标（`spark-team-horizontal.svg`）
- 浅色底：双色金标记 + 墨黑字标（`spark-team-horizontal-light.svg`）
- 单色场景（印刷 / 刺绣 / 切割 / 极小尺寸）：`spark-team-black.svg`、`spark-team-white.svg`
- 金底：只放墨黑单色版

---

## 4. 最小尺寸

| 形态 | 屏幕 | 印刷 |
|---|---|---|
| 标记 | ≥ 16 px | ≥ 4 mm |
| 横向组合 | 宽 ≥ 120 px | 宽 ≥ 28 mm |
| 堆叠组合 | 宽 ≥ 88 px | 宽 ≥ 20 mm |

组合整体偏宽（约 5.2 : 1），窄栏请改用堆叠版或只用标记。

---

## 5. 使用规则

- **安全区域**：四周留白 ≥ 标记单个箭头的高度。
- **不要改双色关系**：上亮下暗是唯一光源，不要反向或加渐变。
- 不要拉伸、旋转、加阴影/描边/发光、不要换字体重排字标。
- 不要在安全区域内加字或加图。

---

## 6. 已知待精修（诚实说明）

1. **字标是描边构建**：`SPARK TEAM` 用统一 16 单位描边绘制（无 `vector-effect`，在 viewBox 内等比缩放）。交付印刷 / 刺绣 / 切割前，建议在矢量编辑器执行 **轮廓化（Outline Stroke）+ 联集（Unite）**。标记本身已是纯填充路径。
2. **字标有几条斜边是"近似角度"**：`A` 的斜边为 73.8°（理想 75°）、`K` 的斜臂 46.1°（理想 45°）、`M` 的中间尖角 60.5°（理想 60°）。视觉上几乎看不出，`svg_audit` 会提示。要彻底干净，需把字标重建为填充轮廓（可做）。
3. 未做商标近似检索。

---

## 7. 审计结果

`svg_audit.py`（本地参考库对比）：

| 文件 | 分数 |
|---|---|
| `spark-team-icon.svg` | **100 / 100** |
| `spark-team-icon-mono.svg` | **100 / 100** |
| `favicon.svg` | **100 / 100** |
| `spark-team-stacked.svg` | 91 / 100（仅"近似角度"提示） |
| `spark-team-wordmark.svg` / `-horizontal.svg` | 90 / 100（仅"近似角度"提示） |
