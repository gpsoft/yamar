# Yamar

## 機能

- Yamapの活動日記をスクレイピングして、アーカイブする
- `<USERID>.edn` ...DB
- `<USERID>.html` ...一覧ページ
- `<USERID>/<ACTIVITYID>.jpg` ...各活動日記のカバー画像

## アイディア

- 最終更新日時を元に、差分のみをDL
- 日付、タイトル、カバー画像、所要時間、距離、獲得標高、写真枚数、日帰りフラグ
- 開始/終了時刻、活動詳細、山リスト、動画有無フラグ、動画URL、通過ランドマークリスト
- slurpして、ednにためて、html出力

## 開発

```
$ clj -M:dev
$ vim src/clj/senju/core.clj
  :Connect 5876 src/clj

$ clj -M -m yamar.core
```

### Release

```
$ clj -T:build clean
$ clj -T:build uber
```
