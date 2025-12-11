# Scarab Docker 使用方法

このドキュメントでは、Scarab を Docker で実行する方法を説明します。

## 前提条件

- Docker がインストールされていること
- Docker が起動していること

## Docker イメージのビルド

プロジェクトのルートディレクトリで以下のコマンドを実行します：

```bash
docker build -t scarab:latest .
```

ビルドには数分かかる場合があります。

## Docker コンテナの実行

### インタラクティブモードで起動

```bash
docker run -it scarab:latest
```

コンテナ内で Scarab が利用可能になります。

### Scarab プログラムの実行

コンテナ内で Scala スクリプトを実行する例：

```bash
# コンテナを起動
docker run -it scarab:latest

# コンテナ内で
scala -cp /scarab/target/scala-2.13/scarab.jar /scarab/examples/test.sc
```

### ホストのファイルをマウントして実行

ホストマシンのファイルをコンテナ内で実行する場合：

```bash
docker run -it -v $(pwd)/examples:/work scarab:latest
```

コンテナ内で：

```bash
cd /work
scala -cp /scarab/target/scala-2.13/scarab.jar your_program.sc
```

### ワンライナーでスクリプト実行

```bash
docker run --rm -v $(pwd)/examples:/work scarab:latest \
  scala -cp /scarab/target/scala-2.13/scarab.jar /work/test.sc
```

## 使用例

### 例1: サンプルプログラムの実行

```bash
docker run -it scarab:latest bash -c \
  "scala -cp /scarab/target/scala-2.13/scarab.jar /scarab/examples/hcp.sc"
```

### 例2: カスタムプログラムの実行

プロジェクトディレクトリで：

```bash
# カスタムスクリプトを作成（例: my_problem.sc）
# 次に Docker コンテナで実行
docker run -it -v $(pwd):/workspace scarab:latest bash -c \
  "cd /workspace && scala -cp /scarab/target/scala-2.13/scarab.jar my_problem.sc"
```

## 環境情報

このDockerイメージには以下が含まれています：

- **ベースイメージ**: Ubuntu 22.04
- **Java**: OpenJDK 11
- **Scala**: 2.13.16
- **sbt**: 1.10.7
- **Scarab**: プロジェクトバージョン（build.sbt 参照）

## トラブルシューティング

### ビルドが失敗する場合

キャッシュをクリアしてビルドし直す：

```bash
docker build --no-cache -t scarab:latest .
```

### コンテナ内でファイルが見つからない場合

マウントパスを確認してください：

```bash
docker run -it -v $(pwd):/workspace scarab:latest ls /workspace
```

## より詳しい情報

Scarab の詳細については、メインの README.md を参照してください。
