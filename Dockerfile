FROM jenkins/jenkins:lts-jdk17

USER root

RUN apt-get update && \
    apt-get install -y chromium git curl python3 unzip xz-utils && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/chromium

# Outils CI (chemins référencés dans ci/jenkins/casc.yaml)
ARG MAVEN_VERSION=3.9.9
ARG NODE_VERSION=22.12.0
ARG SONAR_SCANNER_VERSION=6.2.1.4610

RUN curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
      | tar xz -C /opt && \
    ln -sf "/opt/apache-maven-${MAVEN_VERSION}" /opt/maven && \
    curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.xz" \
      | tar xJ -C /opt && \
    ln -sf "/opt/node-v${NODE_VERSION}-linux-x64" /opt/node && \
    curl -fsSL "https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux-x64.zip" \
      -o /tmp/sonar-scanner.zip && \
    unzip -q /tmp/sonar-scanner.zip -d /opt && \
    ln -sf "/opt/sonar-scanner-${SONAR_SCANNER_VERSION}-linux-x64" /opt/sonar-scanner && \
    rm -f /tmp/sonar-scanner.zip

COPY ci/jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt

USER jenkins
