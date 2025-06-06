# Yamar

## 機能

- Yamapの活動日記をスクレイピングして、アーカイブする
- `<USER-ID>.edn` ...DB
- `<USER-ID>.html` ...一覧ページ
- `<USER-ID>/<ACTIVITY-ID>.jpg` ...各活動日記のカバー画像

## メモ

- 一覧ページの実装が変わり、JSを大量に使うようになった
  - そのため、GETしたhtmlの代わりに、ブラウザで保存したhtmlをスクレイピングする形式に変更
  - ちょっと面倒になった

## 開発

```
$ clj -M:dev
$ vim src/yamar/core.clj
  :Connect 5876 src

$ clj -M -m yamar.core
```

## リリース

```
$ clj -T:build clean
$ clj -T:build uber
```

## 使い方

REPLで、

```
user=> (dev)
yamar.core=> (go! 1764261 "docs/" false)
yamar.core=> (go! 1764261 "docs/" true)
```

↓これは、過去の話し。

```
Usage: java -jar yamar.jar [OPTIONS] USERID

Options:
  -d, --destination DIR  docs/  Destination directory
  -D, --details          false  Archive in details
  -h, --help             false  Show usage
```

## サンプル

[サンプルページ](https://gpsoft.github.io/yamar/1764261.html)
