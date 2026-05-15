pipeline {
    agent any

    environment {
        DOCKERHUB_USER = 'ukbey'
        IMAGE_NAME     = "${DOCKERHUB_USER}/patent-app"
        IMAGE_TAG      = "latest"
        DOCKER_CRED    = 'dockerhub-creds'
    }

    stages {
        stage('1. Clone from GitHub') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/UKBey/Devops_Project_4.git'
            }
        }

        stage('2. Build JAR (Maven)') {
            steps {
                bat 'mvn -B clean package -DskipTests'
            }
        }

        stage('3. Build Docker Image') {
            steps {
                bat "docker build -t %IMAGE_NAME%:%IMAGE_TAG% ."
            }
        }

        stage('4. Login to DockerHub') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: "${DOCKER_CRED}",
                        usernameVariable: 'DH_USER',
                        passwordVariable: 'DH_PASS')]) {
                    bat 'echo %DH_PASS%| docker login -u %DH_USER% --password-stdin'
                }
            }
        }

        stage('5. Push Image to DockerHub') {
            steps {
                bat "docker push %IMAGE_NAME%:%IMAGE_TAG%"
            }
        }

        stage('6. Deploy to Kubernetes (Minikube)') {
            steps {
                bat 'kubectl apply -f k8s/deployment.yaml'
                bat 'kubectl apply -f k8s/service.yaml'
                bat 'kubectl rollout restart deployment/patent-app-deployment'
                bat 'kubectl get pods -o wide'
                bat 'kubectl get svc patent-app-service'
            }
        }
    }

    post {
        always {
            bat 'docker logout || exit 0'
        }
    }
}
