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
        PAYARA_HOME = '/opt/payara5' // Adjust for Windows agents, e.g., 'C:\\payara5'
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'github-hmis-credentials',
                    url: 'https://github.com/shubhamprojects985/hmis.git'
            }
        }
        stage('Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn clean package -DskipTests'
                    } else {
                        bat 'mvn clean package -DskipTests'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war', fingerprint: true
                }
            }
        } 
        // stage('Parallel Testing') {
        //     parallel {
        //         stage('Unit Tests') {
        //             steps {
        //                 script {
        //                     if (isUnix()) {
        //                         sh 'mvn test -Dtest=**/*UnitTest.java'
        //                     } else {
        //                         bat 'mvn test -Dtest=**/*UnitTest.java'
        //                     }
        //                 }
        //             }
        //         }
        //         stage('Integration Tests') {
        //             agent {
        //                 docker {
        //                     image 'mysql:8.0'
        //                     args '-p 3306:3306 -e MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD} -e MYSQL_DATABASE=${MYSQL_DATABASE} -e MYSQL_USER=${MYSQL_USER} -e MYSQL_PASSWORD=${MYSQL_PASSWORD}'
        //                     reuseNode true
        //                 }
        //             }
        //             steps {
        //                 script {
        //                     if (isUnix()) {
        //                         sh 'mvn verify -Dtest=**/*IntegrationTest.java'
        //                     } else {
        //                         bat 'mvn verify -Dtest=**/*IntegrationTest.java'
        //                     }
        //                 }
        //             }
        //         }
        //     }
        //     post {
        //         always {
        //             junit 'target/surefire-reports/*.xml'
        //         }
        //     }
        // }
        stage('Deploy to Payara') {
            when {
                branch 'main'
            }
            steps {
                script {
                    def asadminCmd = isUnix() ? "${PAYARA_HOME}/bin/asadmin" : "${PAYARA_HOME}\\bin\\asadmin.bat"
                    if (isUnix()) {
                        sh "${asadminCmd} stop-domain"
                        sh "${asadminCmd} deploy --force target/*.war"
                        sh "${asadminCmd} start-domain"
                    } else {
                        bat "${asadminCmd} stop-domain"
                        bat "${asadminCmd} deploy --force target/*.war"
                        bat "${asadminCmd} start-domain"
                    }
                }
            }
            post {
                success {
                    script {
                        def artifactDir = "${env.JENKINS_HOME}/artifacts/hmis"
                        if (isUnix()) {
                            sh "mkdir -p ${artifactDir}"
                            sh "cp target/*.war ${artifactDir}/last-successful.war"
                        } else {
                            bat "mkdir ${artifactDir}"
                            bat "copy target\\*.war ${artifactDir}\\last-successful.war"
                        }
                    }
                }
                failure {
                    script {
                        def asadminCmd = isUnix() ? "${PAYARA_HOME}/bin/asadmin" : "${PAYARA_HOME}\\bin\\asadmin.bat"
                        def artifactDir = "${env.JENKINS_HOME}/artifacts/hmis"
                        if (isUnix()) {
                            sh "${asadminCmd} undeploy hmis || true"
                            sh "${asadminCmd} deploy --force ${artifactDir}/last-successful.war || echo 'No rollback artifact available'"
                        } else {
                            bat "${asadminCmd} undeploy hmis || exit 0"
                            bat "${asadminCmd} deploy --force ${artifactDir}\\last-successful.war || echo \"No rollback artifact available\""
                        }
                    }
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
