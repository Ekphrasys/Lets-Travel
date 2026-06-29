pipeline {
    agent any

    options {
        timeout(time: 120, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    tools {
        maven 'maven-3'
        jdk 'jdk-17'
        nodejs 'node-22'
        'hudson.plugins.sonar.SonarRunnerInstallation' 'sonar-scanner'
    }

    environment {
        CHROME_BIN = '/usr/bin/chromium'
        SONAR_SERVER_NAME = 'sonar-server'
        DEPLOY_ENV_FILE = '/var/jenkins_home/deploy-config/.env'
        ELASTICSEARCH_HOST = 'elasticsearch'
        NEO4J_HOST = 'neo4j'
        NEO4J_PASSWORD = credentials('neo4j-password')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Branche: ${env.GIT_BRANCH ?: env.BRANCH_NAME ?: 'master'}"
                sh 'test -f Jenkinsfile && test -d microservices && test -d frontend'
            }
        }

        stage('Build & Test Backend') {
            parallel {
                stage('Core Services') {
                    steps {
                        script {
                            def services = [
                                'discovery-service',
                                'gateway-service',
                                'auth-service',
                                'user-service',
                                'payment-service'
                            ]
                            for (service in services) {
                                dir("microservices/${service}") {
                                    echo "--- Building and Testing ${service} ---"
                                    sh 'mvn clean verify -DskipTests=false'
                                }
                            }
                        }
                    }
                }
                stage('Travel Service (ES + Neo4j)') {
                    steps {
                        dir('microservices/travel-service') {
                            echo "--- Building travel-service (Elasticsearch + Neo4j tests) ---"
                            sh '''
                                mvn clean verify \
                                    -DskipTests=false \
                                    -Delasticsearch.host=${ELASTICSEARCH_HOST} \
                                    -Dneo4j.host=${NEO4J_HOST} \
                                    -Dneo4j.password=${NEO4J_PASSWORD}
                            '''
                        }
                    }
                }
            }
        }

        stage('Test & Build Frontend') {
            steps {
                dir('frontend/travel-admin') {
                    sh 'npm ci'
                    sh 'npm run test -- --no-watch --no-progress --browsers=ChromeHeadlessNoSandbox --code-coverage'
                    sh 'npm run build'
                }
            }
        }

        stage('Code Quality Analysis') {
            steps {
                script {
                    echo "--- SonarQube Analysis + Quality Gate ---"
                    def scannerHome = tool 'sonar-scanner'

                    withSonarQubeEnv("${env.SONAR_SERVER_NAME}") {
                        sh """
                            ${scannerHome}/bin/sonar-scanner \
                              -Dproject.settings=sonar-project.properties \
                              -Dsonar.host.url=\${SONAR_HOST_URL} \
                              -Dsonar.qualitygate.wait=true \
                              -Dsonar.qualitygate.timeout=600 \
                              -Dsonar.projectKey=lets-travel \
                              -Dsonar.projectName=Lets-Travel
                        """
                    }
                }
            }
        }

        stage('Security Scan') {
            steps {
                script {
                    echo "--- Dependency Security Scan ---"
                    sh 'mvn org.owasp:dependency-check-maven:check -Dformat=HTML -Dformat=JSON || true'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    echo "--- Building Docker Images ---"
                    def services = [
                        'discovery-service',
                        'gateway-service',
                        'auth-service',
                        'user-service',
                        'travel-service',
                        'payment-service'
                    ]
                    for (service in services) {
                        dir("microservices/${service}") {
                            sh "docker build -t travel-${service}:latest ."
                        }
                    }
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                sh '''
                    test -f "$DEPLOY_ENV_FILE" || { echo "ERREUR: $DEPLOY_ENV_FILE absent"; exit 1; }
                    docker compose -f infrastructure/docker-compose.yml --project-name travel-staging down --remove-orphans || true
                    docker compose -f infrastructure/docker-compose.yml --project-name travel-staging up -d --build
                '''
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    echo "--- Running Integration Tests ---"
                    sh '''
                        sleep 60
                        bash scripts/audit-api-test.sh staging
                    '''
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    test -f "$DEPLOY_ENV_FILE" || { echo "ERREUR: $DEPLOY_ENV_FILE absent"; exit 1; }
                    bash scripts/deploy-stack.sh
                '''
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            cleanWs()
        }
        success {
            echo 'Pipeline SUCCESS — build, tests, SonarQube, deploy OK'
        }
        failure {
            echo 'Pipeline FAILED — voir logs Jenkins et rapport SonarQube'
        }
    }
}
