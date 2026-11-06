
pipeline {
    agent none

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout()
    }

    stages {

        stage('Build') {
            agent {
                label 'general'
            }

            steps {

                // Checkout source on Agent 01
                checkout scm

                sh '''
                    echo "Agent: $(hostname)"
                    echo "User: $(whoami)"

                    java -version
                    mvn -version

                    mvn -B clean package
                '''

                // Transfer generated JAR files to other agents
                stash(
                    name: 'catalog-jar',
                    includes: '**/target/*.jar'
                )

                echo 'JAR files successfully stashed.'
            }

            post {
                always {
                    junit(
                        testResults: '**/target/surefire-reports/*.xml',
                        allowEmptyResults: false
                    )
                }

                success {
                    archiveArtifacts(
                        artifacts: '**/target/*.jar',
                        fingerprint: true
                    )
                }
            }
        }

        stage('Verification') {
            agent {
                label 'testing'
            }

            steps {
                echo 'Receiving build artifact on Agent 02'

                // Retrieve artifact from Agent 01
                unstash 'catalog-jar'

                sh '''
                    echo "Agent: $(hostname)"
                    echo "User: $(whoami)"

                    echo "Java version:"
                    java -version

                    echo "Received JAR files:"

                    find . -type f -path '*/target/*.jar' -print

                    echo "Verifying JAR files..."

                    JAR_FILES=$(find . -type f -path '*/target/*.jar')

                    if [ -z "$JAR_FILES" ]; then
                        echo "ERROR: No JAR files received."
                        exit 1
                    fi

                    while IFS= read -r jar; do
                        echo "Checking: $jar"

                        sha256sum "$jar"

                        # Validate JAR archive structure
                        jar tf "$jar" > /dev/null

                        echo "JAR validation passed: $jar"
                    done <<EOF
                    $JAR_FILES
                    EOF
                '''

                echo 'Artifact verification completed successfully.'
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
