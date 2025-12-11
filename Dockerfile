# Scarab Docker Image
# Ubuntu 22.04 base with Scala 2.13 and sbt

FROM ubuntu:22.04

# Avoid interactive prompts
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Tokyo

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    gnupg \
    apt-transport-https \
    openjdk-11-jdk \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install Scala 2.13.16
ENV SCALA_VERSION=2.13.16
RUN wget https://downloads.lightbend.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz && \
    tar -xzf scala-${SCALA_VERSION}.tgz && \
    mv scala-${SCALA_VERSION} /usr/local/scala && \
    rm scala-${SCALA_VERSION}.tgz

# Install sbt 1.10.7
ENV SBT_VERSION=1.10.7
RUN wget https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz && \
    tar -xzf sbt-${SBT_VERSION}.tgz && \
    mv sbt /usr/local/sbt && \
    rm sbt-${SBT_VERSION}.tgz

# Set environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV SCALA_HOME=/usr/local/scala
ENV SBT_HOME=/usr/local/sbt
ENV PATH=${SCALA_HOME}/bin:${SBT_HOME}/bin:${PATH}

# Create working directory
WORKDIR /scarab

# Copy project files
COPY build.sbt .
COPY project ./project
COPY src ./src
COPY examples ./examples
COPY LICENSE .
COPY README.md .

# Build project
RUN sbt compile assembly

# Default command
CMD ["/bin/bash"]
