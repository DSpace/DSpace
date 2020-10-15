#!groovy

pipeline {

    agent any

    tools {
        maven 'Maven 3.6.1'
    }

    environment {
        VERSION = "${env.BRANCH_NAME}".replaceAll('/', '_').toLowerCase()
    }

    stages {
        stage('Create release from master branch') {
            when {
                branch 'master'
            }
            stages {

                stage('Init') {
                    steps {
                        println("Running build #${env.BUILD_ID} of job ${env.JOB_NAME}, git branch: ${env.BRANCH_NAME}, release version: ${VERSION}")
                    }
                }
                stage('Create release?') {
                    steps {
                        script {
                            try {
                                timeout(activity: true, time: 30, unit: 'SECONDS') {
                                    input(message: "Create new release?")
                                }
                            } catch (err) {
                                println("Release aborted")
                                throw err
                            }
                        }
                    }
                }
                stage('Tag, test, deploy to registry') {
                    stages {

                        stage('Tag maven version') {
                            steps{
                                withMaven() {
                                    sh("mvn versions:set -DnewVersion=${VERSION}")
                                }
                            }
                        }

                        stage('Test') {
                            steps {
                                withMaven() {
                                    sh 'mvn clean test'
                                }
                            }
                        }
                        stage('Deploy maven artifact') {
                            steps {
                                withMaven() {
                                    sh("mvn -DskipTests=true deploy scm:tag")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        failure {
            script {
                def message = "${currentBuild.fullDisplayName} - Failure after ${currentBuild.durationString.replaceFirst(" and counting", "")}"
                emailext(
                    subject: "FAILURE: ${currentBuild.fullDisplayName}",
                    body: "${message}\n Open: ${env.BUILD_URL}",
                    to: 'testmail-utvikling@unit.no',
                    attachlog: true,
                    compresslog: true,
                    recipientProviders: [[$class: 'CulpritsRecipientProvider']]
                )
            }
        }
    }
}
