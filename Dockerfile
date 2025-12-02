# Scarab Docker Image
# Ubuntu 22.04 base with Scala 2.12 and sbt

FROM ubuntu:22.04

# タイムゾーンの設定（インタラクティブな質問を避ける）
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Tokyo

# 必要なパッケージのインストール
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    gnupg \
    apt-transport-https \
    openjdk-11-jdk \
    git \
    && rm -rf /var/lib/apt/lists/*

# Scala 2.12.8 のインストール
ENV SCALA_VERSION=2.12.8
RUN wget https://downloads.lightbend.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz && \
    tar -xzf scala-${SCALA_VERSION}.tgz && \
    mv scala-${SCALA_VERSION} /usr/local/scala && \
    rm scala-${SCALA_VERSION}.tgz

# sbt 1.5.8 のインストール
ENV SBT_VERSION=1.5.8
RUN wget https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz && \
    tar -xzf sbt-${SBT_VERSION}.tgz && \
    mv sbt /usr/local/sbt && \
    rm sbt-${SBT_VERSION}.tgz

# 環境変数の設定
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV SCALA_HOME=/usr/local/scala
ENV SBT_HOME=/usr/local/sbt
ENV PATH=${SCALA_HOME}/bin:${SBT_HOME}/bin:${PATH}

# 作業ディレクトリの作成
WORKDIR /scarab

# プロジェクトファイルのコピー
COPY build.sbt .
COPY project ./project
COPY src ./src
COPY org.sat4j.core ./org.sat4j.core
COPY org.sat4j.pb ./org.sat4j.pb
COPY examples ./examples
COPY Makefile .
COPY LICENSE .
COPY README.md .

# sbtの初期化とプロジェクトのビルド
RUN sbt compile assembly

# デフォルトコマンド
CMD ["/bin/bash"]
