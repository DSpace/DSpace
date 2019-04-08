pipeline {
    agent any


    environment {
        VERSION = "${env.BRANCH_NAME}".replaceAll('/', '_').toLowerCase()
    }

    stages {

        stage('Checkout') {
            steps {
                println(" --- Running build #${env.BUILD_ID} of job ${env.JOB_NAME}, git branch: ${env.BRANCH_NAME}" as java.lang.Object)
                script {
                    brageVars = checkout scm

                    dir('config') {
                        //configVars = checkout scm
                        git url: 'https://github.com/BIBSYSDEV/2019-03-05_FaaSPresentation.git'
                    }
                }
            }
        }

        stage('Maven Build') {
            steps {
                sh 'echo " --- Pretending to build with maven"'
//              sh 'mvn package -Dmirage2.on=true -P !dspace-lni,!dspace-sword,!dspace-jspui,!dspace-rdf'
            }
        }

        stage('Doing stuff in workspace') {
            steps {
                sh 'echo " --- Doing stuff"'
                sh 'pwd'
            }
        }

    }

//    post {
//        failure {
//            script {
//                def message = "${currentBuild.fullDisplayName} - Failure after ${currentBuild.durationString.replaceFirst(" and counting", "")}"
//                emailext(
//                        subject: "FAILURE: ${currentBuild.fullDisplayName}",
//                        body: "${message}\n Open: ${env.BUILD_URL}",
//                        to: 'pcb@unit.no',
//                        attachlog: true,
//                        compresslog: true,
//                        recipientProviders: [[$class: 'CulpritsRecipientProvider']]
//                )
//
//            }
//        }
//    }
}

