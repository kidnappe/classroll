/* =========================================================
   课片 Classroll — prototype interactions
   ========================================================= */
(() => {
  "use strict";
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const wait = (ms) => new Promise((r) => setTimeout(r, ms));
  const esc = (s) => String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  const fmt = (n) => Number(n).toLocaleString("en-US");
  const overlayReturnFocus = new WeakMap();

  function openOverlay(overlay) {
    overlayReturnFocus.set(overlay, document.activeElement);
    overlay.inert = false;
    overlay.classList.add("open");
    overlay.setAttribute("aria-hidden", "false");
    setTimeout(() => {
      overlay.querySelector("button, [href], input, select, textarea, [tabindex]:not([tabindex='-1'])")?.focus({ preventScroll: true });
    }, 50);
  }

  function closeOverlay(overlay) {
    if (!overlay.classList.contains("open")) {
      overlay.inert = true;
      overlay.setAttribute("aria-hidden", "true");
      return;
    }
    const returnTo = overlayReturnFocus.get(overlay);
    overlayReturnFocus.delete(overlay);
    if (returnTo instanceof HTMLElement && returnTo.isConnected) returnTo.focus({ preventScroll: true });
    if (!returnTo?.isConnected || overlay.contains(document.activeElement)) {
      $(".screen.is-active button, .screen.is-active [role='button']")?.focus({ preventScroll: true });
    }
    overlay.classList.remove("open");
    overlay.inert = true;
    overlay.setAttribute("aria-hidden", "true");
  }

  const GROUPS = {
    g1: { course: "高等数学", session: "第 12 次课", meta: "11月23日 周一 08:00–09:40 · 教三-201", color: "#d6542e", week: 12, mmdd: "1123" },
    g2: { course: "大学物理", session: "第 12 次课", meta: "11月25日 周三 10:00–11:40 · 教二-105", color: "#3564a8", week: 12, mmdd: "1125" },
    g3: { course: "大学物理", session: "边界 · 课间缓冲", meta: "11月25日 09:40–10:00 · 高数 → 大物", color: "#c58a24", week: 12, mmdd: "1125" },
  };

  const PHOTOS = [
    // —— 像课件 18 ——
    { id: "p01", scene: "slide", title: "泰勒展开", group: "g1", verdict: "likely", score: 96, ocr: 126, screen: .86, face: .02, tag: "文字 126 字 · 屏幕 0.86", time: "08:06" },
    { id: "p02", scene: "board", title: "拉格朗日", group: "g1", verdict: "likely", score: 94, ocr: 98, screen: .41, face: .01, tag: "文字 98 字 · 板书", time: "08:14" },
    { id: "p03", scene: "notes", title: "级数笔记", group: "g1", verdict: "likely", score: 92, ocr: 214, screen: .12, face: .03, tag: "文字 214 字 · 纸面", time: "08:27" },
    { id: "p04", scene: "book", title: "课本 P182", group: "g1", verdict: "likely", score: 88, ocr: 340, screen: .17, face: .01, tag: "文字 340 字 · 书页", time: "08:33" },
    { id: "p05", scene: "whiteboard", title: "极限运算", group: "g1", verdict: "likely", score: 90, ocr: 76, screen: .52, face: .02, tag: "文字 76 字 · 白板", time: "08:41" },
    { id: "p06", scene: "slide", title: "洛必达法则", group: "g1", verdict: "likely", score: 95, ocr: 112, screen: .81, face: .01, tag: "文字 112 字 · 屏幕 0.81", time: "08:48" },
    { id: "p07", scene: "notes", title: "错题整理", group: "g1", verdict: "likely", score: 86, ocr: 188, screen: .09, face: .04, tag: "文字 188 字 · 纸面", time: "08:56" },
    { id: "p08", scene: "projector", title: "课件投影", group: "g1", verdict: "likely", score: 83, ocr: 64, screen: .74, face: .06, tag: "屏幕 0.74 · 投影", time: "09:03" },
    { id: "p09", scene: "book", title: "习题 12-3", group: "g1", verdict: "likely", score: 81, ocr: 265, screen: .14, face: .02, tag: "文字 265 字 · 书页", time: "09:12" },
    { id: "p10", scene: "slide", title: "幂级数", group: "g1", verdict: "likely", score: 93, ocr: 104, screen: .79, face: .01, tag: "文字 104 字 · 屏幕 0.79", time: "09:26" },
    { id: "p11", scene: "slide", title: "电磁感应", group: "g2", verdict: "likely", score: 95, ocr: 118, screen: .88, face: .01, tag: "文字 118 字 · 屏幕 0.88", time: "10:08" },
    { id: "p12", scene: "board", title: "法拉第定律", group: "g2", verdict: "likely", score: 92, ocr: 86, screen: .38, face: .02, tag: "文字 86 字 · 板书", time: "10:17" },
    { id: "p13", scene: "notes", title: "楞次定律", group: "g2", verdict: "likely", score: 89, ocr: 176, screen: .1, face: .03, tag: "文字 176 字 · 纸面", time: "10:25" },
    { id: "p14", scene: "projector", title: "线圈实验", group: "g2", verdict: "likely", score: 84, ocr: 58, screen: .7, face: .08, tag: "屏幕 0.70 · 投影", time: "10:34" },
    { id: "p15", scene: "book", title: "课本 P96", group: "g2", verdict: "likely", score: 82, ocr: 298, screen: .13, face: .02, tag: "文字 298 字 · 书页", time: "10:42" },
    { id: "p16", scene: "whiteboard", title: "右手定则", group: "g2", verdict: "likely", score: 90, ocr: 72, screen: .47, face: .03, tag: "文字 72 字 · 白板", time: "10:51" },
    { id: "p17", scene: "slide", title: "麦克斯韦", group: "g2", verdict: "likely", score: 94, ocr: 132, screen: .84, face: .01, tag: "文字 132 字 · 屏幕 0.84", time: "11:02" },
    { id: "p18", scene: "notes", title: "实验数据", group: "g2", verdict: "likely", score: 87, ocr: 142, screen: .11, face: .04, tag: "文字 142 字 · 纸面", time: "11:18" },
    // —— 存疑 6 ——
    { id: "p19", scene: "poster", title: "学术讲座", group: "g1", verdict: "doubtful", score: 54, ocr: 64, screen: .22, face: .18, tag: "有字海报 · 背景杂乱", time: "08:30" },
    { id: "p20", scene: "desk", title: "模糊笔记", group: "g1", verdict: "doubtful", score: 61, ocr: 42, screen: .19, face: .06, tag: "文字偏少 · 轻微失焦", time: "08:37" },
    { id: "p21", scene: "chat", title: "课程群聊", group: "g1", verdict: "doubtful", score: 44, ocr: 86, screen: .63, face: 0, tag: "截图 · 无内部拍摄时间", time: "09:31" },
    { id: "p22", scene: "projector", title: "反光屏幕", group: "g2", verdict: "doubtful", score: 58, ocr: 46, screen: .66, face: .11, tag: "过曝 · 屏幕特征不稳", time: "10:12" },
    { id: "p23", scene: "notes", title: "手写草稿", group: "g2", verdict: "doubtful", score: 63, ocr: 42, screen: .08, face: .09, tag: "文字 42 字 · 字迹偏淡", time: "10:29" },
    { id: "p24", scene: "poster", title: "社团招新", group: "g2", verdict: "doubtful", score: 39, ocr: 52, screen: .18, face: .24, tag: "海报 · 人脸干扰", time: "11:10" },
    // —— 边界 3 ——
    { id: "p25", scene: "slide", title: "课间 PPT", group: "g3", verdict: "boundary", score: 71, ocr: 94, screen: .76, face: .02, tag: "跨两节课 · 缓冲窗口", time: "09:44" },
    { id: "p26", scene: "board", title: "上一节板书", group: "g3", verdict: "boundary", score: 66, ocr: 58, screen: .35, face: .04, tag: "跨课 · 归属存疑", time: "09:51" },
    { id: "p27", scene: "chat", title: "课表截图", group: "g3", verdict: "boundary", score: 49, ocr: 72, screen: .58, face: 0, tag: "截图时间不可靠", time: "09:58" },
    // —— 不像 7 ——
    { id: "p28", scene: "selfie", title: "自拍", group: "g1", verdict: "unlike", score: 8, ocr: 0, screen: .04, face: .38, tag: "无人文字 · 人脸 38%", time: "08:22" },
    { id: "p29", scene: "food", title: "食堂面", group: "g1", verdict: "unlike", score: 5, ocr: 0, screen: .06, face: .01, tag: "无人文字 · 暖色特写", time: "08:59" },
    { id: "p30", scene: "landscape", title: "晚霞", group: "g1", verdict: "unlike", score: 6, ocr: 0, screen: .09, face: 0, tag: "无人文字 · 户外", time: "09:20" },
    { id: "p31", scene: "group", title: "合照", group: "g1", verdict: "unlike", score: 12, ocr: 0, screen: .03, face: .45, tag: "多人脸 · 45%", time: "09:29" },
    { id: "p32", scene: "food", title: "奶茶", group: "g2", verdict: "unlike", score: 4, ocr: 0, screen: .03, face: 0, tag: "无人文字 · 特写", time: "10:22" },
    { id: "p33", scene: "landscape", title: "操场", group: "g2", verdict: "unlike", score: 7, ocr: 0, screen: .11, face: 0, tag: "无人文字 · 户外", time: "10:46" },
    { id: "p34", scene: "selfie", title: "对镜自拍", group: "g2", verdict: "unlike", score: 9, ocr: 0, screen: .05, face: .31, tag: "人脸 31% · 无人文字", time: "11:14" },
  ];
  PHOTOS.forEach((p) => { p.selected = p.verdict === "likely"; });

  const state = {
    theme: "paper", screen: "courses", filter: "likely",
    scanning: false, scanDone: false, autoRunning: false,
    currentPhoto: null, batch: 34,
    flowOrigin: "courses", draft: null,
    archivedSessions: [], lastArchive: null,
  };

  const TABS = ["courses", "library", "settings"];
  const FLOW_STEPS = ["scan", "confirm", "result"];
  const NAV_ORDER = [...TABS, ...FLOW_STEPS];
  const VERDICT_LABEL = { likely: "像课件", doubtful: "存疑", boundary: "边界", unlike: "不像" };

  const ALBUMS = [
    { name: "高等数学", count: 87, color: "#d6542e", covers: ["p01", "p02", "p03"], meta: "周一 08:00–09:40 · 教三-201" },
    { name: "大学物理", count: 64, color: "#3564a8", covers: ["p11", "p12", "p13"], meta: "周三 10:00–11:40 · 教二-105" },
    { name: "形势与政策", count: 21, color: "#2f6a4f", covers: ["p03", "p04", "p19"], meta: "周五 14:00–15:40 · 单周 · 线上" },
    { name: "程序设计", count: 38, color: "#7b5aa6", covers: ["p05", "p21", "p25"], meta: "周三 14:00–15:40 · 机房 A302" },
  ];

  // 原型固定周三课表，?time=HH:mm 用于预览各时段。
  const requestedDemoTime = new URLSearchParams(location.search).get("time") || "10:24";
  const DEMO_TIME = /^(?:[01]\d|2[0-3]):[0-5]\d$/.test(requestedDemoTime) ? requestedDemoTime : "10:24";
  const WEEKDAY_LABEL = { 1: "周一", 2: "周二", 3: "周三", 4: "周四", 5: "周五", 6: "周六", 7: "周日" };
  const CLASS_SCHEDULE = {
    3: [
      { course: "大学物理", start: "10:00", end: "11:40", session: "第 12 次课", room: "教二-105", color: "#3564a8", archived: 26 },
      { course: "程序设计", start: "14:00", end: "15:40", session: "第 12 次课", room: "机房 A302", color: "#7b5aa6", candidates: 10 },
    ],
    4: [
      { course: "程序设计", start: "14:00", end: "15:40", session: "第 13 次课", room: "机房 A302", color: "#7b5aa6", candidates: 10 },
    ],
  };

  /* ---------------- generated photo scenes ---------------- */
  const FONT = "Microsoft YaHei,PingFang SC,sans-serif";
  const HAND = "KaiTi,STKaiti,SimSun,serif";

  function slideSVG(p) {
    const a = p.group === "g2" ? "#7fa7ff" : "#ff8a5b";
    const bg = p.group === "g2" ? "#101a32" : "#1c2230";
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#0b0e14"/>
      <rect x="15" y="13" width="290" height="214" rx="6" fill="${bg}"/>
      <rect x="15" y="13" width="290" height="4" fill="${a}"/>
      <text x="36" y="58" fill="#f8f7f2" font-size="23" font-weight="700" font-family="${FONT}">${esc(p.title)}</text>
      <text x="36" y="78" fill="rgba(255,255,255,.48)" font-size="8" letter-spacing="2" font-family="${FONT}">LECTURE 12 · ${p.group === "g2" ? "PHYSICS" : "CALCULUS"}</text>
      <rect x="36" y="91" width="48" height="2" fill="${a}"/>
      <rect x="36" y="116" width="28" height="54" fill="${a}" opacity=".9"/>
      <rect x="70" y="138" width="28" height="32" fill="${a}" opacity=".48"/>
      <rect x="104" y="124" width="28" height="46" fill="${a}" opacity=".72"/>
      <rect x="138" y="148" width="28" height="22" fill="${a}" opacity=".34"/>
      <path d="M200 170 L224 140 L247 152 L280 106" fill="none" stroke="${a}" stroke-width="2.5"/>
      <circle cx="280" cy="106" r="3.2" fill="${a}"/>
      <path d="M200 180H280" stroke="rgba(255,255,255,.2)" stroke-width=".8"/>
      <text x="36" y="198" fill="rgba(255,255,255,.34)" font-size="8" font-family="${FONT}">f(x) = Σ aₙ (x − x₀)ⁿ</text>
      <rect x="228" y="184" width="68" height="24" rx="4" fill="rgba(255,255,255,.05)"/>
      <text x="238" y="199" fill="rgba(255,255,255,.55)" font-size="8" font-family="${FONT}">第 12 讲 · 课件</text>
    </svg>`;
  }

  function boardSVG(p) {
    const chalk = "rgba(244,241,226,.9)";
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#1d3028"/>
      <rect width="320" height="15" fill="#6b5137"/>
      <rect y="225" width="320" height="15" fill="#5a452f"/>
      <rect x="16" y="26" width="288" height="182" fill="none" stroke="rgba(255,255,255,.08)"/>
      <text x="30" y="58" fill="${chalk}" font-size="24" font-family="${HAND}" opacity=".95">${esc(p.title)}</text>
      <path d="M30 70 q34 -3 74 0" fill="none" stroke="${chalk}" stroke-width="1.4" opacity=".7"/>
      <text x="30" y="98" fill="${chalk}" font-size="14" font-family="${HAND}" opacity=".85">f(x) = Σ (−1)ⁿ x²ⁿ</text>
      <text x="30" y="122" fill="${chalk}" font-size="12" font-family="${HAND}" opacity=".7">R = 1 / lim |aₙ₊₁ / aₙ|</text>
      <path d="M30 142 q28 -20 58 0 t52 -5 t44 8" fill="none" stroke="${chalk}" stroke-width="1.3" opacity=".62"/>
      <path d="M30 164 h112" stroke="${chalk}" stroke-width=".9" opacity=".4"/>
      <path d="M30 181 c20 -20 35 16 52 -4 s28 12 44 -6" fill="none" stroke="rgba(255,214,120,.55)" stroke-width="1.2"/>
      <g opacity=".72">
        <path d="M214 88 h74 M251 55 v78" stroke="${chalk}" stroke-width="1.1"/>
        <circle cx="251" cy="94" r="26" fill="none" stroke="rgba(255,214,120,.5)" stroke-width="1.2"/>
        <path d="M228 94 h46" stroke="${chalk}" stroke-width=".8" stroke-dasharray="3 3"/>
      </g>
      <text x="203" y="156" fill="rgba(255,255,255,.52)" font-size="9" font-family="${HAND}">收敛半径</text>
      <rect x="66" y="226" width="22" height="4" rx="2" fill="#f3efe2" opacity=".8"/>
      <rect x="94" y="227" width="14" height="3" rx="1.5" fill="#f0c86a" opacity=".8"/>
      <rect x="250" y="225" width="26" height="4" rx="2" fill="#f3efe2" opacity=".5"/>
    </svg>`;
  }

  function notesSVG(p) {
    const lines = Array.from({ length: 11 }, (_, i) => `<path d="M22 ${48 + i * 16}h276" stroke="#b8a98f" stroke-width=".55" opacity=".55"/>`).join("");
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#e7d9c0"/>
      <rect x="8" y="6" width="304" height="228" rx="3" fill="#f4ead5"/>
      ${lines}
      <path d="M46 18v206" stroke="#d9a094" stroke-width=".9" opacity=".6"/>
      <circle cx="22" cy="34" r="4" fill="#d8cbb3"/><circle cx="22" cy="120" r="4" fill="#d8cbb3"/><circle cx="22" cy="206" r="4" fill="#d8cbb3"/>
      <rect x="55" y="54" width="132" height="19" fill="#ffe066" opacity=".62" rx="2"/>
      <text x="58" y="68" fill="#2d332c" font-size="17" font-family="${HAND}" font-weight="700">${esc(p.title)}</text>
      <text x="58" y="90" fill="#475044" font-size="9" font-family="${HAND}" opacity=".75">第 12 周 · 重点复习</text>
      <path d="M58 108 q22 -7 44 0 t38 -3 t44 2" fill="none" stroke="#3f4a41" stroke-width="1.2" opacity=".68"/>
      <path d="M58 124 q30 -4 58 0 t50 -2 t38 1" fill="none" stroke="#3f4a41" stroke-width="1.2" opacity=".55"/>
      <path d="M58 140 q18 6 35 0 t33 2 t41 -3" fill="none" stroke="#3f4a41" stroke-width="1.1" opacity=".6"/>
      <text x="58" y="162" fill="#3f4a41" font-size="9" font-family="${HAND}" opacity=".7">① 先看收敛区间</text>
      <text x="58" y="180" fill="#3f4a41" font-size="9" font-family="${HAND}" opacity=".7">② 再代端点检查</text>
      <rect x="56" y="190" width="108" height="13" fill="#ffb4a2" opacity=".38" rx="2"/>
      <g transform="rotate(4 252 78)">
        <rect x="222" y="52" width="72" height="58" fill="#ffe9a8" opacity=".92"/>
        <text x="231" y="76" fill="#7d4d1e" font-size="10" font-family="${HAND}">考试会考</text>
        <path d="M231 86 q22 -4 48 0" stroke="#9b6a31" stroke-width=".8" opacity=".7"/>
        <path d="M231 97 q16 3 32 0" stroke="#9b6a31" stroke-width=".8" opacity=".5"/>
      </g>
      <path d="M192 205 q40 10 78 2" stroke="#7d8b7e" stroke-width="1" fill="none" opacity=".45"/>
      <rect x="238" y="188" width="42" height="5" rx="2.5" fill="#c8b89c" opacity=".5"/>
    </svg>`;
  }

  function bookSVG(p) {
    const lines = (x, y, w, n) => Array.from({ length: n }, (_, i) => `<path d="M${x} ${y + i * 11}h${w - (i % 3) * 8}" stroke="#9c978d" stroke-width=".7" opacity=".6"/>`).join("");
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#b9ad98"/>
      <rect x="18" y="20" width="284" height="200" rx="4" fill="#d7c9ad" opacity=".5"/>
      <path d="M160 42 Q120 28 46 38 L46 202 Q122 192 160 208 Z" fill="#f2ecdf"/>
      <path d="M160 42 Q200 28 274 38 L274 202 Q198 192 160 208 Z" fill="#faf5e9"/>
      <path d="M160 42 V208" stroke="#cfc5b2" stroke-width="2"/>
      ${lines(58, 62, 84, 9)}
      <text x="58" y="54" fill="#5b564d" font-size="9" font-family="${FONT}">§ 12.3 幂级数</text>
      <g>
        <path d="M190 72 h62 v38 h-62z" fill="none" stroke="#9a9488" stroke-width=".8"/>
        <path d="M198 98 L216 78 L240 96" fill="none" stroke="#b66d43" stroke-width="1.2"/>
        <text x="200" y="122" fill="#817a6d" font-size="7" font-family="${FONT}">图 12-2</text>
      </g>
      ${lines(190, 138, 66, 5)}
      <rect x="58" y="176" width="86" height="12" fill="#ffe066" opacity=".55"/>
      <text x="58" y="186" fill="#514b41" font-size="8" font-family="${HAND}">重点：先求收敛半径</text>
      <path d="M190 190 h68" stroke="#c9bfa9" stroke-width=".8"/>
      <text x="190" y="201" fill="#878073" font-size="7" font-family="${FONT}">P. 182 · 习题 12-3</text>
      <g fill="#8e8575" opacity=".55">
        <circle cx="160" cy="55" r="2"/><circle cx="160" cy="76" r="2"/><circle cx="160" cy="97" r="2"/>
        <circle cx="160" cy="118" r="2"/><circle cx="160" cy="139" r="2"/><circle cx="160" cy="160" r="2"/>
        <circle cx="160" cy="181" r="2"/>
      </g>
    </svg>`;
  }

  function whiteboardSVG(p) {
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#d9d3c6"/>
      <rect x="12" y="18" width="296" height="184" rx="3" fill="#fbfbf7" stroke="#bdb8ac" stroke-width="1.2"/>
      <text x="28" y="48" fill="#2b3d58" font-size="17" font-family="${HAND}" font-weight="700">${esc(p.title)}</text>
      <rect x="28" y="58" width="44" height="2" fill="#3f6fa8" opacity=".7"/>
      <text x="28" y="82" fill="#3f4a54" font-size="10" font-family="${FONT}">lim (1 + 1/n)ⁿ = e</text>
      <text x="28" y="102" fill="#9b4d42" font-size="9" font-family="${FONT}">先等价无穷小，再洛必达</text>
      <g stroke="#3f6fa8" stroke-width="1.4" fill="none">
        <path d="M86 118 h54"/><path d="m134 113 6 5-6 5"/>
      </g>
      <g transform="rotate(-2 206 92)">
        <rect x="158" y="66" width="98" height="54" rx="2" fill="#fff3bf"/>
        <text x="168" y="86" fill="#72551b" font-size="9" font-family="${HAND}">判别法顺序</text>
        <text x="168" y="102" fill="#72551b" font-size="8" font-family="${HAND}">比值 → 根值 → 比较</text>
      </g>
      <path d="M158 144 q30 20 66 4" stroke="#a45d4a" stroke-width="1.2" fill="none"/>
      <text x="28" y="132" fill="#3f4a54" font-size="8" font-family="${FONT}" opacity=".65">注：端点单独判</text>
      <rect x="26" y="145" width="94" height="36" fill="#eef3f8" stroke="#b9c7d6"/>
      <text x="34" y="160" fill="#52657a" font-size="8" font-family="${FONT}">① n → ∞</text>
      <text x="34" y="173" fill="#52657a" font-size="8" font-family="${FONT}">② 取对数</text>
      <rect x="8" y="202" width="304" height="8" rx="4" fill="#b8b2a5"/>
      <rect x="242" y="195" width="42" height="5" rx="2.5" fill="#426fa3"/>
      <rect x="250" y="196" width="26" height="3" rx="1.5" fill="#c35b48"/>
    </svg>`;
  }

  function projectorSVG(p) {
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#0b0c10"/>
      <path d="M128 38 36 186 82 196 160 52Z" fill="#dfe9ef" opacity=".055"/>
      <path d="M196 38 292 186 246 196 162 52Z" fill="#dfe9ef" opacity=".055"/>
      <rect x="58" y="36" width="204" height="124" rx="4" fill="#f4f5ee"/>
      <rect x="58" y="36" width="204" height="6" fill="#d6d9d0"/>
      <text x="78" y="70" fill="#233244" font-size="17" font-weight="700" font-family="${FONT}">${esc(p.title)}</text>
      <text x="78" y="86" fill="#79838f" font-size="7" font-family="${FONT}">第 12 讲 · 课堂投影</text>
      <rect x="78" y="100" width="60" height="3" fill="#c06b47"/>
      <rect x="78" y="114" width="38" height="28" fill="#dce3e8"/>
      <rect x="122" y="106" width="38" height="36" fill="#e8edf1"/>
      <rect x="166" y="118" width="38" height="24" fill="#dce3e8"/>
      <path d="M224 84 h20 M234 74 v20" stroke="#9faab4" stroke-width="1"/>
      <g fill="#08090c">
        <circle cx="82" cy="214" r="22"/><rect x="60" y="214" width="44" height="26" rx="10"/>
        <circle cx="162" cy="220" r="19"/><rect x="143" y="220" width="38" height="20" rx="8"/>
        <circle cx="244" cy="214" r="21"/><rect x="223" y="214" width="42" height="26" rx="9"/>
      </g>
      <rect x="284" y="24" width="6" height="6" rx="1" fill="#c44536" opacity=".8"/>
      <rect x="58" y="160" width="204" height="10" fill="#c9cfc6" opacity=".25"/>
      <path d="M0 196h320" stroke="#1b1f27" stroke-width="2"/>
    </svg>`;
  }

  function posterSVG(p) {
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#cfc7b8"/>
      <rect x="38" y="18" width="244" height="212" fill="#b6ae9f" opacity=".35"/>
      <g transform="rotate(-1.5 160 120)">
        <rect x="42" y="22" width="236" height="204" fill="#f6f0df"/>
        <rect x="42" y="22" width="236" height="54" fill="#29405c"/>
        <text x="60" y="48" fill="#f7f1e1" font-size="10" font-family="${FONT}" letter-spacing="2">ACADEMIC TALK</text>
        <text x="60" y="69" fill="#f7f1e1" font-size="20" font-weight="700" font-family="${FONT}">${esc(p.title)}</text>
        <text x="60" y="100" fill="#4f5b64" font-size="9" font-family="${FONT}">从傅里叶级数到信号处理</text>
        <text x="60" y="116" fill="#7c847f" font-size="7" font-family="${FONT}">11月26日 19:00 · 图书馆报告厅</text>
        <g fill="#c7c0b0">
          <rect x="60" y="134" width="110" height="4"/><rect x="60" y="144" width="92" height="4"/>
          <rect x="60" y="154" width="104" height="4"/><rect x="60" y="164" width="70" height="4"/>
        </g>
        <g fill="#2d3334">
          <rect x="214" y="132" width="44" height="44"/>
          <rect x="220" y="138" width="10" height="10" fill="#f6f0df"/><rect x="242" y="138" width="10" height="10" fill="#f6f0df"/>
          <rect x="220" y="158" width="10" height="10" fill="#f6f0df"/><rect x="238" y="162" width="14" height="6" fill="#f6f0df"/>
        </g>
        <text x="60" y="202" fill="#8f938a" font-size="7" font-family="${FONT}">扫码报名 · 座位有限</text>
        <text x="60" y="214" fill="#b1b3a9" font-size="6" font-family="${FONT}">STUDENT UNION · 2026</text>
      </g>
      <rect x="30" y="12" width="34" height="12" fill="#e9d9a9" opacity=".55" transform="rotate(-12 47 18)"/>
      <rect x="266" y="216" width="30" height="11" fill="#e9d9a9" opacity=".45" transform="rotate(8 281 221)"/>
    </svg>`;
  }

  function chatSVG(p) {
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#d8d3ca"/>
      <rect x="34" y="14" width="252" height="226" rx="18" fill="#ededed"/>
      <rect x="34" y="14" width="252" height="34" rx="18" fill="#f7f7f7"/>
      <rect x="34" y="34" width="252" height="14" fill="#f7f7f7"/>
      <text x="154" y="36" fill="#2b2b2b" font-size="9" font-family="${FONT}" text-anchor="middle">高等数学 · 课程群 (48)</text>
      <g>
        <rect x="182" y="60" width="86" height="26" rx="7" fill="#95ec69"/>
        <text x="193" y="77" fill="#1f2a1b" font-size="7.5" font-family="${FONT}">老师，课件在哪？</text>
        <circle cx="272" cy="52" r="8" fill="#c8c2b7"/><text x="272" y="55" fill="#fff" font-size="7" text-anchor="middle">我</text>
      </g>
      <g>
        <circle cx="50" cy="100" r="8" fill="#8fa8c8"/><text x="50" y="103" fill="#fff" font-size="7" text-anchor="middle">学</text>
        <rect x="64" y="92" width="104" height="30" rx="7" fill="#fff"/>
        <text x="74" y="106" fill="#282828" font-size="7.5" font-family="${FONT}">已传到班级云盘，第 12 讲</text>
        <text x="74" y="116" fill="#8b8b8b" font-size="6" font-family="${FONT}">今天 09:32</text>
      </g>
      <g>
        <rect x="182" y="132" width="64" height="24" rx="7" fill="#95ec69"/>
        <text x="192" y="147" fill="#1f2a1b" font-size="7.5" font-family="${FONT}">收到，谢谢老师</text>
      </g>
      <g>
        <rect x="64" y="170" width="128" height="38" rx="5" fill="#fff"/>
        <rect x="70" y="176" width="46" height="26" rx="2" fill="#d7e3f0"/>
        <text x="82" y="192" fill="#748aa2" font-size="6" font-family="${FONT}">PPT</text>
        <text x="123" y="184" fill="#3d3d3d" font-size="7" font-family="${FONT}">第12讲-级数.pdf</text>
        <text x="123" y="194" fill="#969696" font-size="6" font-family="${FONT}">4.8 MB</text>
      </g>
      <rect x="34" y="216" width="252" height="24" fill="#f7f7f7"/>
      <rect x="48" y="220" width="180" height="16" rx="8" fill="#fff"/>
      <text x="57" y="230" fill="#b0b0b0" font-size="6" font-family="${FONT}">输入消息…</text>
      <rect x="238" y="220" width="34" height="16" rx="8" fill="#95ec69"/>
      <text x="255" y="230" fill="#2d4a1f" font-size="6" text-anchor="middle" font-family="${FONT}">发送</text>
      <text x="160" y="10" fill="rgba(0,0,0,.4)" font-size="6" text-anchor="middle" font-family="${FONT}">截图 · 2026/11/23 09:34</text>
    </svg>`;
  }

  function selfieSVG(p) {
    const warm = p.group === "g2";
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="${warm ? "#c78a72" : "#7f8f9e"}"/>
      <rect y="0" width="320" height="86" fill="${warm ? "#e7b291" : "#a9bac8"}"/>
      <circle cx="260" cy="38" r="42" fill="#fff" opacity=".12"/>
      <path d="M0 194 Q80 158 160 182 T320 168 V240 H0Z" fill="${warm ? "#9a5f4c" : "#5c6a77"}" opacity=".55"/>
      <path d="M78 240 Q82 172 160 168 Q238 172 242 240Z" fill="#39424b"/>
      <path d="M138 164 h44 v22 h-44z" fill="#d9a58c"/>
      <ellipse cx="160" cy="116" rx="52" ry="62" fill="#e3b096"/>
      <path d="M108 112 Q104 48 160 46 Q216 48 212 112 Q204 82 160 80 Q118 80 108 112Z" fill="#2c2622"/>
      <path d="M112 96 Q118 52 160 52 Q202 52 208 96 Q184 72 160 72 Q132 72 112 96Z" fill="#3a302a"/>
      <circle cx="141" cy="116" r="3.6" fill="#2e2622"/>
      <circle cx="180" cy="116" r="3.6" fill="#2e2622"/>
      <path d="M153 136 q7 5 15 0" stroke="#a35d51" stroke-width="2.2" fill="none" stroke-linecap="round"/>
      <path d="M144 146 q16 10 32 0" stroke="#8f4d46" stroke-width="2.2" fill="none" stroke-linecap="round"/>
      <circle cx="126" cy="132" r="7" fill="#d58f7b" opacity=".55"/>
      <circle cx="195" cy="132" r="7" fill="#d58f7b" opacity=".55"/>
      <path d="M208 196 q28 -22 48 2" stroke="#39424b" stroke-width="20" fill="none" stroke-linecap="round"/>
      <rect x="228" y="186" width="12" height="42" rx="6" fill="#d9a58c"/>
      <text x="16" y="230" fill="rgba(255,255,255,.5)" font-size="7" font-family="${FONT}">IMG_${p.id.slice(1)} · 自拍</text>
    </svg>`;
  }

  function foodSVG(p) {
    const tea = p.title.includes("奶茶");
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#8e6044"/>
      <path d="M0 48 Q80 60 160 44 T320 52 V240 H0Z" fill="#a97856" opacity=".55"/>
      <ellipse cx="160" cy="172" rx="102" ry="26" fill="#5e3c2a" opacity=".35"/>
      ${tea ? `
        <path d="M118 76 h84 l-10 106 h-64z" fill="#e6d3b7"/>
        <rect x="118" y="76" width="84" height="16" fill="#f3ead8"/>
        <path d="M126 106 q34 -10 68 0 v52 q-34 10 -68 0z" fill="#b98762"/>
        <g fill="#6f4a34"><circle cx="142" cy="136" r="5"/><circle cx="158" cy="146" r="5"/><circle cx="174" cy="132" r="5"/><circle cx="166" cy="158" r="4"/><circle cx="148" cy="160" r="4"/></g>
        <rect x="152" y="50" width="6" height="46" rx="3" fill="#5a8c6a" transform="rotate(6 155 73)"/>
        <text x="160" y="206" fill="rgba(255,255,255,.55)" font-size="7" text-anchor="middle" font-family="${FONT}">奶茶 · 三分糖</text>
      ` : `
        <ellipse cx="160" cy="148" rx="78" ry="46" fill="#e8e2d5"/>
        <ellipse cx="160" cy="148" rx="64" ry="36" fill="#b8803f"/>
        <g fill="none" stroke="#e3b775" stroke-width="4" stroke-linecap="round">
          <path d="M112 140 q24 -18 48 0 t48 0"/><path d="M108 152 q26 -16 52 0 t52 0"/>
          <path d="M118 164 q22 -12 44 0 t44 0"/>
        </g>
        <path d="M196 100 L246 138" stroke="#4e3929" stroke-width="4" stroke-linecap="round"/>
        <path d="M204 94 L254 132" stroke="#4e3929" stroke-width="4" stroke-linecap="round"/>
        <ellipse cx="160" cy="192" rx="70" ry="10" fill="#5e3c2a" opacity=".4"/>
        <g stroke="#e6ddd0" stroke-width="1.2" fill="none" opacity=".55">
          <path d="M140 98 q-8 -18 4 -30"/><path d="M164 94 q-8 -20 4 -32"/><path d="M188 98 q-8 -18 4 -30"/>
        </g>
        <text x="160" y="216" fill="rgba(255,255,255,.5)" font-size="7" text-anchor="middle" font-family="${FONT}">食堂 · 牛肉面</text>
      `}
    </svg>`;
  }

  function landscapeSVG(p) {
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#f0b477"/>
      <rect y="0" width="320" height="58" fill="#e79b68"/>
      <rect y="58" width="320" height="38" fill="#edaa75"/>
      <rect y="96" width="320" height="44" fill="#f4c28e"/>
      <circle cx="238" cy="82" r="28" fill="#ffdf9f"/>
      <circle cx="238" cy="82" r="36" fill="#ffdf9f" opacity=".18"/>
      <path d="M0 156 L58 94 L104 142 L152 108 L214 160 L272 118 L320 150 V240 H0Z" fill="#7a685d"/>
      <path d="M0 178 L54 138 L118 174 L184 136 L250 176 L320 150 V240 H0Z" fill="#5f514c"/>
      <path d="M0 204 Q70 182 150 200 T320 188 V240 H0Z" fill="#393733"/>
      <path d="M0 220 Q88 204 168 216 T320 210 V240 H0Z" fill="#242521"/>
      <g stroke="#4e4038" stroke-width="1" fill="none" opacity=".65">
        <path d="M64 134 q3 -8 7 0 M72 138 q3 -7 6 0"/>
      </g>
      <g fill="#fff" opacity=".55">
        <path d="M72 72 q14 -7 24 0 q14 -5 22 4 q-22 4 -46 0Z"/>
        <path d="M104 52 q10 -5 18 0 q10 -4 16 3 q-16 3 -34 0Z"/>
      </g>
      <text x="16" y="228" fill="rgba(255,255,255,.55)" font-size="7" font-family="${FONT}">傍晚 · 操场边</text>
    </svg>`;
  }

  function groupSVG(p) {
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#9aaba0"/>
      <rect y="0" width="320" height="100" fill="#bac8bd"/>
      <path d="M0 176 Q80 152 160 168 T320 158 V240 H0Z" fill="#87998c"/>
      <circle cx="60" cy="64" r="22" fill="#e9e2ce" opacity=".35"/>
      <g>
        <path d="M18 240 q4 -58 48 -58 q44 0 48 58Z" fill="#5d6e7d"/>
        <circle cx="66" cy="156" r="25" fill="#ddb092"/>
        <path d="M41 154 q2 -30 26 -30 q24 0 24 30 q-10 -18 -25 -18 q-16 0 -25 18Z" fill="#3b332d"/>
        <circle cx="58" cy="158" r="1.8" fill="#2b2622"/><circle cx="74" cy="158" r="1.8" fill="#2b2622"/>
        <path d="M60 168 q6 4 12 0" stroke="#8f5b4c" stroke-width="1.5" fill="none"/>
      </g>
      <g>
        <path d="M108 240 q2 -70 52 -70 q50 0 52 70Z" fill="#7a6258"/>
        <circle cx="160" cy="136" r="30" fill="#e6b89a"/>
        <path d="M130 136 q0 -38 30 -38 q30 0 30 38 q-14 -24 -30 -24 q-18 0 -30 24Z" fill="#40342d"/>
        <circle cx="150" cy="139" r="2" fill="#2b2622"/><circle cx="170" cy="139" r="2" fill="#2b2622"/>
        <path d="M152 151 q8 6 16 0" stroke="#9b5b4e" stroke-width="1.7" fill="none"/>
      </g>
      <g>
        <path d="M206 240 q4 -58 44 -58 q42 0 46 58Z" fill="#495d6a"/>
        <circle cx="250" cy="160" r="23" fill="#d7a487"/>
        <path d="M227 158 q2 -28 23 -28 q24 0 23 28 q-8 -16 -23 -16 q-14 0 -23 16Z" fill="#332a25"/>
        <circle cx="243" cy="161" r="1.7" fill="#2b2622"/><circle cx="257" cy="161" r="1.7" fill="#2b2622"/>
        <path d="M245 170 q5 4 10 0" stroke="#8a5148" stroke-width="1.4" fill="none"/>
      </g>
      <path d="M16 216 q30 -12 62 0" stroke="#c7d0c8" stroke-width="1" fill="none" opacity=".4"/>
      <text x="240" y="228" fill="rgba(255,255,255,.45)" font-size="7" font-family="${FONT}">同学合照</text>
    </svg>`;
  }

  function deskSVG(p) {
    return `<svg viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <rect width="320" height="240" fill="#8d7359"/>
      <rect y="0" width="320" height="34" fill="#a4896c"/>
      <g transform="rotate(-3 160 130)">
        <rect x="46" y="42" width="216" height="166" rx="3" fill="#f2e7d2"/>
        <rect x="62" y="58" width="182" height="134" fill="none" stroke="#d8cbb7" stroke-width=".8"/>
        <g stroke="#b8a98f" stroke-width=".5" opacity=".6">
          <path d="M64 80h180M64 96h180M64 112h180M64 128h180M64 144h180M64 160h180M64 176h180"/>
        </g>
        <path d="M74 74 q30 -4 62 0 t54 -2" stroke="#59655c" stroke-width="1.2" fill="none" opacity=".6"/>
        <path d="M74 90 q40 -3 70 0 t48 -1" stroke="#59655c" stroke-width="1.1" fill="none" opacity=".5"/>
        <path d="M74 106 q24 4 50 0 t34 2" stroke="#59655c" stroke-width="1" fill="none" opacity=".45"/>
        <rect x="78" y="132" width="74" height="12" fill="#ffe066" opacity=".5"/>
        <text x="80" y="141" fill="#4f584f" font-size="8" font-family="${HAND}">第 12 周</text>
        <path d="M78 158 q36 8 76 -2" stroke="#7c867b" stroke-width="1" fill="none" opacity=".4"/>
      </g>
      <path d="M236 78 L296 172" stroke="#3a3f42" stroke-width="5" stroke-linecap="round"/>
      <path d="M242 72 L302 166" stroke="#6e5e4c" stroke-width="3" stroke-linecap="round"/>
      <ellipse cx="78" cy="204" rx="34" ry="10" fill="#4a3d30" opacity=".6"/>
      <path d="M54 146 h48 v52 h-48z" fill="#e9e4d9"/>
      <path d="M54 146 h48 l-6 -10 h-36z" fill="#d9d2c5"/>
      <path d="M60 158 q14 6 32 0 v20 q-16 5 -32 0z" fill="#7b4c33"/>
      <path d="M80 152 q-4 -18 4 -26" stroke="#b8b3a8" stroke-width="1" fill="none" opacity=".5"/>
      <rect x="224" y="186" width="62" height="30" rx="4" fill="#e7dfd0" opacity=".75"/>
      <text x="232" y="198" fill="#6f685c" font-size="7" font-family="${FONT}">焦点没对准</text>
      <text x="232" y="208" fill="#989185" font-size="6" font-family="${FONT}">手抖 · 反光</text>
    </svg>`;
  }

  function photoSVG(p) {
    switch (p.scene) {
      case "slide": return slideSVG(p);
      case "board": return boardSVG(p);
      case "notes": return notesSVG(p);
      case "book": return bookSVG(p);
      case "whiteboard": return whiteboardSVG(p);
      case "projector": return projectorSVG(p);
      case "poster": return posterSVG(p);
      case "chat": return chatSVG(p);
      case "selfie": return selfieSVG(p);
      case "food": return foodSVG(p);
      case "landscape": return landscapeSVG(p);
      case "group": return groupSVG(p);
      case "desk": return deskSVG(p);
      default: return notesSVG(p);
    }
  }

  /* ---------------- preview grid ---------------- */
  const photoGroups = $("#photoGroups");
  const segThumb = $("#segThumb");
  const segmented = $("#segmented");
  const selectedCount = $("#selectedCount");
  const archiveBtn = $("#archiveBtn");
  const batchBar = $("#batchBar");

  function cardHTML(p, i) {
    return `<button type="button" class="photo-card ${p.selected ? "sel" : ""} verdict-${p.verdict}" data-id="${p.id}" aria-pressed="${p.selected}" aria-label="${esc(GROUPS[p.group].course)} ${esc(p.title)}">
      <span class="photo-media">${photoSVG(p)}</span>
      <span class="ph-check"><svg class="icon"><use href="#i-check"/></svg></span>
      <span class="ph-score">${p.score}</span>
      <span class="ph-tag">${esc(p.tag)}</span>
    </button>`;
  }

  function groupHeader(gk, list) {
    const g = GROUPS[gk];
    return `<div class="group-head">
      <div class="group-main">
        <h3><span class="course-swatch" style="--c:${g.color}"></span>${esc(g.course)} · ${esc(g.session)}</h3>
        <p>${esc(g.meta)} · ${list.length} 张</p>
      </div>
      <button type="button" class="group-skip" data-group-skip="${gk}">整组不归档</button>
    </div>`;
  }

  function renderPhotos() {
    const list = PHOTOS.filter((p) => p.verdict === state.filter);
    const keys = [...new Set(list.map((p) => p.group))];
    let index = 0;
    photoGroups.innerHTML = keys.length
      ? keys.map((gk) => {
          const items = list.filter((p) => p.group === gk);
          return `<div class="group">
            ${groupHeader(gk, items)}
            <div class="photo-grid">${items.map((p) => {
              const delay = Math.min(index++, 13) * 26;
              return cardHTML(p, delay).replace('class="photo-card', `style="--d:${delay}ms" class="photo-card`);
            }).join("")}</div>
          </div>`;
        }).join("")
      : `<div class="empty-state">这一档是空的。</div>`;
    updateBatch();
  }

  function updateCard(id) {
    const p = PHOTOS.find((x) => x.id === id);
    const el = $(`.photo-card[data-id="${id}"]`);
    if (p && el) {
      el.classList.toggle("sel", p.selected);
      el.setAttribute("aria-pressed", String(p.selected));
    }
  }

  function updateBatch() {
    const count = PHOTOS.filter((p) => p.selected).length;
    const active = state.screen === "confirm" && state.flowStep === "confirm";
    selectedCount.textContent = count;
    archiveBtn.style.opacity = count ? "1" : ".55";
    batchBar.inert = !active;
    batchBar.setAttribute("aria-hidden", String(!active));
  }

  function toggleSelect(id, force) {
    const p = PHOTOS.find((x) => x.id === id);
    if (!p) return;
    p.selected = typeof force === "boolean" ? force : !p.selected;
    updateCard(id);
    updateBatch();
  }

  function moveSeg() {
    const active = segmented.querySelector("button.active");
    if (!active || !segThumb) return;
    segThumb.style.width = active.offsetWidth + "px";
    segThumb.style.transform = `translateX(${active.offsetLeft}px)`;
  }

  segmented.addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-filter]");
    if (!btn || btn.classList.contains("active")) return;
    state.filter = btn.dataset.filter;
    $$("button[data-filter]", segmented).forEach((b) => {
      const on = b === btn;
      b.classList.toggle("active", on);
      b.setAttribute("aria-selected", String(on));
    });
    moveSeg();
    renderPhotos();
  });

  photoGroups.addEventListener("click", (e) => {
    const skip = e.target.closest("[data-group-skip]");
    if (skip) {
      const gk = skip.dataset.groupSkip;
      PHOTOS.filter((p) => p.group === gk && p.verdict === state.filter).forEach((p) => { p.selected = false; });
      $$(`.photo-card`).forEach((el) => {
        const p = PHOTOS.find((x) => x.id === el.dataset.id);
        if (p) { el.classList.toggle("sel", p.selected); el.setAttribute("aria-pressed", String(p.selected)); }
      });
      updateBatch();
      toast(`已排除「${GROUPS[gk].course}」本组照片`);
      return;
    }
    const card = e.target.closest(".photo-card");
    if (!card) return;
    if (e.target.closest(".ph-check")) toggleSelect(card.dataset.id);
    else openViewer(card.dataset.id);
  });

  $("#selectAll").addEventListener("click", () => {
    PHOTOS.filter((p) => p.verdict === state.filter).forEach((p) => { p.selected = true; });
    renderPhotos();
    toast("已全选本档照片");
  });
  $("#invertSelect").addEventListener("click", () => {
    PHOTOS.filter((p) => p.verdict === state.filter).forEach((p) => { p.selected = !p.selected; });
    renderPhotos();
    toast("已反选本档照片");
  });

  /* ---------------- photo viewer ---------------- */
  const viewerLayer = $("#viewerLayer");
  const viewerPhoto = $("#viewerPhoto");
  const viewerTitle = $("#viewerTitle");
  const viewerScore = $("#viewerScore");
  const viewerMeta = $("#viewerMeta");
  const viewerReasons = $("#viewerReasons");

  function openViewer(id) {
    const p = PHOTOS.find((x) => x.id === id);
    if (!p) return;
    state.currentPhoto = p;
    const g = GROUPS[p.group];
    viewerPhoto.innerHTML = photoSVG(p);
    viewerTitle.textContent = p.title;
    viewerScore.textContent = p.score;
    viewerMeta.textContent = `${g.course} · ${g.session} · ${p.time} · ${VERDICT_LABEL[p.verdict]}`;
    const reasons = [
      { label: "文字量 · 主信号", value: p.ocr ? `${p.ocr} 字` : "0 字", cls: p.ocr >= 60 ? "high" : p.ocr > 15 ? "mid" : "low", w: p.ocr >= 60 ? "+2" : p.ocr > 15 ? "+1" : "0" },
      { label: "屏幕 / 投影", value: p.screen.toFixed(2), cls: p.screen >= .55 ? "high" : p.screen >= .22 ? "mid" : "low", w: p.screen >= .55 ? "+1" : p.screen >= .22 ? "+0.5" : "0" },
      { label: "人脸占比 · 负信号", value: `${(p.face * 100).toFixed(1)}%`, cls: p.face >= .25 ? "low" : p.face >= .1 ? "mid" : "high", w: p.face >= .25 ? "−2" : p.face >= .1 ? "−1" : "0" },
      { label: "组内相似度", value: p.score >= 80 ? "0.91" : p.score >= 50 ? "0.62" : "0.28", cls: p.score >= 80 ? "high" : p.score >= 50 ? "mid" : "low", w: p.score >= 80 ? "相符" : "偏离" },
    ];
    viewerReasons.innerHTML = reasons.map((r) => `<div class="reason ${r.cls}"><span>${r.label}</span><b>${r.value} <i>${r.w}</i></b></div>`).join("");
    openOverlay(viewerLayer);
  }

  function closeViewer() {
    closeOverlay(viewerLayer);
    state.currentPhoto = null;
  }
  $("#viewerClose").addEventListener("click", closeViewer);
  $(".viewer-backdrop").addEventListener("click", closeViewer);
  $("#viewerReject").addEventListener("click", () => {
    if (!state.currentPhoto) return;
    toggleSelect(state.currentPhoto.id, false);
    const name = state.currentPhoto.title;
    closeViewer();
    toast(`「${name}」已设为不归档`);
  });
  $("#viewerKeep").addEventListener("click", () => {
    if (!state.currentPhoto) return;
    toggleSelect(state.currentPhoto.id, true);
    closeViewer();
    toast("已保留这张，归档时复制");
  });

  /* ---------------- navigation / flow / theme / toast ---------------- */
  const tabbar = $("#tabbar");
  const body = document.body;
  const autoBtn = $("#autoDemo");
  const flowChrome = $("#flowChrome");
  const flowCount = $("#flowCount");
  let scanRun = 0;

  const isTab = (name) => TABS.includes(name);
  const isFlow = (name) => FLOW_STEPS.includes(name);

  function updateRailActive() {
    const flowActive = isFlow(state.screen);
    $$(".flow [data-nav]").forEach((b) => {
      b.classList.toggle("active", !flowActive && b.dataset.nav === state.screen);
    });
    $$("[data-demo-step]").forEach((b) => {
      b.classList.toggle("active", flowActive && b.dataset.demoStep === state.screen);
    });
  }

  function updateFlowChrome(step) {
    if (!flowChrome) return;
    const idx = FLOW_STEPS.indexOf(step);
    flowChrome.setAttribute("aria-hidden", "false");
    flowCount.textContent = `${idx + 1} / 3`;
    $$("[data-flow-label]", flowChrome).forEach((el, i) => {
      el.classList.toggle("on", i === idx);
      el.classList.toggle("done", i < idx);
    });
  }

  function showScreen(name, dir = 1) {
    if (isTab(name) && state.flowStep) endFlow(name);
    const prev = $(`.screen[data-screen="${state.screen}"]`);
    const next = $(`.screen[data-screen="${name}"]`);
    if (!next) return;
    const returnFocus = prev?.contains(document.activeElement) && isTab(name);
    if (prev && prev !== next) {
      prev.classList.remove("is-active");
      prev.classList.add("is-leaving");
      prev.inert = true;
      setTimeout(() => prev.classList.remove("is-leaving"), 280);
    }
    next.style.setProperty("--dir", dir);
    next.classList.remove("is-leaving");
    next.classList.add("is-active");
    next.inert = false;
    next.scrollTop = 0;
    state.screen = name;
    body.dataset.screen = name;
    updateBatch();
    const activeTab = isTab(name) ? name : (state.flowOrigin || "courses");
    tabbar.querySelectorAll("[data-nav]").forEach((b) => b.classList.toggle("active", b.dataset.nav === activeTab));
    updateRailActive();
    if (name === "confirm") {
      renderPhotos();
      requestAnimationFrame(moveSeg);
    }
    if (name === "result") {
      renderArchive();
      requestAnimationFrame(celebrateArchive);
    }
    if (name === "library") renderLibrary();
    if (name === "courses") updateHome();
    if (returnFocus) tabbar.querySelector(`[data-nav="${name}"]`)?.focus({ preventScroll: true });
  }

  function endFlow(target = state.flowOrigin || "courses") {
    const returnTo = tabbar.querySelector(`[data-nav="${target}"]`) || tabbar.querySelector(`[data-nav="${state.flowOrigin || "courses"}"]`) || tabbar.querySelector("button");
    const active = document.activeElement;
    const returnFocus = flowChrome.contains(active) || !!active?.closest?.(".screen[data-flow-screen]");
    state.flowStep = null;
    body.removeAttribute("data-flow");
    updateBatch();
    if (returnFocus) returnTo?.focus({ preventScroll: true });
    if (flowChrome) flowChrome.setAttribute("aria-hidden", "true");
  }

  function openFlow(step) {
    if (!isFlow(step)) return;
    const moveFocus = $("#phoneScreen").contains(document.activeElement);
    if (!state.flowStep) {
      state.flowOrigin = isTab(state.screen) ? state.screen : "courses";
    }
    state.flowStep = step;
    body.dataset.flow = step;
    updateFlowChrome(step);
    showScreen(step, 1);
    if (moveFocus) requestAnimationFrame(() => $("#flowClose").focus({ preventScroll: true }));
  }

  function go(name, opts = {}) {
    if (isFlow(name)) { openFlow(name); return; }
    if (!isTab(name)) return;
    if (state.scanning) cancelScan(true);
    if (state.screen === "confirm") saveDraft();
    if (state.flowStep) endFlow(name);
    if (state.screen === name && !opts.force) { updateRailActive(); return; }
    const dir = NAV_ORDER.indexOf(name) >= NAV_ORDER.indexOf(state.screen) ? 1 : -1;
    showScreen(name, dir);
  }

  function applyTheme(name) {
    state.theme = name;
    body.dataset.theme = name;
    $$("[data-set-theme]").forEach((b) => {
      const on = b.dataset.setTheme === name;
      b.classList.toggle("active", on);
      if (b.hasAttribute("aria-pressed")) b.setAttribute("aria-pressed", String(on));
    });
    document.querySelector('meta[name="theme-color"]').setAttribute("content", name === "paper" ? "#ece5d6" : name === "mono" ? "#060606" : "#080808");
    requestAnimationFrame(moveSeg);
  }

  function setTheme(name) {
    if (name === state.theme) return;
    if (document.startViewTransition && !matchMedia("(prefers-reduced-motion: reduce)").matches) {
      document.startViewTransition(() => applyTheme(name));
    } else {
      applyTheme(name);
    }
  }

  $$("[data-set-theme]").forEach((b) => b.addEventListener("click", () => setTheme(b.dataset.setTheme)));

  document.addEventListener("click", (e) => {
    const stepBtn = e.target.closest("[data-demo-step]");
    if (stepBtn) { demoStep(stepBtn.dataset.demoStep); return; }
    const nav = e.target.closest("[data-nav]");
    if (nav) { go(nav.dataset.nav); return; }
    if (e.target.closest("[data-demo-flow]")) { autoDemo(); return; }
    if (e.target.closest("[data-start-scan], #libraryScan")) { requestScan(); return; }
    const album = e.target.closest("[data-open-album]");
    if (album) { openAlbum(album.dataset.openAlbum); return; }
    if (e.target.closest("#quickScan")) { quickScanAction(); return; }
    const goto = e.target.closest("[data-goto]");
    if (goto) { go(goto.dataset.goto); return; }
  });

  let toastTimer = null;
  function toast(message) {
    const el = $("#toast");
    $("#toastText").textContent = message;
    el.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove("show"), 2300);
  }

  function animateNumber(el, to, duration = 700) {
    if (!el) return;
    const from = Number(String(el.textContent).replace(/[^\d]/g, "")) || 0;
    if (to === from) { el.textContent = fmt(to); return; }
    const start = performance.now();
    const tick = (now) => {
      const p = Math.min(1, (now - start) / duration);
      const e = 1 - Math.pow(1 - p, 3);
      el.textContent = fmt(Math.round(from + (to - from) * e));
      if (p < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }

  /* ---------------- import sheet ---------------- */
  const importOverlay = $("#importOverlay");
  function openImport() {
    openOverlay(importOverlay);
  }
  function closeImport() {
    closeOverlay(importOverlay);
  }
  $$("#openImport, #settingsImport, [data-open-import]").forEach((b) => b.addEventListener("click", openImport));
  $$("[data-close-import]").forEach((b) => b.addEventListener("click", closeImport));
  importOverlay.addEventListener("click", (e) => { if (e.target === importOverlay) closeImport(); });
  $("#settingsPermission")?.addEventListener("click", () => toast("相册权限：全部照片 · 可访问 2,184 张"));
  $("#settingsBuffer")?.addEventListener("click", () => toast("缓冲时间：课前 10 分钟 / 课后 20 分钟"));
  $("#confirmImport").addEventListener("click", () => {
    closeImport();
    toast("已导入 4 门课程 · 生成 64 个时间段");
  });
  $("#pickFile").addEventListener("click", () => $("#fileInput").click());
  $("#fileInput").addEventListener("change", async (e) => {
    const file = e.target.files && e.target.files[0];
    if (!file) return;
    try {
      const data = JSON.parse(await file.text());
      const n = Array.isArray(data.courses) ? data.courses.length : 0;
      closeImport();
      toast(`已读取 ${file.name} · ${n} 门课程`);
    } catch {
      toast("JSON 解析失败，请检查文件内容");
    }
    e.target.value = "";
  });

  /* ---------------- candidate selections / draft / home CTA ---------------- */
  const draftOverlay = $("#draftOverlay");

  function syncSegment() {
    if (!segmented) return;
    segmented.querySelectorAll("button[data-filter]").forEach((b) => {
      const on = b.dataset.filter === state.filter;
      b.classList.toggle("active", on);
      b.setAttribute("aria-selected", String(on));
    });
    requestAnimationFrame(moveSeg);
  }

  function resetSelections() {
    PHOTOS.forEach((p) => { p.selected = p.verdict === "likely"; });
    state.filter = "likely";
    syncSegment();
    renderPhotos();
  }

  function saveDraft() {
    state.draft = {
      selected: PHOTOS.filter((p) => p.selected).map((p) => p.id),
      filter: state.filter,
    };
    updateHome();
  }

  function clearDraft() {
    state.draft = null;
    updateHome();
  }

  function openDraft() {
    if (!state.draft) return;
    $("#draftSelected").textContent = String(state.draft.selected.length);
    openOverlay(draftOverlay);
  }

  function closeDraft() {
    closeOverlay(draftOverlay);
  }

  function resumeDraft() {
    if (!state.draft) return;
    const set = new Set(state.draft.selected);
    PHOTOS.forEach((p) => { p.selected = set.has(p.id); });
    state.filter = state.draft.filter || "likely";
    syncSegment();
    closeDraft();
    openFlow("confirm");
    toast("已恢复上次勾选");
  }

  function enterScanFlow() {
    openFlow("scan");
    if (state.scanDone) return;
    setTimeout(() => {
      if (state.screen === "scan" && !state.scanning && !state.scanDone) startScan();
    }, 260);
  }

  function freshScan() {
    closeDraft();
    clearDraft();
    resetScan();
    resetSelections();
    enterScanFlow();
  }

  function requestScan() {
    if (state.draft) { openDraft(); return; }
    if (state.archivedSessions.length) { freshScan(); return; }
    enterScanFlow();
  }

  function quickScanAction() {
    const quick = $("#quickScan");
    const mode = quick ? quick.dataset.mode : "scan";
    if (state.draft) {
      if (mode === "resume" || mode === "confirm") resumeDraft();
      else openDraft();
      return;
    }
    if (mode === "scanning") { openFlow("scan"); return; }
    if (state.archivedSessions.length) { freshScan(); return; }
    enterScanFlow();
  }

  function demoStep(step) {
    if (!isFlow(step)) return;
    if (state.screen === "confirm" && step !== "confirm") saveDraft();
    if (state.scanning && step !== "scan") cancelScan(true);
    closeAllOverlays();
    if (step === "scan") {
      openFlow("scan");
      if (!state.scanDone && !state.scanning) {
        setTimeout(() => {
          if (state.screen === "scan" && !state.scanning && !state.scanDone) startScan({ duration: 3000 });
        }, 240);
      }
      return;
    }
    if (step === "confirm") {
      if (!PHOTOS.some((p) => p.selected)) resetSelections();
      openFlow("confirm");
      return;
    }
    if (!PHOTOS.some((p) => p.selected) && !state.lastArchive) resetSelections();
    openFlow("result");
  }

  /* ---------------- scan ---------------- */
  const scanner = $("#scanner");
  const scanRing = $("#scanRing");
  const scanNum = $("#scanNum");
  const scanLabel = $("#scanLabel");
  const scanFound = $("#scanFound");
  const scanBtn = $("#scanBtn");
  const scanBtnLabel = $("#scanBtnLabel");
  const scanAgain = $("#scanAgain");
  const RING = 653.45;
  const SCAN_TOTAL = 2184;

  function setStep(index, stateName) {
    const li = $(`.scan-step[data-step="${index}"]`);
    if (!li) return;
    li.dataset.state = stateName;
    const ico = $(".step-ico", li);
    const label = $(".step-state", li);
    if (stateName === "done") {
      ico.innerHTML = '<svg class="icon"><use href="#i-check"/></svg>';
      label.textContent = "完成";
    } else {
      ico.textContent = String(index + 1);
      label.textContent = stateName === "active" ? "进行中" : "待机";
    }
  }

  function resetSteps() { [0, 1, 2, 3].forEach((i) => setStep(i, "pending")); }

  function updateScan(p) {
    scanRing.style.strokeDashoffset = String(RING * (1 - p));
    scanNum.textContent = fmt(Math.round(SCAN_TOTAL * p));
    const bounds = [0, .2, .46, .73, 1];
    [0, 1, 2, 3].forEach((i) => {
      if (p >= bounds[i + 1]) setStep(i, "done");
      else if (p >= bounds[i]) setStep(i, "active");
      else setStep(i, "pending");
    });
  }

  function resetScan() {
    scanRun++;
    state.scanning = false;
    state.scanDone = false;
    scanner.classList.remove("is-scanning", "done");
    scanRing.style.strokeDashoffset = String(RING);
    scanNum.textContent = "0";
    scanLabel.textContent = "张已检查";
    scanFound.classList.remove("show");
    scanAgain.classList.add("hidden");
    resetSteps();
    $$("#scanFound [data-found]").forEach((el) => { el.textContent = "0"; });
    scanBtn.disabled = false;
    scanBtn.classList.remove("is-busy");
    scanBtn.innerHTML = '<svg class="icon"><use href="#i-scan"/></svg><span id="scanBtnLabel">开始扫描</span>';
  }

  function cancelScan(silent = false) {
    if (!state.scanning) return;
    scanRun++;
    state.scanning = false;
    scanner.classList.remove("is-scanning");
    scanBtn.disabled = false;
    scanBtn.classList.remove("is-busy");
    scanBtn.innerHTML = '<svg class="icon"><use href="#i-scan"/></svg><span id="scanBtnLabel">开始扫描</span>';
    scanNum.textContent = "0";
    scanRing.style.strokeDashoffset = String(RING);
    scanLabel.textContent = "张已检查";
    resetSteps();
    updateHome();
    if (!silent) toast("扫描已暂停");
  }

  function finishScan(resolve) {
    state.scanning = false;
    state.scanDone = true;
    scanner.classList.remove("is-scanning");
    scanner.classList.add("done");
    scanRing.style.strokeDashoffset = "0";
    scanNum.textContent = fmt(SCAN_TOTAL);
    scanLabel.textContent = "张已检查";
    [0, 1, 2, 3].forEach((i) => setStep(i, "done"));
    scanBtn.disabled = false;
    scanBtn.classList.remove("is-busy");
    scanBtn.innerHTML = '<svg class="icon"><use href="#i-grid"/></svg><span id="scanBtnLabel">查看 34 张结果</span>';
    scanAgain.classList.remove("hidden");
    scanFound.classList.add("show");
    $$("#scanFound [data-found]").forEach((el, i) => {
      const target = [18, 6, 3, 7][i];
      animateNumber(el, target, 620 + i * 80);
    });
    saveDraft();
    updateHome();
    toast("扫描完成 · 34 张待确认");
    if (resolve) resolve();
  }

  function startScan({ duration = 4600 } = {}) {
    if (state.scanning) return Promise.resolve();
    if (!state.draft) resetSelections();
    const run = ++scanRun;
    state.scanning = true;
    state.scanDone = false;
    scanner.classList.add("is-scanning");
    scanner.classList.remove("done");
    scanFound.classList.remove("show");
    scanAgain.classList.add("hidden");
    scanBtn.disabled = true;
    scanBtn.classList.add("is-busy");
    scanBtn.innerHTML = '<svg class="icon"><use href="#i-refresh"/></svg><span id="scanBtnLabel">正在扫描…</span>';
    $$("#scanFound [data-found]").forEach((el) => { el.textContent = "0"; });
    resetSteps();
    updateHome();
    return new Promise((resolve) => {
      const start = performance.now();
      const tick = (now) => {
        if (run !== scanRun) { resolve(); return; }
        const raw = Math.min(1, (now - start) / duration);
        const eased = raw < .5 ? 2 * raw * raw : 1 - Math.pow(-2 * raw + 2, 2) / 2;
        updateScan(eased);
        if (raw < 1) requestAnimationFrame(tick);
        else finishScan(resolve);
      };
      requestAnimationFrame(tick);
    });
  }

  scanBtn.addEventListener("click", () => {
    if (state.scanning) return;
    if (state.scanDone) openFlow("confirm");
    else startScan();
  });
  scanAgain.addEventListener("click", () => {
    clearDraft();
    resetScan();
    startScan({ duration: 3800 });
  });

  function fillScanFloats() {
    const picks = ["p03", "p01", "p12"];
    picks.forEach((id, i) => {
      const p = PHOTOS.find((x) => x.id === id);
      const el = $(`#scanFloat${i + 1}`);
      if (p && el) el.innerHTML = photoSVG(p);
    });
  }

  /* ---------------- archive / library ---------------- */
  const BASE_TOTAL = ALBUMS.reduce((sum, a) => sum + a.count, 0);

  function archivedTotals() {
    const map = {};
    ALBUMS.forEach((a) => { map[a.name] = a.count; });
    state.archivedSessions.forEach((s) => {
      Object.entries(s.albums).forEach(([course, n]) => { map[course] = (map[course] || 0) + n; });
    });
    return map;
  }

  function archivedSum() {
    return Object.values(archivedTotals()).reduce((a, b) => a + b, 0);
  }

  function timeMinutes(value) {
    const [hours, minutes] = value.split(":").map(Number);
    return hours * 60 + minutes;
  }

  function getTodayClassView() {
    const now = timeMinutes(DEMO_TIME);
    const schedule = CLASS_SCHEDULE[3] || [];
    const parts = [
      schedule.filter((item) => timeMinutes(item.start) < 720),
      schedule.filter((item) => timeMinutes(item.start) >= 720),
    ];
    const startPart = now < 720 ? 0 : 1;

    for (let i = startPart; i < parts.length; i++) {
      const items = parts[i];
      if (!items.some((item) => timeMinutes(item.end) > now)) continue;
      return {
        dayOffset: 0,
        weekday: 3,
        items,
        preview: i > startPart || items.every((item) => timeMinutes(item.start) > now),
      };
    }

    for (let offset = 1; offset <= 7; offset++) {
      const weekday = ((3 - 1 + offset) % 7) + 1;
      const items = CLASS_SCHEDULE[weekday] || [];
      if (items.length) return { dayOffset: offset, weekday, items, preview: true };
    }

    return { dayOffset: 0, weekday: 3, items: [], preview: false };
  }

  function renderTodayClasses() {
    const view = getTodayClassView();
    const now = timeMinutes(DEMO_TIME);
    const title = $("#todayTitle");
    const meta = $("#todayMeta");
    const list = $("#todayClasses");
    const statusTime = $(".statusbar .time");
    if (!title || !meta || !list) return;

    statusTime.textContent = DEMO_TIME;
    title.textContent = view.dayOffset
      ? `${view.dayOffset === 1 ? "明天" : "后续"} · ${WEEKDAY_LABEL[view.weekday]}`
      : `今天 · ${WEEKDAY_LABEL[view.weekday]}`;
    meta.textContent = view.preview ? "课程预告" : `${view.items.length} 节课`;

    list.innerHTML = view.items.map((item) => {
      const phase = now < timeMinutes(item.start) ? "upcoming" : now < timeMinutes(item.end) ? "current" : "done";
      const isPreview = view.preview || phase === "upcoming";
      const tag = phase === "current" && !view.preview
        ? '<span class="now-tag">进行中</span>'
        : isPreview ? '<span class="preview-tag">预告</span>' : "";
      let status = "";
      let action = "";

      if (isPreview) {
        status = `<span class="status wait"><span class="dot"></span>${esc(item.start)} 开始</span>`;
      } else if (item.archived != null) {
        status = `<span class="status done"><span class="dot"></span>已归档 ${fmt(item.archived)} 张</span>`;
        action = `<button type="button" class="ghost-link" data-open-album="${esc(item.course)}">查看<svg class="icon"><use href="#i-chevron"/></svg></button>`;
      } else {
        status = `<span class="status wait"><span class="dot"></span>待扫描 · ${fmt(item.candidates || 0)} 张候选</span>`;
        action = '<button type="button" class="ghost-link" data-start-scan>去扫描<svg class="icon"><use href="#i-chevron"/></svg></button>';
      }

      return `<div class="class-card ${isPreview ? "is-preview" : ""}" style="--c:${item.color}">
        <div class="class-time"><b>${esc(item.start)}</b>${esc(item.end)}</div>
        <div class="class-main">
          <h3>${esc(item.course)}${tag}</h3>
          <div class="class-meta">
            <span><svg class="icon"><use href="#i-clock"/></svg>${esc(item.session)}</span>
            <span><svg class="icon"><use href="#i-pin"/></svg>${esc(item.room)}</span>
          </div>
          <div class="class-foot">${status}${action}</div>
        </div>
      </div>`;
    }).join("") || '<div class="empty-state">今天没有课程</div>';
  }

  function updateHome() {
    renderTodayClasses();
    const total = archivedSum();

    const quick = $("#quickScan");
    if (quick) {
      const title = $("#quickTitle");
      const sub = $("#quickSub");
      const action = $("#quickAction");
      if (state.scanning) {
        quick.dataset.mode = "scanning";
        title.textContent = "正在扫描本周照片…";
        sub.textContent = "读取拍摄时间 · 匹配 64 个时间段";
        action.textContent = "查看进度";
      } else if (state.draft) {
        quick.dataset.mode = "resume";
        title.textContent = `${state.draft.selected.length} 张已勾选，等待确认`;
        sub.textContent = "上次挑到一半 · 选择已经保存";
        action.textContent = "继续确认";
      } else if (state.archivedSessions.length) {
        quick.dataset.mode = "rescan";
        title.textContent = "本周照片已整理完";
        sub.textContent = `本机已归档 ${fmt(total)} 张 · 下次课后再扫`;
        action.textContent = "再扫一遍";
      } else {
        quick.dataset.mode = "scan";
        title.textContent = "34 张照片等待确认";
        sub.textContent = "本周 5 次课 · 预计 40 秒扫完";
        action.textContent = "去扫描";
      }
    }

    const totals = archivedTotals();
    $$(".course-row[data-open-album]").forEach((row) => {
      const num = $(".course-num", row);
      const course = row.dataset.openAlbum;
      if (num && totals[course]) num.textContent = `${fmt(totals[course])} 张`;
    });

    const storage = $("#settingsStorage");
    if (storage) storage.textContent = `${(1.2 + Math.max(0, total - BASE_TOTAL) * .005).toFixed(1)} GB`;
  }

  function buildRecord(selected) {
    const albums = {};
    const files = [];
    const counters = {};
    selected.forEach((p) => {
      const g = GROUPS[p.group];
      albums[g.course] = (albums[g.course] || 0) + 1;
      counters[g.course] = (counters[g.course] || 0) + 1;
      const num = String(counters[g.course]).padStart(3, "0");
      files.push({
        course: g.course,
        name: `W${g.week}_${g.mmdd}_${num}_IMG_${6100 + Number(p.id.slice(1))}.jpg`,
        time: p.time,
      });
    });
    return { count: selected.length, albums, files, date: "2026-11-25 · 10:24" };
  }

  function archiveRecord() {
    if (state.lastArchive) return state.lastArchive;
    return buildRecord(PHOTOS.filter((p) => p.selected));
  }

  function renderArchive() {
    const rec = archiveRecord();
    const names = Object.keys(rec.albums);
    $("#archiveSub").textContent = rec.count
      ? `${rec.count} 张照片已复制到 ${names.length} 个课程相册`
      : "还没有勾选照片，回到确认页挑几张";
    $("#archiveDate").textContent = rec.date || "2026-11-25 · 10:24";
    animateNumber($("#archiveCount"), rec.count, 760);
    animateNumber($("#archiveAlbums"), names.length, 760);
    $("#archiveOriginals").textContent = "0";

    const folderHTML = names.map((course, idx) => {
      const list = rec.files.filter((f) => f.course === course);
      const rows = list.slice(0, 4).map((f) => `<div class="file-row"><span>${esc(f.name)}</span><i>${esc(f.time)}</i></div>`).join("");
      const moreCount = rec.albums[course] - list.slice(0, 4).length;
      const more = moreCount > 0 ? `<div class="file-more">… 另有 ${moreCount} 张</div>` : "";
      return `<div class="folder-card ${idx === 0 ? "open" : ""}" style="--d:${idx * 80}ms">
        <button type="button" class="folder-top">
          <span class="folder-ico"><svg class="icon"><use href="#i-folder-check"/></svg></span>
          <div class="folder-info"><b>${esc(course)}</b><small>Pictures/课程归档/${esc(course)}/</small></div>
          <span class="folder-count">${rec.albums[course]} 张</span>
          <svg class="icon chev"><use href="#i-chevron"/></svg>
        </button>
        <div class="folder-files">${rows}${more}</div>
      </div>`;
    }).join("");
    $("#archiveFolders").innerHTML = folderHTML || `<div class="empty-state">暂无归档内容</div>`;
  }

  $("#archiveFolders").addEventListener("click", (e) => {
    const top = e.target.closest(".folder-top");
    if (top) top.closest(".folder-card").classList.toggle("open");
  });

  function burstConfetti() {
    const layer = $("#confetti");
    if (!layer) return;
    layer.innerHTML = "";
    const colors = getComputedStyle(body).getPropertyValue("--confetti").split(",").map((s) => s.trim()).filter(Boolean);
    for (let i = 0; i < 36; i++) {
      const s = document.createElement("i");
      s.style.left = `${8 + Math.random() * 84}%`;
      s.style.background = colors[i % colors.length];
      s.style.setProperty("--x", `${Math.random() * 150 - 75}px`);
      s.style.setProperty("--r", `${Math.random() * 900 - 450}deg`);
      s.style.animationDelay = `${Math.random() * .25}s`;
      s.style.animationDuration = `${1.4 + Math.random() * .9}s`;
      s.style.width = `${4 + Math.random() * 4}px`;
      s.style.height = `${6 + Math.random() * 6}px`;
      layer.appendChild(s);
    }
    setTimeout(() => { layer.innerHTML = ""; }, 3000);
  }

  function celebrateArchive() {
    const hero = $("#archiveHero");
    if (!hero) return;
    hero.classList.remove("celebrate");
    void hero.offsetWidth;
    hero.classList.add("celebrate");
    burstConfetti();
  }

  /* ---------------- archive library ---------------- */
  function renderLibrary() {
    const totals = archivedTotals();
    $("#libCount").textContent = fmt(archivedSum());
    $("#libNew").textContent = state.lastArchive ? fmt(state.lastArchive.count) : "0";
    const addedMap = state.lastArchive ? state.lastArchive.albums : {};
    $("#albumGrid").innerHTML = ALBUMS.map((a, i) => {
      const total = totals[a.name] || a.count;
      const added = addedMap[a.name] || 0;
      const picks = a.covers.map((id) => PHOTOS.find((p) => p.id === id)).filter(Boolean);
      const tiles = [0, 1, 2].map((t) => {
        const p = picks[t % Math.max(1, picks.length)];
        return `<span class="ac-tile t${t + 1}">${p ? photoSVG(p) : ""}</span>`;
      }).join("");
      return `<button type="button" class="album-card" data-open-album="${esc(a.name)}" style="--d:${i * 70}ms">
        <span class="album-cover">${tiles}</span>
        <span class="album-body">
          <b>${esc(a.name)}</b>
          <small>${fmt(total)} 张 · ${esc(a.meta.split(" · ")[0])}</small>
          <small class="${added ? "album-add" : ""}">${added ? `本次新增 +${added} 张` : "已同步系统相册"}</small>
        </span>
      </button>`;
    }).join("");
  }

  /* ---------------- course album sheet ---------------- */
  const albumOverlay = $("#albumOverlay");

  function openAlbum(course) {
    const album = ALBUMS.find((a) => a.name === course);
    if (!album) return;
    const total = archivedTotals()[album.name] || album.count;
    $("#albumTitle").textContent = album.name;
    $("#albumMeta").textContent = `Pictures/课程归档/${album.name}/ · ${fmt(total)} 张`;
    $("#albumPath").textContent = `系统相册里会显示为「${album.name}」`;
    const pool = PHOTOS.filter((p) => GROUPS[p.group].course === album.name);
    const fallback = album.covers.map((id) => PHOTOS.find((p) => p.id === id)).filter(Boolean);
    const source = pool.length ? pool : fallback;
    const cards = [];
    for (let i = 0; i < 8; i++) {
      const p = source[i % Math.max(1, source.length)];
      if (!p) continue;
      const g = GROUPS[p.group];
      const num = String(i + 1).padStart(3, "0");
      const file = `W${g.week}_${g.mmdd}_${num}_IMG_${6100 + Number(p.id.slice(1))}.jpg`;
      cards.push(`<div class="album-photo">${photoSVG(p)}<span>${esc(file)}</span></div>`);
    }
    $("#albumPhotos").innerHTML = cards.join("");
    openOverlay(albumOverlay);
  }

  function closeAlbum() {
    closeOverlay(albumOverlay);
  }

  $$("[data-close-album]").forEach((b) => b.addEventListener("click", closeAlbum));
  albumOverlay.addEventListener("click", (e) => { if (e.target === albumOverlay) closeAlbum(); });
  $("#albumOpenSystem")?.addEventListener("click", () => toast("已请求系统相册打开该课程相簿"));
  $("#archiveView")?.addEventListener("click", () => go("library"));
  $("#archiveHome")?.addEventListener("click", () => go("courses"));

  /* ---------------- commit / flow exit ---------------- */
  function commitArchive() {
    const selected = PHOTOS.filter((p) => p.selected);
    if (!selected.length) {
      toast("还没有勾选照片，先选几张");
      return;
    }
    const rec = buildRecord(selected);
    state.lastArchive = rec;
    state.archivedSessions.push({ count: rec.count, albums: { ...rec.albums }, date: rec.date });
    state.draft = null;
    state.scanDone = false;
    PHOTOS.forEach((p) => { p.selected = false; });
    resetScan();
    updateHome();
    renderLibrary();
    openFlow("result");
    toast(`已复制 ${rec.count} 张到 ${Object.keys(rec.albums).length} 个课程相册`);
  }

  $("#archiveBtn").addEventListener("click", commitArchive);

  $("#flowClose")?.addEventListener("click", () => {
    const was = state.screen;
    const hadDraft = was === "confirm" || (was === "scan" && state.scanDone);
    if (hadDraft) saveDraft();
    if (was === "scan" && state.scanning) cancelScan(true);
    go(state.flowOrigin || "courses");
    if (hadDraft) toast("已保存勾选，可在首页继续确认");
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Tab") {
      const overlay = $(".overlay.open, .viewer-layer.open");
      if (!overlay) return;
      const focusable = $$("button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])", overlay)
        .filter((el) => el.getClientRects().length > 0);
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
      return;
    }
    if (e.key !== "Escape") return;
    if (viewerLayer.classList.contains("open")) { closeViewer(); return; }
    const openOverlay = $(".overlay.open");
    if (openOverlay === importOverlay) { closeImport(); return; }
    if (openOverlay === draftOverlay) { closeDraft(); return; }
    if (openOverlay === albumOverlay) { closeAlbum(); return; }
    if (state.flowStep) { $("#flowClose").click(); return; }
    if (state.screen === "library") go("courses");
  });

  $$("[data-close-draft]").forEach((b) => b.addEventListener("click", closeDraft));
  draftOverlay.addEventListener("click", (e) => { if (e.target === draftOverlay) closeDraft(); });
  $("#draftResume")?.addEventListener("click", resumeDraft);
  $("#draftRestart")?.addEventListener("click", freshScan);

  /* ---------------- auto demo / reset ---------------- */
  function closeAllOverlays() {
    $$(".overlay.open, .viewer-layer.open").forEach(closeOverlay);
    state.currentPhoto = null;
  }

  function resetDemoState() {
    state.archivedSessions = [];
    state.lastArchive = null;
    state.draft = null;
    state.scanDone = false;
    state.scanning = false;
    state.autoRunning = false;
    state.flowStep = null;
    state.flowOrigin = "courses";
    body.removeAttribute("data-flow");
    if (flowChrome) flowChrome.setAttribute("aria-hidden", "true");
    closeAllOverlays();
    closeDraft();
    closeAlbum();
    resetScan();
    resetSelections();
    updateHome();
    renderLibrary();
  }

  async function autoDemo() {
    if (state.autoRunning || state.scanning) return;
    autoBtn.classList.add("is-busy");
    autoBtn.innerHTML = '<svg class="icon"><use href="#i-refresh"/></svg><span>演示中…</span>';
    resetDemoState();
    state.autoRunning = true;
    go("courses", { force: true });
    await wait(620);
    openFlow("scan");
    await wait(360);
    await startScan({ duration: 2400 });
    await wait(650);
    openFlow("confirm");
    await wait(1500);
    commitArchive();
    await wait(2600);
    go("library");
    await wait(1600);
    state.autoRunning = false;
    autoBtn.classList.remove("is-busy");
    autoBtn.innerHTML = '<svg class="icon"><use href="#i-play"/></svg><span>自动演示</span>';
    toast("演示结束 · 归档库已更新为 228 张");
  }

  autoBtn.addEventListener("click", autoDemo);
  $("#resetDemo").addEventListener("click", () => {
    if (state.autoRunning || state.scanning) return;
    resetDemoState();
    go("courses", { force: true });
    toast("演示已重置");
  });

  /* ---------------- fit & init ---------------- */
  function fitPhone() {
    const narrow = innerWidth <= 1000;
    const availH = innerHeight - (narrow ? 112 : 108);
    const availW = narrow ? Math.min(innerWidth - 22, 430) : 430;
    const scale = Math.max(.5, Math.min(1, availH / 864, availW / 410));
    document.documentElement.style.setProperty("--pscale", scale.toFixed(4));
    const device = $("#device");
    device.style.width = `${410 * scale}px`;
    device.style.height = `${864 * scale}px`;
    requestAnimationFrame(moveSeg);
  }

  function init() {
    fillScanFloats();
    renderPhotos();
    renderLibrary();
    updateHome();
    $$(".screen").forEach((el) => { el.inert = !el.classList.contains("is-active"); });
    moveSeg();
    fitPhone();
    window.addEventListener("resize", fitPhone);
    setTimeout(fitPhone, 120);
    setTimeout(moveSeg, 220);
    document.querySelectorAll(".screen.is-active").forEach((el) => el.classList.remove("is-leaving"));
  }

  init();
})();

