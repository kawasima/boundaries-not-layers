# Boundaries not layers

## Examples

`examples/` に、同じ「カート」機能を違う設計で実装した5プロジェクトを置いている。前半3つは、カートの不変条件「合計数量が上限を超えない」を守るときのトリレンマを示す。性能（全アイテムをロードしない）・純粋性（ドメインがリポジトリに触れない）・完全性（不変条件をドメイン自身が判断する）は同時に満たしにくく、どれも2つを取って1つを諦める。

- `performance-completeness`（純粋性を諦める）: [`CartForUpdate.add`](examples/performance-completeness/src/main/java/com/example/cart/domain/CartForUpdate.java#L29-L34) が内側からリポジトリへ委譲。完全＋高速だが、ドメインが更新中にI/O実行する。集計・価格照会・保存を自分で呼ぶ [`CheckoutService`](examples/performance-completeness/src/main/java/com/example/cart/domain/CheckoutService.java#L41-L55) 。
- `performance-purity`（完全性を諦める）: [`AddItemToCartUseCase`](examples/performance-purity/src/main/java/com/example/cart/usecase/AddItemToCartUseCase.java#L41-L45) が `getItemCount() + quantity > UPPER_BOUND` を自前で判定する。純粋＋高速だが、不変条件がドメインの外にある。
- `purity-completeness`（性能を諦める）: [`Cart`](examples/purity-completeness/src/main/java/com/example/cart/domain/Cart.java#L22-L49) が全 `CartItem` を持ち自分で合計判定。純粋＋完全だが、追加のたび全ロードする。ユースケースと [`AddItemToCartService`](examples/purity-completeness/src/main/java/com/example/cart/domain/AddItemToCartService.java#L27) に同じ `requirePositive` が二重にある。

どれを選んでも微妙。原因は「純粋性」をレイヤーの性質、ドメイン層がI/Oを実行していいかどうかだけで捉えているため。

純粋性はレイヤーではなく単一責務の振る舞いの性質と考える。操作を純粋な振る舞いと gateway（I/O）に分解して合成し、ドメインが持つ状態も境界の decode で不変条件に要る分（合計数量）だけに絞る。

- `raoh`: `net.unit8.raoh` で境界の入力を型へ decode する。
  - [`Cart.add`](examples/raoh/src/main/java/com/example/cart/domain/Cart.java#L25-L30) はリポジトリを受け取らない純関数。`Cart` は合計数量だけ持つので、全ロードせず（性能）・上限を自分で判断でき（完全性）・`add` は純粋（純粋性）。
  - [`AddItemToCart`](examples/raoh/src/main/java/com/example/cart/domain/AddItemToCart.java#L29-L39) が純粋な振る舞い（`Cart.add` / `Product.ensureOnSale`）と gateway（`products.load` / `carts.addItem`）を `flatMap` で合成する。
  - [`PriceCart`](examples/raoh/src/main/java/com/example/cart/domain/PriceCart.java#L26-L33) は注文（`PlaceOrder`）と見積（`IssueQuote`）が共有する価格付けの振る舞い。
  - [`Orderer`](examples/raoh/src/main/java/com/example/cart/domain/Orderer.java#L11-L20) は sealed（`Individual` / `Corporation`）で、「法人番号の無い法人」を構築できない。
  - 失敗は例外でなく `Result`（`Ok` / `Err`）で返す。

もう一歩進めて、ドメインそのものを実行可能な仕様として書くのが `raoh-souther`。data・invariant・behavior を Souther DSL で書き、raoh は境界の decode を担う。

- `raoh-souther`（`raoh` の完全移植で、ドメインを Souther 生成に置き換えた版）: [`cart.sou`](examples/raoh-souther/src/main/souther/cart.sou) に値オブジェクト＋invariant・sealed な `Orderer`・純粋 behavior（`addToCart` / `priceLine` / `placeOrder` / `issueQuote`）を書くと、raoh の `Result` を返す `decoder()` / `encoder()` が導出される。
  - Souther に無い正規化・正規表現（判別子つき Orderer、法人番号13桁）は [`JsonOrdererDecoders`](examples/raoh-souther/src/main/java/com/example/cart/web/JsonOrdererDecoders.java) で raoh が受け持ち、検証済みの値を Souther の `decoder()` に渡してドメイン型を組む。これが raoh と Souther の継ぎ目。
  - DB アクセスは Souther の**注入 behavior**（`loadProduct` / `loadCart` / `saveItem` / `priceCart` / `saveOrder`、jOOQ で実装）で、合成 behavior が `raoh` 版と同じく I/O を組み込む。Controller は decode→コマンド→振る舞い呼び出し→encode の薄い層。HTTP 契約・DB 効果・テストは `raoh` 版と同一。
  - JDK 25 が要る。ビルドは `org.souther-lang:souther-maven-plugin` が `src/main/souther` を `target/classes` へコンパイルする。Souther は 0.1.0、raoh は 0.7.2 を使う。
  - 例が足りているかは `souther examples src/main/souther/cart.sou` で確かめる（現在 `adequacy: satisfied`）。`--strict` を付けると、埋まっていない指摘でビルドが落ちる。

## 使い方

```sh
npm install
npm run build   # slide.md -> slide.html を生成し、目次を注入
npm run watch   # 変更を監視して自動ビルド
npm run pdf     # slide.pdf を書き出し（登壇配布用）
npm run pptx    # slide.pptx を書き出し
```

`slide.html` をブラウザで開き、`[n]` キーでサムネイル目次を開閉できる。

## メモ

- 背景画像は `images/background.png` を CSS で相対参照する。差し替えたい場合は同名で上書きする。`slide.html` はブラウザがパスを解決するので問題ないが、Marp 自身の PDF/PPTX/画像エクスポートでは `--allow-local-files` が必要（`pdf` / `pptx` スクリプトには付与済み）。
- 明るい背景の上で本文を読ませたいスライドは、先頭に `<!-- _class: veil -->`（暗幕）または `<!-- _class: divider -->`（セクション扉・中央大見出し）を置く。実装は `themes/deck.css`。
