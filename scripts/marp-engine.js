import { Marp } from '@marp-team/marp-core';
import hljs from 'highlight.js';
import domainLang from './domain-lang.js';

export default (opts) => {
  const marp = new Marp(opts);

  // ドメイン記述ミニ言語を登録
  if (!hljs.getLanguage('domain')) {
    hljs.registerLanguage('domain', domainLang);
  }

  // カスタムハイライター設定
  marp.highlighter = (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value;
      } catch (e) {
        console.error(e);
      }
    }

    return hljs.highlightAuto(code).value;
  };

  return marp;
};
