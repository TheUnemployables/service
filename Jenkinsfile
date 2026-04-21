pipeline {
    agent none

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {

        stage('Unit Tests & Build') {
            agent {
                docker {
                    image 'gradle:8.12.0-jdk21'
                    args '-v gradle-cache:/root/.gradle'
                    reuseNode false
                }
            }
            steps {
                sh './gradlew clean test build -x javadoc'
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/**/*.xml', allowEmptyResults: true
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: 'build/reports/jacoco/test/jacocoTestReport.xml']],
                        qualityGates: [[threshold: 50.0, metric: 'LINE', baseline: 'PROJECT', unstable: true]]
                    )
                }
            }
        }

        stage('Integration Tests') {
            agent {
                docker {
                    image 'gradle:8.12.0-jdk21'
                    args '-v gradle-cache:/root/.gradle'
                    reuseNode false
                }
            }
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
            agent any
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
