// ドメイン記述ミニ言語のhighlight.js言語定義
export default function(hljs) {
  return {
    name: 'domain',
    case_insensitive: false,
    keywords: {
      keyword: 'data behavior List',
      built_in: 'AND OR',
      type: '文字列 整数 数値 真偽値'
    },
    contains: [
      // コメント（// で始まる行）
      hljs.COMMENT('//', '$'),

      // 文字列リテラル
      hljs.QUOTE_STRING_MODE,

      // 数値
      hljs.NUMBER_MODE,

      // 矢印演算子
      {
        className: 'operator',
        begin: '->',
        relevance: 10
      },

      // 演算子（=, ?, など）
      {
        className: 'operator',
        begin: /[=?]/
      },

      // ジェネリック型（List<...>）
      {
        className: 'type',
        begin: /List</,
        end: />/,
        contains: [
          {
            className: 'type',
            begin: /[A-Za-z_\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF]+/
          }
        ]
      },

      // data定義の型名
      {
        className: 'title.class',
        begin: /(?:data|behavior)\s+/,
        end: /\s*=/,
        excludeEnd: true,
        returnBegin: true,
        contains: [
          {
            className: 'keyword',
            begin: /^(?:data|behavior)/
          },
          {
            className: 'title.class',
            begin: /[A-Za-z_\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF][A-Za-z0-9_\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF]*/,
            relevance: 0
          }
        ]
      },

      // 型名・識別子（日本語対応）
      {
        className: 'type',
        begin: /[A-Za-z_\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF][A-Za-z0-9_\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF]*/,
        relevance: 0
      }
    ]
  };
}
