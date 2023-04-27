# Yamar

## 機能

- Yamapの活動日記をスクレイピングして、アーカイブする
- `<USERID>.edn` ...DB
- `<USERID>.html` ...一覧ページ
- `<USERID>/<ACTIVITYID>.jpg` ...各活動日記のカバー画像

## アイディア

- 最終更新日時を元に、差分のみをDL
- 日付、タイトル、所要時間、距離、獲得標高、写真枚数
- 開始/終了時刻、活動詳細、山リスト、動画有無フラグ、動画URL、通過ランドマークリスト
- slurp/scrapeして、ednにためて、html出力
- JSで動的にレンダリングされる情報は取れないよ(カバー画像とか)

## 開発

```
$ clj -M:dev
$ vim src/yamar/core.clj
  :Connect 5876 src

$ clj -M -m yamar.core
```

### Release

```
$ clj -T:build clean
$ clj -T:build uber
```
