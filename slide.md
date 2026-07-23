---
marp: true
theme: deck
---

<!-- ===== 表紙 ===== -->

# 「なんとなくレイヤリング」から抜け出すための<br>本物の設計境界の作り方

kawasima / 株式会社ウルフチーフ

---

<!-- ===== Part 1: つかみ ===== -->

<!-- _class: veil -->

## なんとなくレイヤリング

HTTPを受けたらController、業務っぽい処理はService、DBに触るならRepository

- コンパイルは通るし、レビューも通る
- 「レイヤードアーキテクチャだから」で説明した気になれる
- だがレイヤーに分けたことで何がどう良くなったのかは、意外と言えない

---

<!-- _class: veil -->

## レイヤーがあるのに境界がないことが困る

Controller / Service / Repository で処理のとりあえずな置き場は決まる。
でも毎回それらをセットで変更しているような…

- API の返却形式を変えたい
- 注文者の入力ルールを変えたい
- カートに入れられる条件を変えたい

---

<!-- _class: veil -->

## 人は何のためにレイヤードアーキテクチャにするのか?

ZennとQiitaの記事を分析しそれらの主張をまとめてみました。

| 目的          | 記事でよく使われる表現             | 実際に操作している設計変数              |
| ----------- | ----------------------- | -------------------------- |
| 変更影響の局所化    | 保守性、変更容易性、変化に強い         | 変更理由が異なるコードを別の境界へ置く        |
| 理解・探索コストの削減 | 可読性、見通し、どこに何があるか分かる     | コードの配置規則を標準化する             |
| 責務混在の防止     | 関心の分離、神クラス防止            | 一つのモジュールが扱う判断・入出力・永続化を限定する |
| 依存関係の制御     | 疎結合、単方向依存               | 変更の伝播経路を制約する               |
| テスト代替可能性    | テスト容易性、モックしやすい          | 外部I/Oとの境界を作り、差し替え可能にする     |
| ドメイン知識の保護   | ドメインを中心にする、業務ロジックを漏らさない | 業務判断をUI・DB・フレームワーク表現から分離する |
| 開発時の認知負荷低減  | 実装場所に迷わない、コードを読みやすい     | チーム内の分類体系を固定する             |
| 並行作業        | チーム開発、開発効率              | 層の境界を担当境界として利用する           |
| 再利用         | 再利用性                    | 特定技術や呼び出し元への依存を減らす         |

---

<!-- _class: veil -->

## 変更影響の局所化

> レイヤーに分ければ変更が特定の層に閉じるため、変更影響が局所化する。

残念ながらレイヤーに分けただけでは、変更がレイヤーに閉じることはない。

変更を閉じ込めるには…

- 変更軸とレイヤーの境界が一致している
- 境界を越えて内部表現が漏れていない
- 呼出側が下位層の具体的構造に依存していない
- データ変換、エラー表現、トランザクション境界まで含めて変更が封じ込められている

```java
@PostMapping("/checkout")
public ResponseEntity<Order> checkout(@RequestBody CheckoutRequest request) {
    Order order = checkoutUseCase.handle(...);
    return ResponseEntity.status(CREATED).body(order);  // ドメインの Order をそのまま返す
}
```

<span class="src">[performance-purity/web/CartController.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-purity/src/main/java/com/example/cart/web/CartController.java#L43-L48)</span>

ドメインの `Order` をそのまま返すので、`Money` の内部表現（`{"amount": ...}`）も、区分つき `Orderer`（個人なら会社名が null）も、そのままAPIレスポンスのJSONになる。ドメインを直すとクライアント側の参照キーも変わる。レイヤーはあるのに、内部表現が境界を越えて漏れている。

---

<!-- _class: veil -->

## 理解・探索コストの削減

> レイヤーごとにコードの置き場所が決まるため、どこを読めばよいか分かりやすい。

局所的にはその通りだが、コードの分類容易性とシステムの理解容易性を混同している。

UseCaseクラスを探せることと、「このユースケースが何を行うのか」を理解できることは別の話。
典型的なレイヤード構成では、一つの処理を理解するために次を往復する。

```
CartController
  → CheckoutRequest / OrdererForm（@Valid）
  → CheckoutUseCase
  → CheckoutService
  → CartRepository / ProductRepository（ポート）
  → JooqCartRepository / JooqProductRepository
  → OrderRepository → JooqOrderRepository
```

ファイルの置き場所は予測できても、カートのチェックアウト1つを理解するのにこれだけのファイルを読まなきゃいけない。

---

<!-- _class: veil -->

## 責務混在の防止

> 責務を分離するために、責務ごとに層へ分ける。

「責務」という言葉が、そもそも曖昧

- 入出力方式という技術的責務
- ユースケース遂行という機能的責務
- 不変条件維持という意味的責務
- データ保存という運用的責務
- 変更理由という保守上の責務

```java
@Transactional
public void handle(AddItemToCartCommand command) {
    UUID userId = UUID.fromString(command.userId());            // 入力パース
    UUID productId = UUID.fromString(command.productId());
    int quantity = requirePositive(command.quantity());          // 入力検証
    boolean onSale = productRepository.isNowOnSale(productId);    // 業務データ取得
    Cart cart = cartRepository.loadCart(userId);                 // 永続化(読み)
    addItemToCartService.addItem(cart, productId, onSale, quantity);  // 業務判断
    cartRepository.saveCart(userId, cart);                       // 永続化(書き)
}
```

<span class="src">[purity-completeness/usecase/AddItemToCartUseCase.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/purity-completeness/src/main/java/com/example/cart/usecase/AddItemToCartUseCase.java#L35-L50)</span>

入力パース・入力検証・業務データ取得・業務判断・永続化が1メソッドに同居している。「UseCaseに置く」だけでは、これらは分かれない。

---

<!-- _class: veil -->

## 依存関係の制御

これはレイヤードアーキテクチャの目的そのものなので、その通りだが…

依存方向を揃えても、意味的結合は消えない。

```java
// Web の入力
public record AddItemToCartRequest(String userId, String productId, int quantity) {}
// アプリ層の入力 ── 形が Request と 1:1
public record AddItemToCartCommand(String userId, String productId, int quantity) {}
```

<span class="src">[performance-purity/web/AddItemToCartRequest.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-purity/src/main/java/com/example/cart/web/AddItemToCartRequest.java#L3) · [usecase/AddItemToCartCommand.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-purity/src/main/java/com/example/cart/usecase/AddItemToCartCommand.java#L3)</span>

Application層がPresentation層をimportしていなくても、`Command` の形が画面のリクエストに追随していれば意味的には結合している。
同様に、Repository InterfaceをDomain側へ置いて依存方向を逆転しても、ドメインモデルがDBのテーブル粒度や更新都合を反映していれば、意味的依存は残る。

---

<!-- _class: veil -->

## テスト代替可能性

> レイヤーに分けるとモックできるため、テストしやすくなる。

- モック可能性はレイヤーではなく代替可能な境界によって生じる。関数引数、インターフェース、高階関数、インメモリ実装など、実現方法はいくらでもある。
- モック可能性とテスト容易性は同義ではない。レイヤーごとにモックを挟むと、実装の呼出手順を検証する相互作用テストが増え、リファクタリング耐性が下がる。
- DTO, Mapper, Repository Interfaceを増やせば、テスト対象とテスト用fixtureが増える。テストしやすくなるどころか、意味のない変換テストを大量に生むことがある。

```java
// performance-completeness の CheckoutService は集計・価格照会・保存を自分で呼ぶ（不純）
// → 割引後の合計を確かめるだけでも、3つのリポジトリをモックする羽目になる
@Test void 割引後の合計() {
    when(cartReadRepository.findAllItems(USER)).thenReturn(List.of(new CartItem(COFFEE, 8)));
    when(productRepository.isNowOnSale(COFFEE)).thenReturn(true);
    when(productRepository.priceOf(COFFEE)).thenReturn(1200L);

    Order order = checkoutService.checkout(USER, orderer);

    assertEquals(8640, order.total().amount());   // 1200×8=9600、10%引きで 8640
}
```

<span class="src">[performance-completeness/domain/CheckoutService.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-completeness/src/main/java/com/example/cart/domain/CheckoutService.java#L41-L55)</span>

確かめたいのは割引後の合計という一点なのに、それが I/O と絡んでいるので、3つのモックが必要になる。

---

<!-- _class: veil -->

## ドメイン知識の保護

> ドメイン層をUIやDBから分離すれば、ドメイン知識を保護できる。

「保護」が何を守るのか、実は曖昧である

- 技術変更から保護するのか
- 不正な値から保護するのか
- 不正な操作順から保護するのか
- 外部からの任意操作から保護するのか
- ドメイン概念の歪曲から保護するのか

レイヤーのimport制約で直接守れるのは、1番目だけ。

```java
// domain 層。区分と、個人・法人 両方の項目をフラットに持つ
public record Orderer(OrdererType type, String email,
                      String name, String companyName, String corporateNumber) {}

// 法人なのに法人番号が無い、不正な注文者がそのまま作れる
new Orderer(OrdererType.CORPORATION, "a@b.com", null, null, null);
```

<span class="src">[performance-purity/domain/Orderer.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-purity/src/main/java/com/example/cart/domain/Orderer.java#L12-L18)</span>

importの向きは正しいのに、「法人なのに法人番号が無い」不正状態が通りする。
Web層のBean Validationだけでチェックしているだけでは、ドメイン層では何も保護できていない。

---

<!-- _class: veil -->

## 開発時の認知負荷低減

レイヤードアーキテクチャは、初心者にとって分類表としては使いやすい。

- HTTPならController
- 業務処理ならService
- DBならRepository

しかし、この分類の単純さは設計の単純さではない。実際には「このロジックはServiceかDomainか」「DTOはApplicationかPresentationか」「変換は誰の責務か」という境界争いが発生する。

そしてモデルを層ごとに分離すると、次の認知負荷が増える。

- 同じ概念に複数の型がある
- どの段階の型なのか判別する必要がある
- Mapperを追う必要がある
- 値の妥当性がどこで保証されたか分からない
- 各レイヤーで再検証が必要か判断できない

```java
// Web入力（Bean Validation つき）
public record OrdererForm(String type, String email, String name,
                          String companyName, String corporateNumber) {}
// Domain（区分フラット）
public record Orderer(OrdererType type, String email, String name,
                      String companyName, String corporateNumber) {}

Orderer orderer = toOrderer(request.orderer());  // フォーム → ドメインへ詰め替え
```

<span class="src">[performance-purity/web/OrdererForm.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-purity/src/main/java/com/example/cart/web/OrdererForm.java#L16) · [domain/Orderer.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-purity/src/main/java/com/example/cart/domain/Orderer.java#L12-L18) · [web/CartController.java (toOrderer)](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-purity/src/main/java/com/example/cart/web/CartController.java#L58-L61)</span>

一つの「注文者」に、Webの `OrdererForm` とドメインの `Orderer` と詰め替えの `toOrderer` がある。どちらも区分つきのフラット型で、どの項目が有効かは `type` 次第。目の前の値がどの段階で妥当になったのか、コードからは追えない。

---

<!-- _class: veil -->

## 並行作業

レイヤーごとに担当を分ければ、並行開発できる。

これはコンウェイ的な組織分割を、アーキテクチャの利点と取り違えた説明である。

Presentation担当、Service担当、Repository担当に分けると、一つのユースケースを完成させるのに複数チームの調整が要る。これは並行じゃなく、水平分業でハンドオフが増えているだけ。

```
画面チーム
  ↓ DTO合意待ち
業務チーム
  ↓ Repository API合意待ち
DBチーム
```

機能単位で縦に切れば一つのチームで完結できる変更を、レイヤー単位で横に切ることで待ち行列へ変えている。

並行開発を成立させるのは、レイヤーではなく次の条件である。

- 独立して変更・デプロイ可能な作業単位
- 安定した境界契約
- 共有データモデルへの変更競合が少ないこと
- チームが端から端まで所有できること

---

<!-- _class: veil -->

## 再利用

> 下位レイヤーを上位レイヤーの都合から切り離せば、再利用できる。

「依存されていない」ことと「再利用可能」であることを混同している。

再利用するには、少なくともこれらの条件を満たさなければならない。

- 利用文脈をまたいで意味が安定している
- 前提条件が明示されている
- データ構造が利用側に適合する
- ライフサイクルやトランザクション条件が一致する
- エラー契約が一般化されている
- 再利用側の変更要求を受けても複雑化しない

`CheckoutService` は Controller から分離され、確かに「依存されていない」。だが再利用できるとは限らない。その正しさは「注文者が検証済みであること」を前提にしているのに、その前提は型にも引数の契約にも無い。守っているのはWeb境界の Bean Validation だけだ。

```java
// Controller からは分離されている（＝依存されていない）
public Order checkout(UUID userId, Orderer orderer, ...) {
    // orderer が正しいかは確かめない。Bean Validation を通った前提で使う
    return new Order(..., orderer, ...);
}

// 別の文脈（バッチ、別のUseCase）から呼ぶと──
checkoutService.checkout(userId,
    new Orderer(OrdererType.CORPORATION, "a@b.com", null, null, null),  // 検証を通っていない不正な注文者
    ...);                                                               // 素通しして壊れた Order ができる
```

<span class="src">[performance-completeness/domain/CheckoutService.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/performance-completeness/src/main/java/com/example/cart/domain/CheckoutService.java#L41-L55)</span>

前提条件（注文者は検証済み）が型にも契約にも無く、境界の Bean Validation に暗黙に依存している。
「Controllerに依存していない」＝「再利用可能」ではない。

---

<!-- _class: divider -->

## では、どこに線を引くか

---

<!-- _class: veil -->

## 最低限ほしいのは、型で守る境界

ここまでの問題は、どれも「外から来た未確定の値」と「業務処理が前提にしたい値」が同じ形のまま流れていることから起きていた。

なので、入力を受け取る場所に線を引く

- 信頼できるデータの領域と、まだ信頼できない領域を別の型にする
- 内側では、入力形式についての検査を繰り返さない
- 「正しいか」を確かめる処理は、入口に寄せる

---

<!-- _class: veil -->

## 境界を越えるとは、型を変えること

不正な状態のものを、安全な領域に踏み入れさせない。そのために境界では、入力を検査して通すのではなく、正しい型へ変換する。変換に成功した値だけが、内側に入れる。

- validate: 値はそのまま。正しさはフラグとして得るだけで、型には残らない
- parse: 正しければドメインの型になる。以降は型が正しさの証明になる
- 不正な状態のものは、そもそも内側の型を名乗れない

![境界](images/entrance.png)

---

<!-- _class: veil -->

## decode を担うライブラリ Raoh

**Raoh** は、境界の未確定な入力を、型のついたドメイン値へ変換（decode）するための Java ライブラリ。ここまで話した parse, don't validate を、そのまま API にしている。

- 失敗は例外で飛ばさず、`Result`（`Ok` / `Err`）の値で返す
- どのフィールドで失敗したかを、パス付きで蓄積する（途中で止めない）
- record・sealed・パターンマッチが前提（Java 25）
- 入力源ごとにモジュールを足す。`raoh`（コア）に `raoh-json` / `raoh-jooq` を追加する

<span class="src">Maven Central: `net.unit8.raoh` ・ [github.com/kawasima/raoh](https://github.com/kawasima/raoh)</span>

---

<!-- _class: veil -->

## 検査(validate)ではなく変換(decode)する

`validate` は「この値は正しいか？」を検査して bool を返す。値の型は変わらないまま、内側へ流れていく。
`decode` は「正しければ、この型にする」。境界を越えた先では、もう型が正しさを保証している。

```java
public interface Decoder<I, T> {
    // 生の入力 → 型のついた値 or 構造化エラー
    Result<T> decode(I input, Path path);
}
```

<span class="src">[raoh（ライブラリ）decode/Decoder.java](https://github.com/kawasima/raoh/blob/main/raoh/src/main/java/net/unit8/raoh/decode/Decoder.java)</span>

parse, don't validate ── 検査して通すのではなく、境界で型に変える

---

<!-- _class: veil -->

## 境界での失敗は確実にハンドリングされるようにResult

decode の結果は、`Result` で返し、ハンドリングを強制する

```java
public sealed interface Result<T> permits Ok, Err {}

// 成功 ── 値を持つ
public record Ok<T>(T value)          implements Result<T> {}

// 失敗 ── エラーを持つ
public record Err<T>(Issues issues)   implements Result<T> {}
```

<span class="src">[raoh（ライブラリ）Result.java](https://github.com/kawasima/raoh/blob/main/raoh/src/main/java/net/unit8/raoh/Result.java) · [Ok.java](https://github.com/kawasima/raoh/blob/main/raoh/src/main/java/net/unit8/raoh/Ok.java) · [Err.java](https://github.com/kawasima/raoh/blob/main/raoh/src/main/java/net/unit8/raoh/Err.java)</span>

- 例外で大域脱出せず、成功／失敗を値で返す。`switch` のパターンマッチが、`Ok` と `Err` の両方に必ず向き合わせる。
- 「エラーを握り潰す」ができない。`Ok` / `Err` は record（値がそろう「AND」）、`Result` は sealed（どちらか「OR」）

---

<!-- _class: veil -->

## 注文者を record×sealed で型にする

境界で組み立てる「正しい型」を、先に用意する。注文者は個人か法人。**record**（値がそろう「AND」）と **sealed**（どれか一つ「OR」）を組み合わせると、この形を過不足なく型にできる。

```java
public sealed interface Orderer permits Orderer.Individual, Orderer.Corporation {
    // 個人 = メール AND 氏名
    record Individual(Email email, String name) implements Orderer {}

    // 法人 = メール AND 会社名 AND 法人番号
    record Corporation(Email email, String companyName,
                       String corporateNumber) implements Orderer {} 
}
```

<span class="src">[raoh/domain/Orderer.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/raoh/src/main/java/com/example/cart/domain/Orderer.java#L11-L20)</span>

`decoder` が作る `Orderer` には、「法人なのに法人番号が無い」「個人なのに会社名がある」という組み合わせが入らない。

> Brian Goetz は "Data-Oriented Programming in Java" で、record と sealed を使えば「誤った状態を表現できない（make illegal states unrepresentable）」ようにモデリングできる、と述べている。

---

<!-- _class: veil -->

## 生の入力を、その場で型に変える

```java
JsonDecoder<Orderer> orderer() {
    return discriminate("type",                    // type で個人／法人に振り分ける
        variant("individual", combine(
            field("email", string().trim().toLowerCase().email().map(Email::new)),
            field("name",  string().trim().nonBlank())
        ).map(Orderer.Individual::new)),
        variant("corporation", combine(
            field("email",           string().trim().toLowerCase().email().map(Email::new)),
            field("companyName",     string().trim().nonBlank()),
            field("corporateNumber", string().pattern("\\d{13}"))   // 法人番号は13桁
        ).map(Orderer.Corporation::new)));
}
```

<span class="src">[raoh/web/JsonOrdererDecoders.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/raoh/src/main/java/com/example/cart/web/JsonOrdererDecoders.java#L39-L41)</span>

注目は `discriminate`。`type` を見て個人／法人それぞれの decoder に振り分け、法人番号の13桁まで検証する。
ここを通過した `Orderer` は、個人なら個人・法人なら法人として完全に型付けられる。
「法人番号の無い法人」は、そもそも decode を通らない。

---

<!-- _class: veil -->

## switch で網羅して分解する

`decode` の結果（`Result`）も注文者（`Orderer`）も sealedである。

`switch` で分解すると、全ケースを書かないとコンパイルエラーになる（網羅性検査・`default` 不要）。

しかも型引数はセレクタの型から推論されるので、`Ok<Tuple2<…>>` のような入れ子を `case` ごとに書き並べなくていい。

```java
// セレクタはの型はここでは、Result<Tuple2<UserId, Orderer>>
switch (decoder.decode(json)) {
    // 型引数は推論される。ネストした record も var で分解
    // Tupleの_1(), _2()みたいな何番目かを意識した取り出しは不要
    case Ok(Tuple2(var userId, var orderer)) -> …
    case Err(var issues)                     -> …
}
```

<span class="src">[raoh/web/CartController.java (checkout)](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/raoh/src/main/java/com/example/cart/web/CartController.java#L86-L95)</span>

- 「ありうる場合は全部処理した」がコンパイラ保証になる。permits に足せば switch が赤くなる
- record パターンで分岐と同時に中身も取り出せる。型で守っても、分解は冗長にならない

---

<!-- _class: veil -->

## エラーは途中で止めず、全部ためる

入力:

```json
{ "type": "corporation", "companyName": "", "corporateNumber": "12345" }
```

結果:

```text
Err[/companyName: 空です, /corporateNumber: 13桁の数字ではありません]
```

`combine` は最初の1個で止まらず、全フィールドの失敗をパス付きで蓄積する。前半で見た「レイヤーごとの再検証」も「一方しか報告できない if 連鎖」も要らなくなる。

---

<!-- _class: veil -->

## ドメインの型には、成立条件を持たせる

```domain
data 数量 = 正の整数
behavior 追加する = カート AND 数量 -> 追加できた明細
```

```java
public record Quantity(int value) {
    public Quantity {
        if (value <= 0) throw new IllegalArgumentException("quantity must be positive");
    }
}
public record CartItem(ProductId productId, Quantity quantity) {} // 数量は必ず正
```

<span class="src">[raoh/domain/Quantity.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/raoh/src/main/java/com/example/cart/domain/Quantity.java) · [domain/CartItem.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/raoh/src/main/java/com/example/cart/domain/CartItem.java#L4)</span>

ここで守るのは「数量は正」であって、カート全体の上限ではない。後者は現在のカートに依存するので、`Cart.add` の判断として残す。型に押し込めるものと、その場で判断するものを分ける。

---

<!-- _class: veil -->

## 業務ルールも「通ったら型になる」

```java
// behavior 追加する = カート AND 数量 -> 追加できた明細
public Result<CartItem> add(ProductId productId, Quantity quantity) {
    if (!currentQuantity.canAdd(quantity, UPPER_BOUND))
        return Result.fail("cart_full", "商品数の上限に達しています");
    return Result.ok(new CartItem(productId, quantity));  // 通過して初めて構築
}
```

<span class="src">[raoh/domain/Cart.java](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/raoh/src/main/java/com/example/cart/domain/Cart.java#L25-L30)</span>

`new CartItem(...)` は成功パスにしか無い。不正な状態はそもそも構築できない。

---

<!-- _class: veil -->

## 入力の正しさと業務ルールを型で分ける

```java
return switch (ADD_ITEM.decode(body)) {
    case Ok(Tuple3(var userId, var productId, var quantity)) ->
        switch (addItemToCart.apply(userId, productId, quantity)) {
            case Ok(var item)    -> ResponseEntity.status(CREATED).build();
            case Err(var issues) -> ResponseEntity.unprocessableContent().body(errors(issues)); // 業務NG 422
        };
    case Err(var issues) -> ResponseEntity.badRequest().body(errors(issues));                    // 入力NG 400
};
```

<span class="src">[raoh/web/CartController.java (addItem)](https://github.com/kawasima/boundaries-not-layers/blob/main/examples/raoh/src/main/java/com/example/cart/web/CartController.java#L73-L84)</span>

「入力が正しくない(400)」と「入力は正しいが業務上できない(422)」が、型と `switch` で自然に分かれる。

---

<!-- _class: veil -->

## まとめ

- レイヤーは置き場をそろえるが、変更を止める境界までは作ってくれない
- 外から来た値は、decodeしてから内側へ渡す
- record・sealed・パターンマッチは、その境界を Java で書く道具になる
