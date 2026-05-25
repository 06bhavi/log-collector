pipeline {
    agent any

    environment {
        GHCR_REPO = '06bhavi/log-collector'
        REGISTRY = 'ghcr.io'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Pull Images') {
            steps {
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
        }
        failure {
            echo '❌ Pipeline failed! Please check the Jenkins logs for more details.'
                    }
        always {
            sh 'docker logout $REGISTRY'
        }
    }
}
