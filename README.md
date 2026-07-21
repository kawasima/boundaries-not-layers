# Boundaries not layers

## Examples

`examples/` に、同じ「カート」機能を違う設計で実装した4プロジェクトを置いている。前半3つは、カートの不変条件「合計数量が上限を超えない」を守るときのトリレンマを示す。性能（全アイテムをロードしない）・純粋性（ドメインがリポジトリに触れない）・完全性（不変条件をドメイン自身が判断する）は同時に満たしにくく、どれも2つを取って1つを諦める。

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
