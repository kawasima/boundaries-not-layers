import { readFileSync, writeFileSync, statSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, '..');
const outputPath = resolve(projectRoot, 'slide.html');
const tocPath = resolve(__dirname, 'toc.html');

const toc = readFileSync(tocPath, 'utf-8');

function inject() {
  const html = readFileSync(outputPath, 'utf-8');
  // 既に注入済みの場合は何もしない
  if (html.includes('id="toc-nav"')) return false;
  const injected = html.replace('</body>', toc + '\n</body>');
  writeFileSync(outputPath, injected, 'utf-8');
  return true;
}

// 引数で --watch が指定されたら polling モード
const watchMode = process.argv.includes('--watch');

if (!watchMode) {
  // 一回実行で終了
  const changed = inject();
  if (changed) console.log('[inject-toc] TOC injected into slide.html');
} else {
  // polling で slide.html の変更を監視して都度注入
  let lastMtime = 0;
  const INTERVAL = 500; // ms

  console.log('[inject-toc] watching slide.html for changes...');

  setInterval(() => {
    try {
      const mtime = statSync(outputPath).mtimeMs;
      if (mtime > lastMtime) {
        lastMtime = mtime;
        const changed = inject();
        if (changed) console.log('[inject-toc] TOC injected');
      }
    } catch (_) {
      // ファイルが存在しない場合は無視
    }
  }, INTERVAL);
}
