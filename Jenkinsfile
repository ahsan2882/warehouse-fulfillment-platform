
pipeline {
    agent none

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout()
    }

    environment {
        APP_NAME = 'catalog-service'
    }

    stages {

        stage('Checkout') {
            agent {
                label 'general'
            }

            steps {
                checkout scm

                echo "Building ${APP_NAME}"
                sh 'java -version'
                sh 'mvn -version'
            }
        }

        stage('Build') {
            agent {
                label 'general'
            }

            steps {
                sh 'mvn -B clean package'
            }

            post {
                always {
                    junit(
                        testResults: '**/target/surefire-reports/*.xml',
                        allowEmptyResults: false
                    )
                }
            }
        }

        stage('Verification') {
            agent {
                label 'testing'
            }

            steps {
                echo 'Running verification on Agent 02'

                sh '''
                    echo "Agent: $(hostname)"
                    echo "User: $(whoami)"

                    mvn -version
                    mvn -B clean verify
                '''
            }
        }
    }

    post {
        success {
            echo 'Warehouse application CI build successful!'
        }

        failure {
            echo 'Build failed. Check the console output.'
        }
    }
}
