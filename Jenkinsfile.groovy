pipeline {
    agent any

    checkout scm

    dir('artifacts') {
        git url: 'https://github.com/BIBSYSDEV/2019-03-05_FaaSPresentation.git'
    }

    environment {
        VERSION = "${env.BRANCH_NAME}".replaceAll('/', '_').toLowerCase()
    }

    stages {
//        stage('Maven Build') {
//            steps {
//                sh 'mvn package -Dmirage2.on=true -P !dspace-lni,!dspace-sword,!dspace-jspui,!dspace-rdf'
//            }
//        }
        stage('Doing stuff in workspace') {
            steps {
                sh 'echo "Doing stuff"'
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

