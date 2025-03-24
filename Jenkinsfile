pipeline {
    agent any
    tools {
        maven 'Maven 3.8.6'
        jdk 'JDK11'
    }
    environment {
        MYSQL_ROOT_PASSWORD = 'root'
        MYSQL_DATABASE = 'hmis_db'
        MYSQL_USER = 'hmis_user'
        MYSQL_PASSWORD = 'hmis_pass'
        PAYARA_HOME = '/opt/payara5'
    }
    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/shubhamprojects985/hmis.git', branch: 'main'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war', fingerprint: true
                }
            }
        }
        stage('Parallel Testing') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh 'mvn test -Dtest=**/*UnitTest.java'
                    }
                }
                stage('Integration Tests') {
                    agent {
                        docker {
                            image 'mysql:8.0'
                            args '-p 3306:3306 -e MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD} -e MYSQL_DATABASE=${MYSQL_DATABASE} -e MYSQL_USER=${MYSQL_USER} -e MYSQL_PASSWORD=${MYSQL_PASSWORD}'
                            reuseNode true
                        }
                    }
                    steps {
                        sh 'mvn verify -Dtest=**/*IntegrationTest.java'
                    }
                }
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Deploy to Payara') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sh "${PAYARA_HOME}/bin/asadmin stop-domain"
                    sh "${PAYARA_HOME}/bin/asadmin deploy --force target/*.war"
                    sh "${PAYARA_HOME}/bin/asadmin start-domain"
                }
            }
            post {
                failure {
                    sh "${PAYARA_HOME}/bin/asadmin undeploy hmis"
                    sh "${PAYARA_HOME}/bin/asadmin deploy --force ${env.WORKSPACE}/previous-successful-build/target/*.war || echo 'No rollback artifact available'"
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
        success {
            mail to: 'shubhamstudy551@gmail.com',
                 subject: "HMIS Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build succeeded. Check details at ${env.BUILD_URL}"
        }
        failure {
            mail to: 'shubhamstudy551@gmail.com',
                 subject: "HMIS Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed. Check details at ${env.BUILD_URL}"
        }
    }
}
