// erd-build.py 가 만든 HTML 을 헤드리스 크롬으로 렌더링해 PNG 로 저장한다. 사용법은 erd-build.py 참고.
const path = require("path");
const puppeteer = require("puppeteer");
(async () => {
  const [, , input, output] = process.argv;
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.setViewport({ width: 3000, height: 2000, deviceScaleFactor: 2 });
  page.on("pageerror", e => { console.error("PAGE ERROR", e.message); process.exitCode = 1; });
  await page.goto("file://" + path.resolve(input));
  await page.waitForSelector("body[data-ready='1']", { timeout: 5000 });
  const canvas = await page.$("#canvas");
  await canvas.screenshot({ path: output });
  await browser.close();
})();
