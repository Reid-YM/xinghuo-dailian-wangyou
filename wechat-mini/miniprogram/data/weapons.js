/**
 * 武器 / 迷彩数据快照（离线兜底用）—— **这个文件是自动生成的，不要手改**。
 *
 * 来源：https://wydlgf.cn/api/tool-station
 * 生成：node tools/sync-data.js
 * camoTracks 版本：v25r1 · camoGuide 版本：v25r4 · storageVersion：v23r6
 *
 * 运行时优先用线上接口（改了网站就立刻生效）；接口不可用时才用这份快照。
 * 官方公布新武器 / 新迷彩后，重跑同步脚本并提交新版本即可。
 */
module.exports = {
  "schemaVersion": 3,
  "defaultGameCode": "CODMW4",
  "storageVersion": "v23r6",
  "games": [
    {
      "code": "CODMW4",
      "shortName": "MW4",
      "name": "Call of Duty: Modern Warfare 4",
      "year": 2026,
      "artworkUrl": "/assets/tool/artwork/codmw4.jpg",
      "officialSource": "https://www.callofduty.com/",
      "dataStatus": "MW4 武器与迷彩进度",
      "accent": "#f4c934",
      "playerLevelEnabled": true,
      "modes": [
        {
          "code": "MULTIPLAYER",
          "name": "多人",
          "masteryCamos": 3,
          "trackLabel": "多人迷彩路线"
        },
        {
          "code": "DMZ",
          "name": "DMZ",
          "masteryCamos": 3,
          "trackLabel": "DMZ 迷彩路线"
        },
        {
          "code": "WARZONE",
          "name": "战区",
          "masteryCamos": 3,
          "trackLabel": "战区迷彩路线"
        }
      ],
      "weapons": [
        {
          "code": "HAN86",
          "name": "Han 86",
          "nameZh": "汉86",
          "image": "/assets/mw4weapon/han86.webp",
          "weaponType": "突击步枪",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 68,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "HAN 86 X20 SKYBREAKER"
          ],
          "note": ""
        },
        {
          "code": "M4",
          "name": "M4",
          "nameZh": "M4",
          "image": "/assets/mw4weapon/m4.webp",
          "weaponType": "突击步枪",
          "unlockLabel": "玩家等级 9",
          "unlockPlayerRank": 9,
          "maxLevel": 68,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "M4 HURRICANE"
          ],
          "note": ""
        },
        {
          "code": "HYEONBURST",
          "name": "Hyeon Burst",
          "nameZh": "玄铁（点射型）",
          "image": "/assets/mw4weapon/hyeonburst.webp",
          "weaponType": "突击步枪",
          "unlockLabel": "玩家等级 18",
          "unlockPlayerRank": 18,
          "maxLevel": 64,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "HYEON AUTO",
            "HYEON HYPERBURST"
          ],
          "note": ""
        },
        {
          "code": "KASTOV762",
          "name": "Kastov 762",
          "nameZh": "卡斯托夫762",
          "image": "/assets/mw4weapon/kastov762.webp",
          "weaponType": "突击步枪",
          "unlockLabel": "玩家等级 21",
          "unlockPlayerRank": 21,
          "maxLevel": 71,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "KASTOV 545",
            "KASTOV ARC"
          ],
          "note": ""
        },
        {
          "code": "PATRIOTXMR",
          "name": "Patriot XMR",
          "nameZh": "爱国者XMR",
          "image": "/assets/mw4weapon/patriotxmr.webp",
          "weaponType": "突击步枪",
          "unlockLabel": "玩家等级 25",
          "unlockPlayerRank": 25,
          "maxLevel": 64,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "PATRIOT XMR VOLT"
          ],
          "note": "该武器归类为战斗步枪。"
        },
        {
          "code": "AXION",
          "name": "Axion",
          "nameZh": "阿克西恩",
          "weaponType": "突击步枪",
          "unlockLabel": "默认配置限定",
          "unlockPlayerRank": null,
          "maxLevel": 67,
          "levelingEnabled": false,
          "gunsmith": false,
          "apexAttachments": [
            "AXION CRUSADER"
          ],
          "note": "默认配置不支持武器改装与 Apex 配件。",
          "image": "/assets/mw4weapon/axion.webp"
        },
        {
          "code": "ISONIGHTSHADE",
          "name": "ISO Nightshade",
          "nameZh": "ISO夜影",
          "image": "/assets/mw4weapon/isonightshade.webp",
          "weaponType": "冲锋枪",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 56,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "ISO TRACE"
          ],
          "note": ""
        },
        {
          "code": "PPSH41",
          "name": "PPSH-41",
          "nameZh": "PPSH-41",
          "image": "/assets/mw4weapon/ppsh41.webp",
          "weaponType": "冲锋枪",
          "unlockLabel": "玩家等级 11",
          "unlockPlayerRank": 11,
          "maxLevel": 54,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "PPSH-41 EMBER"
          ],
          "note": ""
        },
        {
          "code": "X58NYX",
          "name": "X-58 NYX",
          "nameZh": "X-58夜神",
          "image": "/assets/mw4weapon/x58nyx.webp",
          "weaponType": "冲锋枪",
          "unlockLabel": "玩家等级 27",
          "unlockPlayerRank": 27,
          "maxLevel": 47,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "X-58 VOID"
          ],
          "note": ""
        },
        {
          "code": "WZ55STRIGA",
          "name": "Wz. 55 Striga",
          "nameZh": "WZ55斯崔加",
          "weaponType": "冲锋枪",
          "unlockLabel": "战役任务限定",
          "unlockPlayerRank": null,
          "maxLevel": 52,
          "levelingEnabled": false,
          "gunsmith": false,
          "apexAttachments": [
            "[CLASSIFIED]"
          ],
          "note": "战役任务获取，不支持武器改装与 Apex 配件。",
          "image": "/assets/mw4weapon/wz55striga.webp"
        },
        {
          "code": "REZI12",
          "name": "REZI 12",
          "nameZh": "雷兹12",
          "image": "/assets/mw4weapon/rezi12.webp",
          "weaponType": "霰弹枪",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 60,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "REZI 12 ENFORCER"
          ],
          "note": ""
        },
        {
          "code": "FINNLMG",
          "name": "Finn LMG",
          "nameZh": "菲恩轻型机枪",
          "image": "/assets/mw4weapon/finnlmg.webp",
          "weaponType": "轻机枪",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 52,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "FINN ADVERSE"
          ],
          "note": ""
        },
        {
          "code": "TYPE73",
          "name": "Type 73",
          "nameZh": "73式",
          "image": "/assets/mw4weapon/type73.webp",
          "weaponType": "轻机枪",
          "unlockLabel": "玩家等级 5",
          "unlockPlayerRank": 5,
          "maxLevel": 57,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "TYPE 73 ASSAULT"
          ],
          "note": ""
        },
        {
          "code": "MAR9",
          "name": "Mar-9",
          "nameZh": "MAR-9",
          "image": "/assets/mw4weapon/mar9.webp",
          "weaponType": "射手步枪",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 67,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "MAR-9 AUTO"
          ],
          "note": ""
        },
        {
          "code": "ORIS86",
          "name": "Oris 8.6",
          "nameZh": "奥里斯 8.6",
          "image": "/assets/mw4weapon/oris86.webp",
          "weaponType": "射手步枪",
          "unlockLabel": "玩家等级 14",
          "unlockPlayerRank": 14,
          "maxLevel": 68,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "ORIS 8.6 RAZOR"
          ],
          "note": ""
        },
        {
          "code": "KG7VULCAN",
          "name": "KG-7 Vulcan",
          "nameZh": "KG-7火神",
          "image": "/assets/mw4weapon/kg7vulcan.webp",
          "weaponType": "狙击步枪",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 72,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "KG-9 DRILLCHARGE"
          ],
          "note": ""
        },
        {
          "code": "SIGNAL50",
          "name": "Signal 50",
          "nameZh": "西格诺50",
          "image": "/assets/mw4weapon/signal50.webp",
          "weaponType": "狙击步枪",
          "unlockLabel": "玩家等级 30",
          "unlockPlayerRank": 30,
          "maxLevel": 73,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "SIGNAL 50 INFERNO"
          ],
          "note": ""
        },
        {
          "code": "KARITP68",
          "name": "Krait P68",
          "nameZh": "环蛇P68",
          "image": "/assets/mw4weapon/kraitp68.webp",
          "weaponType": "手枪",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 34,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "KARIT P68 COMMANDO"
          ],
          "note": ""
        },
        {
          "code": "50GS",
          "name": ".50 GS",
          "nameZh": "点50 GS",
          "image": "/assets/mw4weapon/50gs.webp",
          "weaponType": "手枪",
          "unlockLabel": "玩家等级 23",
          "unlockPlayerRank": 23,
          "maxLevel": 50,
          "levelingEnabled": true,
          "gunsmith": true,
          "apexAttachments": [
            "GS MAGNA"
          ],
          "note": ""
        },
        {
          "code": "SANG9MM",
          "name": "Sang 9mm",
          "nameZh": "尚武9毫米",
          "weaponType": "手枪",
          "unlockLabel": "默认配置限定",
          "unlockPlayerRank": null,
          "maxLevel": 34,
          "levelingEnabled": false,
          "gunsmith": false,
          "apexAttachments": [
            "SANG 9&12"
          ],
          "note": "默认配置不支持武器改装与 Apex 配件。",
          "image": "/assets/mw4weapon/sang9mm.webp"
        },
        {
          "code": "PILA",
          "name": "Pila",
          "nameZh": "皮拉",
          "image": "/assets/mw4weapon/pila.webp",
          "weaponType": "发射器",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 36,
          "levelingEnabled": true,
          "gunsmith": false,
          "apexAttachments": [],
          "note": "该武器不支持武器改装。"
        },
        {
          "code": "COMBATKNIFE",
          "name": "Combat Knife",
          "nameZh": "战术小刀",
          "image": "/assets/mw4weapon/combatknife.webp",
          "weaponType": "近战",
          "unlockLabel": "立即解锁",
          "unlockPlayerRank": null,
          "maxLevel": 30,
          "levelingEnabled": true,
          "gunsmith": false,
          "apexAttachments": [],
          "note": "该武器不支持武器改装。"
        },
        {
          "code": "ODEN",
          "name": "Oden",
          "nameZh": "欧登",
          "image": "/assets/mw4weapon/oden.webp",
          "weaponType": "突击步枪",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "CRONENSQUALL",
          "name": "Cronen Squall",
          "nameZh": "克洛南烈风",
          "image": "/assets/mw4weapon/cronensquall.webp",
          "weaponType": "突击步枪",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "VEL46",
          "name": "VEL 46",
          "nameZh": "威尔46",
          "image": "/assets/mw4weapon/vel46.webp",
          "weaponType": "冲锋枪",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "WARLORD",
          "name": "Warlord",
          "nameZh": "战将",
          "image": "/assets/mw4weapon/warlord.webp",
          "weaponType": "霰弹枪",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "MOSINNAGANT",
          "name": "Mosin-Nagant",
          "nameZh": "莫辛纳甘",
          "image": "/assets/mw4weapon/mosinnagant.webp",
          "weaponType": "狙击步枪",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "JOKR",
          "name": "JOKR",
          "nameZh": "标枪导弹",
          "image": "/assets/mw4weapon/jokr.webp",
          "weaponType": "发射器",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "RPG",
          "name": "RPG",
          "nameZh": "RPG-8",
          "image": "",
          "weaponType": "发射器",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "APOC338",
          "name": "APOC 338",
          "nameZh": "APOC 338",
          "image": "/assets/mw4weapon/apoc338.webp",
          "weaponType": "轻机枪",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "MOHAVE357",
          "name": "Mohave .357",
          "nameZh": "莫哈韦点357",
          "image": "/assets/mw4weapon/mohave357.webp",
          "weaponType": "手枪",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "ATALOS57",
          "name": ".57 Atalos",
          "nameZh": "点57阿塔罗斯",
          "image": "/assets/mw4weapon/atalos57.webp",
          "weaponType": "狙击步枪",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        },
        {
          "code": "PM40",
          "name": "PM-40",
          "nameZh": "PM-40铁砧",
          "image": "/assets/mw4weapon/pm40.webp",
          "weaponType": "发射器",
          "unlockLabel": "",
          "unlockPlayerRank": null,
          "maxLevel": 0,
          "levelingEnabled": false,
          "gunsmith": [],
          "apexAttachments": [],
          "note": ""
        }
      ],
      "camoTracks": {
        "version": "v25r1",
        "order": [
          "MULTIPLAYER",
          "WARZONE",
          "DMZ",
          "PYROCLAST"
        ],
        "tiersPerMode": 3,
        "modes": {
          "MULTIPLAYER": {
            "code": "MULTIPLAYER",
            "name": "多人迷彩",
            "shortName": "多人",
            "mastery": true,
            "finalCamo": "Orion Reforged",
            "finalCamoZh": "重铸猎户座",
            "status": "placeholder",
            "items": [
              {
                "code": "TIER_1",
                "tier": 1,
                "name": "Mercurial Drift",
                "nameZh": "霞流",
                "requirement": "所有枪械完成完美主义后，可用任意一把枪完成精通迷彩。前置：先解锁完美主义者（武器精通迷彩）。单局击杀 10 名敌人，累计 250 次。",
                "image": "/assets/mw4camo/mercurial-drift.webp"
              },
              {
                "code": "TIER_2",
                "tier": 2,
                "name": "Polyatomic Reforged",
                "nameZh": "重铸原子",
                "requirement": "完成上一档后：无伤完成 3 杀，累计 250 次。",
                "image": "/assets/mw4camo/polyatomic-reforged.webp"
              },
              {
                "code": "TIER_3",
                "tier": 3,
                "name": "Orion Reforged",
                "nameZh": "重铸猎户座",
                "requirement": "完成上一档后：不死完成 5 杀，累计 250 次。",
                "image": "/assets/mw4camo/orion-reforged.webp"
              }
            ],
            "previewImage": "/assets/tool/camo/MULTIPLAYER.jpg",
            "previewAlt": "多人迷彩预览图"
          },
          "WARZONE": {
            "code": "WARZONE",
            "name": "战区迷彩",
            "shortName": "战区",
            "mastery": true,
            "finalCamo": "Empyros",
            "finalCamoZh": "炽焰",
            "status": "placeholder",
            "items": [
              {
                "code": "TIER_1",
                "tier": 1,
                "name": "Parallax",
                "nameZh": "幻彩",
                "requirement": "完成该武器的基础迷彩挑战后，累计 10 次不死 3 杀。",
                "image": "/assets/mw4camo/parallax.webp"
              },
              {
                "code": "TIER_2",
                "tier": 2,
                "name": "Damascus Reforged",
                "nameZh": "重铸大马士革",
                "requirement": "使用该武器完成 10 次双杀或更高连杀。",
                "image": "/assets/mw4camo/damascus-reforged.webp"
              },
              {
                "code": "TIER_3",
                "tier": 3,
                "name": "Empyros",
                "nameZh": "炽焰",
                "requirement": "累计 3 次不死 5 杀（发射器：单局 5 杀）。条件参考 BO7 推演，MW4 正式条件以上线为准。",
                "image": "/assets/mw4camo/empyros.webp"
              }
            ],
            "previewImage": "/assets/tool/camo/WARZONE.jpg",
            "previewAlt": "战区迷彩预览图"
          },
          "DMZ": {
            "code": "DMZ",
            "name": "DMZ迷彩",
            "shortName": "DMZ",
            "mastery": true,
            "finalCamo": "Helio",
            "finalCamoZh": "日曜",
            "status": "placeholder",
            "items": [
              {
                "code": "TIER_1",
                "tier": 1,
                "name": "Chiral",
                "nameZh": "赤流",
                "requirement": "所有枪械完成完美主义后，可用任意一把枪完成精通迷彩。前置：先解锁完美主义者（武器精通迷彩）。在 3 级区及以上累计击杀 1000 名敌人。",
                "image": "/assets/mw4camo/chiral.webp"
              },
              {
                "code": "TIER_2",
                "tier": 2,
                "name": "Ripple Tide",
                "nameZh": "涟漪潮汐",
                "requirement": "完成上一档后：单局撤离 2 个副官狗牌，累计 25 次。",
                "image": "/assets/mw4camo/ripple-tide.webp"
              },
              {
                "code": "TIER_3",
                "tier": 3,
                "name": "Helio",
                "nameZh": "日曜",
                "requirement": "完成上一档后：使用满级角色与传说品质武器累计击杀 2500 名敌人。",
                "image": "/assets/mw4camo/helio.webp"
              }
            ],
            "previewImage": "/assets/tool/camo/DMZ.jpg",
            "previewAlt": "DMZ迷彩预览图"
          },
          "PYROCLAST": {
            "code": "PYROCLAST",
            "name": "巅峰迷彩",
            "shortName": "巅峰迷彩",
            "mastery": false,
            "finalCamo": "Pyroclast Cascade",
            "finalCamoZh": "火山碎屑倾屑",
            "status": "placeholder",
            "items": [
              {
                "code": "PYROCLAST",
                "name": "Pyroclast",
                "nameZh": "火山碎屑",
                "requirement": "武器满级后可自愿「巅峰转身」：等级归零、配件重新锁定，解锁第 1 档「火山碎屑」。",
                "image": "/assets/mw4camo/pyroclast.webp"
              },
              {
                "code": "PYROCLAST_VERGE",
                "name": "Pyroclast Verge",
                "nameZh": "火山碎屑终点",
                "requirement": "巅峰等级达到对应级数，解锁第 2 档「火山碎屑终点」。",
                "image": "/assets/mw4camo/pyroclast-verge.webp"
              },
              {
                "code": "PYROCLAST_SCATTER",
                "name": "Pyroclast Scatter",
                "nameZh": "火山碎屑散落",
                "requirement": "巅峰等级达到 150 级，解锁第 3 档「火山碎屑散落」。",
                "image": "/assets/mw4camo/pyroclast-scatter.webp"
              },
              {
                "code": "PYROCLAST_CASCADE",
                "name": "Pyroclast Cascade",
                "nameZh": "火山碎屑倾屑",
                "requirement": "巅峰等级达到 250 级，解锁第 4 档「火山碎屑倾屑」。",
                "image": "/assets/mw4camo/pyroclast-cascade.webp"
              }
            ],
            "previewImage": "",
            "previewAlt": "Pyroclast 迷彩"
          }
        },
        "note": "四个标签：多人 / 战区 / DMZ / Pyroclast。每把武器在三个模式各 3 层大师迷彩（Tier 1-3），Pyroclast 为独立 4 款。总项数 = 22 把 × 3 模式 × 3 层 + 4 = 202。",
        "officialSource": "https://www.callofduty.com/"
      },
      "camoGuide": {
        "version": "v25r4",
        "note": "军事/特殊/完成主义迷彩数据与图片已同步 CODMunity 的 6 把完整武器；其余武器为现有攻略估算，标「参考」的项以游戏内实测为准",
        "classes": {
          "突击步枪": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁：10 / 18 / 25 / 34 / 42 / 50 / 57 / 65 级（汉86）",
              "estimate": false,
              "image": "/assets/mw4camo/military.webp",
              "levels": [
                10,
                18,
                25,
                34,
                42,
                50,
                57,
                65
              ]
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "Lv 25 + 架设状态 20 击杀",
                "estimate": false,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "Lv 46 + 消音器 50 击杀",
                "estimate": false,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "Lv 72 + 3 杀不死 ×15",
                "estimate": false,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：50 爆头",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业 8 个谐波挑战",
                "estimate": false,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：25 双杀",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          },
          "冲锋枪": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁（具体等级未公布）",
              "estimate": true,
              "image": "/assets/mw4camo/military.webp",
              "levels": []
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：25 次背后击杀",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业若干谐波挑战（数量未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：50 次腰射击杀",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          },
          "霰弹枪": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁（具体等级未公布）",
              "estimate": true,
              "image": "/assets/mw4camo/military.webp",
              "levels": []
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：125 次腰射击杀",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业 2 个谐波挑战",
                "estimate": false,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：2 杀不换弹 ×25 次",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          },
          "轻机枪": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁（具体等级未公布）",
              "estimate": true,
              "image": "/assets/mw4camo/military.webp",
              "levels": []
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：3 杀不死 ×15 次",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业 3 个谐波挑战",
                "estimate": false,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：2 杀不松扳机 ×25 次",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          },
          "射手步枪": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁（具体等级未公布）",
              "estimate": true,
              "image": "/assets/mw4camo/military.webp",
              "levels": []
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：75 次卧倒击杀",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业 3 个谐波挑战",
                "estimate": false,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：75 次一枪击杀",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          },
          "狙击步枪": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁（具体等级未公布）",
              "estimate": true,
              "image": "/assets/mw4camo/military.webp",
              "levels": []
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：75 次卧倒击杀",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业 3 个谐波挑战",
                "estimate": false,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：75 次一枪击杀",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          },
          "手枪": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁（具体等级未公布）",
              "estimate": true,
              "image": "/assets/mw4camo/military.webp",
              "levels": []
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：20 次背后击杀",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业 2 个谐波挑战",
                "estimate": false,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：40 次腰射击杀",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          },
          "发射器": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁（具体等级未公布）",
              "estimate": true,
              "image": "/assets/mw4camo/military.webp",
              "levels": []
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：2 杀无伤 ×15 次",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业若干谐波挑战（数量未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：单局 10 杀 ×7 次",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          },
          "近战": {
            "military": {
              "count": 8,
              "text": "按武器等级解锁（具体等级未公布）",
              "estimate": true,
              "image": "/assets/mw4camo/military.webp",
              "levels": []
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "武器等级 + 挑战（具体未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局 15 击杀（同职业通用）",
                "estimate": false,
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "本职业全部金皮后：3 杀不死 ×15 次",
                "estimate": false,
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "完成本职业若干谐波挑战（数量未公布）",
                "estimate": true,
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "铂金后：25 次背后击杀",
                "estimate": false,
                "image": "/assets/mw4camo/divergence.webp"
              }
            ]
          }
        },
        "militaryImages": [
          "/assets/mw4camo/military-1.webp",
          "/assets/mw4camo/military-2.webp",
          "/assets/mw4camo/military-3.webp",
          "/assets/mw4camo/military-4.webp",
          "/assets/mw4camo/military-5.webp",
          "/assets/mw4camo/military-6.webp",
          "/assets/mw4camo/military-7.webp",
          "/assets/mw4camo/military-8.webp"
        ],
        "weapons": {
          "HAN86": {
            "maxLevel": "72",
            "unlock": "军阶4级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 72）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "25级，20次架枪击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "46级，消音器50次击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "72级，15次不死3杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次爆头击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次双杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "M4": {
            "maxLevel": "72",
            "unlock": "军阶14级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 72）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "26级，20次零距离击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "51级，20次架枪击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "71级，25次背后击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次爆头击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次双杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "HYEONBURST": {
            "maxLevel": "68",
            "unlock": "军阶21级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 68）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "23级，10次不换弹2次击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "46级，20次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "68级，全自动100次击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次爆头击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次双杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "KASTOV762": {
            "maxLevel": "71",
            "unlock": "军阶34级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 71）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "24级，10次不死3杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "48级，20次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "71级，20次腰射击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次爆头击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次双杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "PATRIOTXMR": {
            "maxLevel": 72,
            "unlock": "军阶27级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 72）：12 / 20 / 28 / 36 / 43 / 52 / 59 / 66",
              "levels": [
                12,
                20,
                28,
                36,
                43,
                52,
                59,
                66
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "24级，20次腰射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "49级，消音器50次击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "72级，30次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次爆头击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次双杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "ISONIGHTSHADE": {
            "maxLevel": "64",
            "unlock": "军阶4级解锁",
            "military": {
              "count": 8,
              "text": "按武器等级解锁：12 / 19 / 25 / 31 / 38 / 44 / 50 / 57 级",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false,
              "levels": [
                12,
                19,
                25,
                31,
                38,
                44,
                50,
                57
              ]
            },
            "special": [
              {
                "name": "Asphalt（沥青）",
                "text": "22级；多人：瞄准射击击杀 50 名玩家；DMZ：瞄准射击击杀 125 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-asphalt.webp"
              },
              {
                "name": "Black Topo（黑色地形）",
                "text": "44级；多人：对玩家完成 10 次双杀；DMZ：对敌人完成 30 次双杀",
                "estimate": false,
                "image": "/assets/mw4camo/special-black-topo.webp"
              },
              {
                "name": "Brown（棕色）",
                "text": "64级；多人：单局不死击杀 3 名玩家，累计 15 次；DMZ：单局无伤击杀 3 名敌人，累计 50 次",
                "estimate": false,
                "image": "/assets/mw4camo/special-brown.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局击杀 15 名玩家；DMZ：单局击杀 100 名敌人",
                "image": "/assets/mw4camo/gold.webp",
                "estimate": false
              },
              {
                "name": "铂金",
                "text": "从背后击杀 25 名玩家；DMZ：从背后击杀 75 名敌人",
                "image": "/assets/mw4camo/platinum.webp",
                "estimate": false
              },
              {
                "name": "和声间奏",
                "text": "腰射击杀 50 名玩家；DMZ：腰射击杀 100 名敌人",
                "image": "/assets/mw4camo/harmonic-break.webp",
                "estimate": false
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流",
            "note": "军事/特殊/完成主义迷彩数据与图片同步自 CODMunity（2026-10-21）；DMZ 为对应模式条件。"
          },
          "VEL46": {
            "maxLevel": "60",
            "unlock": "军阶5级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 60）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "20级，20次卧倒击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "40级，20次卧倒击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "60级，20次爆头击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "25次背后击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "50次腰射击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "WZ55STRIGA": {
            "maxLevel": "60",
            "unlock": "军阶18级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 60）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "20级，消音器50次击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "41级，20次跳跃击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "60级，40次零距离击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "25次背后击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "50次腰射击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "PPSH41": {
            "maxLevel": "62",
            "unlock": "军阶30级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 62）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "20级，10次双杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "41级，20次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "62级，举枪瞄准100次击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "25次背后击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "50次腰射击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "X58NYX": {
            "maxLevel": 53,
            "unlock": "军阶46级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 53）：9 / 15 / 20 / 25 / 29 / 35 / 42 / 47",
              "levels": [
                9,
                15,
                20,
                25,
                29,
                35,
                42,
                47
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "17级，20次移动击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "35级，20次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "53级，40次爆头击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "25次背后击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "50次腰射击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "REZI12": {
            "maxLevel": "60",
            "unlock": "军阶4级解锁",
            "military": {
              "count": 8,
              "text": "按武器等级解锁：11 / 18 / 26 / 30 / 38 / 42 / 47 / 54 级",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false,
              "levels": [
                11,
                18,
                26,
                30,
                38,
                42,
                47,
                54
              ]
            },
            "special": [
              {
                "name": "Struck Oil（钻油）",
                "text": "22级；多人：近距离击杀 20 名玩家；DMZ：近距离击杀 75 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-struck-oil.webp"
              },
              {
                "name": "Brushstroke（笔触）",
                "text": "42级；多人：远距离击杀 10 名玩家；DMZ：远距离击杀 50 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-brushstroke.webp"
              },
              {
                "name": "White Pine（白松）",
                "text": "60级；多人：跳跃击杀 40 名玩家；DMZ：跳跃击杀 100 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-white-pine.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局击杀 15 名玩家；DMZ：单局击杀 100 名敌人",
                "image": "/assets/mw4camo/gold.webp",
                "estimate": false
              },
              {
                "name": "铂金",
                "text": "腰射击杀 125 名玩家；DMZ：腰射击杀 125 名敌人",
                "image": "/assets/mw4camo/platinum.webp",
                "estimate": false
              },
              {
                "name": "和声间奏",
                "text": "不换弹完成 2 杀，累计 25 次；DMZ：不换弹完成 2 杀，累计 50 次",
                "image": "/assets/mw4camo/harmonic-break.webp",
                "estimate": false
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流",
            "note": "军事/特殊/完成主义迷彩数据与图片同步自 CODMunity（2026-10-21）；DMZ 为对应模式条件。"
          },
          "WARLORD": {
            "maxLevel": "60",
            "unlock": "军阶32级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 60）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "21级，20次移动击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "41级，10次双杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "60级，50次卧倒击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次腰射击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次不换弹2次击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "FINNLMG": {
            "maxLevel": "60",
            "unlock": "军阶4级解锁",
            "military": {
              "count": 8,
              "text": "按武器等级解锁：12 / 18 / 24 / 31 / 37 / 42 / 48 / 53 级",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false,
              "levels": [
                12,
                18,
                24,
                31,
                37,
                42,
                48,
                53
              ]
            },
            "special": [
              {
                "name": "Desert Tactics（沙漠战术）",
                "text": "18级；多人：近距离击杀 20 名玩家；DMZ：近距离击杀 75 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-desert-tactics.webp"
              },
              {
                "name": "Green Tiger（绿虎）",
                "text": "39级；多人：不换弹击杀 2 人，累计 10 次；DMZ：不换弹击杀 2 名敌人，累计 30 次",
                "estimate": false,
                "image": "/assets/mw4camo/special-green-tiger.webp"
              },
              {
                "name": "Thunderhead（雷暴云砧）",
                "text": "60级；DMZ：远距离击杀 100 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-thunderhead.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局击杀 15 名玩家；DMZ：单局击杀 100 名敌人",
                "image": "/assets/mw4camo/gold.webp",
                "estimate": false
              },
              {
                "name": "铂金",
                "text": "单局不死完成 3 杀，累计 15 次；DMZ：单局无伤完成 3 杀，累计 50 次",
                "image": "/assets/mw4camo/platinum.webp",
                "estimate": false
              },
              {
                "name": "和声间奏",
                "text": "不松扳机完成 2 杀，累计 25 次；DMZ：不松扳机完成 2 杀，累计 75 次",
                "image": "/assets/mw4camo/harmonic-break.webp",
                "estimate": false
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流",
            "note": "军事/特殊/完成主义迷彩数据与图片同步自 CODMunity（2026-10-21）；DMZ 为对应模式条件。"
          },
          "TYPE73": {
            "maxLevel": "65",
            "unlock": "军阶9级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 65）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "23级，20次架枪击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "45级，10次双杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "65级，40次爆头击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "15次不死3杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次不松扳机2次击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "APOC338": {
            "maxLevel": "56",
            "unlock": "军阶44级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 56）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "15级，20次腰射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "36级，40次爆头击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "56级，100次卧倒击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "15次不死3杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次不松扳机2次击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "MAR9": {
            "maxLevel": "73",
            "unlock": "军阶4级解锁",
            "military": {
              "count": 8,
              "text": "按武器等级解锁：10 / 19 / 29 / 38 / 45 / 52 / 58 / 69 级",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false,
              "levels": [
                10,
                19,
                29,
                38,
                45,
                52,
                58,
                69
              ]
            },
            "special": [
              {
                "name": "Mangrove（红树林）",
                "text": "26级；多人：架设击杀 20 名玩家；DMZ：架设击杀 75 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-mangrove.webp"
              },
              {
                "name": "Fungal Grid（菌格）",
                "text": "49级；多人：蹲伏击杀 25 名玩家；DMZ：蹲伏击杀 75 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-fungal-grid.webp"
              },
              {
                "name": "White（白色）",
                "text": "73级；多人：远距离击杀 30 名玩家；DMZ：远距离击杀 100 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-white.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局击杀 15 名玩家；DMZ：单局击杀 100 名敌人",
                "image": "/assets/mw4camo/gold.webp",
                "estimate": false
              },
              {
                "name": "铂金",
                "text": "趴伏击杀 75 名玩家；DMZ：趴伏击杀 150 名敌人",
                "image": "/assets/mw4camo/platinum.webp",
                "estimate": false
              },
              {
                "name": "和声间奏",
                "text": "一枪击杀 75 名玩家；DMZ：一枪击杀 150 名敌人",
                "image": "/assets/mw4camo/harmonic-break.webp",
                "estimate": false
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流",
            "note": "军事/特殊/完成主义迷彩数据与图片同步自 CODMunity（2026-10-21）；DMZ 为对应模式条件。"
          },
          "ORIS86": {
            "maxLevel": "70",
            "unlock": "军阶11级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 70）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "27级，10次瞬狙击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "49级，20次双杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "70级，15次不死3杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "75次卧倒击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "75次一枪一个",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "MOSINNAGANT": {
            "maxLevel": "69",
            "unlock": "军阶36级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 69）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "25级，25次爆头击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "15次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "20次爆头击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "75次卧倒击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "75次一枪一个",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "KG7VULCAN": {
            "maxLevel": "75",
            "unlock": "军阶4级解锁",
            "military": {
              "count": 8,
              "text": "按武器等级解锁：13 / 22 / 31 / 38 / 47 / 54 / 61 / 69 级",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false,
              "levels": [
                13,
                22,
                31,
                38,
                47,
                54,
                61,
                69
              ]
            },
            "special": [
              {
                "name": "Treeline（林线）",
                "text": "22级；多人：腰射击杀 10 名玩家",
                "estimate": false,
                "image": "/assets/mw4camo/special-treeline.webp"
              },
              {
                "name": "Spidersilk（蛛丝）",
                "text": "47级；多人：爆头击杀 20 名玩家；DMZ：专注状态击杀 75 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-spidersilk.webp"
              },
              {
                "name": "Tension（张力）",
                "text": "75级；多人：单局无伤击杀 2 名玩家，累计 20 次；DMZ：单局无伤击杀 2 名敌人，累计 30 次",
                "estimate": false,
                "image": "/assets/mw4camo/special-tension.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局击杀 15 名玩家；DMZ：单局击杀 100 名敌人",
                "image": "/assets/mw4camo/gold.webp",
                "estimate": false
              },
              {
                "name": "铂金",
                "text": "趴伏击杀 75 名玩家；DMZ：趴伏击杀 150 名敌人",
                "image": "/assets/mw4camo/platinum.webp",
                "estimate": false
              },
              {
                "name": "和声间奏",
                "text": "一枪击杀 75 名玩家；DMZ：一枪击杀 150 名敌人",
                "image": "/assets/mw4camo/harmonic-break.webp",
                "estimate": false
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流",
            "note": "军事/特殊/完成主义迷彩数据与图片同步自 CODMunity（2026-10-21）；DMZ 为对应模式条件。"
          },
          "SIGNAL50": {
            "maxLevel": "74",
            "unlock": "军阶50级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 74）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "28级，10次5秒内2杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "52级，20次瞬狙击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "74级，40次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "75次卧倒击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "75次一枪一个",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "SANG9MM": {
            "maxLevel": "38",
            "unlock": "军阶4级解锁",
            "military": {
              "count": 8,
              "text": "按武器等级解锁：9 / 13 / 17 / 21 / 25 / 28 / 31 / 35 级",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false,
              "levels": [
                9,
                13,
                17,
                21,
                25,
                28,
                31,
                35
              ]
            },
            "special": [
              {
                "name": "Autumn（秋）",
                "text": "13级；多人：瞄准射击击杀 25 名玩家；DMZ：瞄准射击击杀 75 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-autumn.webp"
              },
              {
                "name": "Space Time（时空）",
                "text": "25级；多人：单局不死击杀 3 名玩家，累计 5 次；DMZ：双持击杀 125 名敌人",
                "estimate": false,
                "image": "/assets/mw4camo/special-space-time.webp"
              },
              {
                "name": "Snakeskin（蛇皮）",
                "text": "38级；多人：双持击杀 50 名玩家；DMZ：单局无伤击杀 3 名敌人，累计 15 次",
                "estimate": false,
                "image": "/assets/mw4camo/special-snakeskin.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局击杀 15 名玩家；DMZ：单局击杀 100 名敌人",
                "image": "/assets/mw4camo/gold.webp",
                "estimate": false
              },
              {
                "name": "铂金",
                "text": "从背后击杀 20 名玩家；DMZ：从背后击杀 75 名敌人",
                "image": "/assets/mw4camo/platinum.webp",
                "estimate": false
              },
              {
                "name": "和声间奏",
                "text": "腰射击杀 40 名玩家；DMZ：腰射击杀 100 名敌人",
                "image": "/assets/mw4camo/harmonic-break.webp",
                "estimate": false
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流",
            "note": "军事/特殊/完成主义迷彩数据与图片同步自 CODMunity（2026-10-21）；DMZ 为对应模式条件。"
          },
          "KARITP68": {
            "maxLevel": "28",
            "unlock": "军机7级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 28）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "13级，15次悬挂击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "25级，15次零距离击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "38级，消音器50次击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "20次背后击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "40次腰射击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "50GS": {
            "maxLevel": "50",
            "unlock": "军阶41级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 50）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "18级，20次爆头击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "34级，7次双杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "50级，50零距离击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "20次背后击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "40次腰射击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "MOHAVE357": {
            "maxLevel": "44",
            "unlock": "军阶48级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 44）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "16级，15次一枪一个击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "30级，10次不换弹2杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "44级，30次零距离击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "20次背后击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "40次腰射击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "RPG": {
            "maxLevel": "46",
            "unlock": "军阶23级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 46）：每达到指定等级解锁一款",
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "16级，10次趴下击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "31级，10次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "46级，摧毁30个连杀或载具",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "15次无伤2杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "7次单局10杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "ODEN": {
            "maxLevel": 65,
            "unlock": "军阶39级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 65）：12 / 21 / 25 / 33 / 39 / 46 / 53 / 59",
              "levels": [
                12,
                21,
                25,
                33,
                39,
                46,
                53,
                59
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "25级，配备米里40毫米榴弹发射器25杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "44级，10次不换弹2次击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "65级，举枪瞄准100次击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次爆头击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次双杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "CRONENSQUALL": {
            "maxLevel": 67,
            "unlock": "军阶55级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 67）：10 / 17 / 24 / 31 / 38 / 45 / 54 / 60",
              "levels": [
                10,
                17,
                24,
                31,
                38,
                45,
                54,
                60
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "24级，20次零距离击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "45级，20次架枪击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "67级，15次不死3杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次爆头击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次双杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "AXION": {
            "maxLevel": 69,
            "unlock": "军阶52级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 69）：10 / 19 / 26 / 34 / 41 / 48 / 55 / 62",
              "levels": [
                10,
                19,
                26,
                34,
                41,
                48,
                55,
                62
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "23级，20次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "45级，配备米里40毫米榴弹发射器25杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "69级，25次不换弹2次击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "50次爆头击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次双杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "COMBATKNIFE": {
            "maxLevel": 46,
            "unlock": "军阶4级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 46）：6 / 11 / 16 / 21 / 26 / 31 / 36 / 41",
              "levels": [
                6,
                11,
                16,
                21,
                26,
                31,
                36,
                41
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "16级，25次跳跃击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "31级，25次强力近身处决",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "46级，25次5秒内2杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "15次不死3杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "25次背后击杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "JOKR": {
            "maxLevel": 46,
            "unlock": "军阶43级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 46）：6 / 11 / 16 / 21 / 26 / 31 / 36 / 41",
              "levels": [
                6,
                11,
                16,
                21,
                26,
                31,
                36,
                41
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "16级，5次双杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "31级，10次远射击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "46级，摧毁30个连杀或载具",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "15次无伤2杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "7次单局10杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "PM40": {
            "maxLevel": 46,
            "unlock": "军阶54级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 46）：6 / 11 / 16 / 21 / 26 / 31 / 36 / 41",
              "levels": [
                6,
                11,
                16,
                21,
                26,
                31,
                36,
                41
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "16级，25次直接击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "31级，5次不换弹2杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "46级，40次移动击杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "15次无伤2杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "7次单局10杀",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          },
          "ATALOS57": {
            "maxLevel": 54,
            "unlock": "军阶25级解锁",
            "military": {
              "count": 8,
              "text": "武器等级解锁（满级 54）：10 / 15 / 20 / 26 / 32 / 37 / 42 / 48",
              "levels": [
                10,
                15,
                20,
                26,
                32,
                37,
                42,
                48
              ],
              "image": "/assets/mw4camo/military.webp",
              "estimate": false
            },
            "special": [
              {
                "name": "特殊迷彩 1",
                "text": "20级，10次不死3杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 2",
                "text": "37级，20次架枪击杀",
                "image": "/assets/mw4camo/special.webp"
              },
              {
                "name": "特殊迷彩 3",
                "text": "54级，20次5秒内2杀",
                "image": "/assets/mw4camo/special.webp"
              }
            ],
            "completionist": [
              {
                "name": "金",
                "text": "单局15次击杀",
                "image": "/assets/mw4camo/gold.webp"
              },
              {
                "name": "铂金",
                "text": "75次卧倒击杀",
                "image": "/assets/mw4camo/platinum.webp"
              },
              {
                "name": "和声间奏",
                "text": "75次一枪一个",
                "image": "/assets/mw4camo/harmonic-break.webp"
              },
              {
                "name": "分歧",
                "text": "完成该武器所有的皮肤挑战",
                "image": "/assets/mw4camo/divergence.webp"
              }
            ],
            "mastery": "模式大师-霞流"
          }
        },
        "source": "Lunana66 武器迷彩信息表（docs.qq.com/smartsheet/DQ3FRemRxcHZrTFJo）2026-10-18"
      }
    }
  ]
}
