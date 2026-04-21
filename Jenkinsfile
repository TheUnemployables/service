pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        JAVA_HOME = '/opt/java/openjdk'
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
    }

    stages {

        stage('Unit Tests & Build') {
            steps {
                sh './gradlew clean test build -x javadoc'
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/**/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'build/reports/jacoco/test/**', allowEmptyArchive: true
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
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t prod-eng-img .'
            }
        }
    }

    post {
        success {
            echo "Pipeline passed — image prod-eng-img is ready to deploy."
        }
        failure {
            echo "Pipeline failed — check test results above."
        }
    }
}
