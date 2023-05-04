# Yamar

## 機能

- Yamapの活動日記をスクレイピングして、アーカイブする
- `<USER-ID>.edn` ...DB
- `<USER-ID>.html` ...一覧ページ
- `<USER-ID>/<ACTIVITY-ID>.jpg` ...各活動日記のカバー画像

## メモ

- JSで動的にレンダリングされる情報は取れない(カバー画像とか)
- 開始/終了時刻、活動詳細、山リスト、動画有無フラグ、動画URL、通過ランドマークリスト

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

```
Usage: java -jar yamar.jar [OPTIONS] USERID

Options:
  -d, --destination DIR  docs/  Destination directory
  -D, --details          false  Archive in details
  -h, --help             false  Show usage
```

## サンプル

[サンプルページ](https://gpsoft.github.io/yamar/1764261.html)
