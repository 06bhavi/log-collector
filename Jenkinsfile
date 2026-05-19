pipeline {
    agent any

    environment {
        // Change this to match the repository name used in your GitHub Actions
        GHCR_REPO = '06bhavi/log-collector'
        REGISTRY = 'ghcr.io'
    }

    stages {
        stage('Checkout') {
            steps {
                // Pull the latest code from the Git repository
                checkout scm
            }
        }

        stage('Pull Images') {
            steps {
                // Assume credentials configured in Jenkins with ID 'ghcr-credentials'
                withCredentials([usernamePassword(credentialsId: 'ghcr-credentials', passwordVariable: 'GHCR_PAT', usernameVariable: 'GHCR_USER')]) {
                    sh 'echo $GHCR_PAT | docker login $REGISTRY -u $GHCR_USER --password-stdin'
                }

                sh """
                    docker pull ${REGISTRY}/${GHCR_REPO}/log-collector:latest
                    docker pull ${REGISTRY}/${GHCR_REPO}/mock-storefront:latest
                    docker pull ${REGISTRY}/${GHCR_REPO}/analytics-dashboard:latest
                    
                    # Tag the pulled GHCR images so docker-compose matches them to the local image names
                    docker tag ${REGISTRY}/${GHCR_REPO}/log-collector:latest log-collector:latest
                    docker tag ${REGISTRY}/${GHCR_REPO}/mock-storefront:latest mock-storefront:latest
                    docker tag ${REGISTRY}/${GHCR_REPO}/analytics-dashboard:latest analytics-dashboard:latest
                """
            }
        }

        stage('Deploy') {
            steps {
                // Restart the services with the fresh images
                // Using --no-build ensures we strictly use the images we just pulled/tagged
                sh '''
                    docker-compose down
                    docker-compose up -d --no-build
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline finished successfully! The microservices are up and running with the fresh images.'
            // Example Slack notification (uncomment and configure if needed):
            // slackSend color: 'good', message: "Deployment Successful: ${env.JOB_NAME} [${env.BUILD_NUMBER}]"
        }
        failure {
            echo '❌ Pipeline failed! Please check the Jenkins logs for more details.'
            // Example Slack notification (uncomment and configure if needed):
            // slackSend color: 'danger', message: "Deployment Failed: ${env.JOB_NAME} [${env.BUILD_NUMBER}]"
        }
        always {
            // Clean up by logging out of the registry
            sh 'docker logout $REGISTRY'
        }
    }
}
