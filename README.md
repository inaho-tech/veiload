# Veiload

yt-dlp / FFmpegをアプリ内に内包した、黒基調のAndroidネイティブ動画ダウンローダーです。UIはComposeの描画・Foundation APIから独自実装しており、Materialコンポーネントは使用していません。

## 主な機能

- Safari風のアプリ内ブラウザ（URL検索、戻る・進む・更新、外部ブラウザ連携）
- 表示中ページを `CAPTURE` してyt-dlpの前景サービスでダウンロード
- 動画・音声の自動結合とメタデータ／サムネイル埋め込み（FFmpeg）
- ダウンロード進捗、ETA、キャンセル、完了／失敗通知
- サムネイル付きライブラリ、タイトル検索、再生、共有
- 画面遷移、背景オーブ、プログレス軌道、パルス、展開メニューなどの独自アニメーション

ファイルは端末の `Downloads/Veiload` に保存されます。

## ビルド

Android Studioでプロジェクトを開くか、JDK 17以上を指定して実行します。

```powershell
.\gradlew.bat assembleDebug
```

デバッグAPKは `app/build/outputs/apk/debug/app-debug.apk` に生成されます。yt-dlp・Python・FFmpegのネイティブバイナリを内包するため、APKサイズは一般的なUIアプリより大きくなります。

ダウンロード対象の利用規約と著作権を確認し、保存する権利のあるコンテンツにのみ使用してください。
