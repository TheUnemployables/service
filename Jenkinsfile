pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        DOCKER_IMAGE = 'theunemployables/prod-eng'
        JAVA_HOME    = '/opt/java/openjdk'
        PATH         = "${env.JAVA_HOME}/bin:${env.PATH}"
    }

    stages {

        stage('Build') {
            steps {
                sh './gradlew clean build -x test -x javadoc'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test jacocoTestReport'
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/**/*.xml', allowEmptyResults: true
                    publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'build/reports/tests/test',
                        reportFiles          : 'index.html',
                        reportName           : 'Unit Test Report'
                    ])
                    publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'build/reports/jacoco/test/html',
                        reportFiles          : 'index.html',
                        reportName           : 'Coverage Report'
                    ])
                }
            }
        }

        stage('Version') {
            steps {
                script {
                    def gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${BUILD_NUMBER}-${gitCommit}"
                    echo "Version: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} -t ${DOCKER_IMAGE}:latest ."
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                    sh "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    def mongoIp = sh(
                        script: "docker inspect service-mongo-1 --format '{{.NetworkSettings.Networks.service_default.IPAddress}}' 2>/dev/null || echo '172.19.0.2'",
                        returnStdout: true
                    ).trim()
                    sh """
                        docker rm -f service-prod-eng-1 2>/dev/null || true
                        docker run -d \
                          --name service-prod-eng-1 \
                          --network service_default \
                          -p 8080:8080 \
                          -p 5005:5005 \
                          --add-host mongo:${mongoIp} \
                          -e ENVIRONMENT_NAME=local \
                          -e MONGODB_CONECTION_URL=mongodb://root:example@${mongoIp}:27017/ \
                          --restart always \
                          ${DOCKER_IMAGE}:${IMAGE_TAG}
                        timeout 60 bash -c 'until curl -sf http://localhost:8080/actuator/health; do sleep 3; done'
                    """
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh './gradlew testIT'
            }
            post {
                always {
                    junit testResults: 'build/test-results/testIT/**/*.xml', allowEmptyResults: true
                    publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'build/reports/tests/testIT',
                        reportFiles          : 'index.html',
                        reportName           : 'Integration Test Report'
                    ])
                }
            }
        }

        stage('Tag') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'github', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    sh """
                        git config user.email "jenkins@localhost"
                        git config user.name "Jenkins"
                        git tag -a v${IMAGE_TAG} -m "Release v${IMAGE_TAG}"
                        git push https://${GIT_USER}:${GIT_TOKEN}@github.com/TheUnemployables/service.git v${IMAGE_TAG}
                    """
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        success {
            echo "Deployed ${DOCKER_IMAGE}:${IMAGE_TAG}"
        }
        failure {
            echo "Pipeline failed — check stage logs above."
        }
    }
}
