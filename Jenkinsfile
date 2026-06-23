pipeline {
    agent any

    options {
        timeout(time: 90, unit: 'MINUTES')
        timestamps()
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
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Branche: ${env.GIT_BRANCH ?: env.BRANCH_NAME ?: 'master'}"
                sh 'test -f Jenkinsfile && test -d microservices'
            }
        }

        stage('Build & Test Backend') {
            steps {
                script {
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
                            echo "--- Building and Testing ${service} ---"
                            sh 'mvn clean verify -DskipTests=false'
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
                              -Dsonar.qualitygate.timeout=600
                        """
                    }
                }
            }
        }

        stage('Deploy to Production') {
            steps {
                sh '''
                    test -f "$DEPLOY_ENV_FILE" || { echo "ERREUR: $DEPLOY_ENV_FILE absent"; exit 1; }
                    bash scripts/deploy-stack.sh
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline SUCCESS — build, tests, SonarQube, deploy OK'
        }
        failure {
            echo 'Pipeline FAILED — voir logs Jenkins et rapport SonarQube'
        }
    }
}
