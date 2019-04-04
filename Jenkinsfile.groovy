pipeline {
    agent any

    environment {
        VERSION = "${env.BRANCH_NAME}".replaceAll('/', '_').toLowerCase()
    }

    stages {
        stage('Maven Build') {
            steps {
                sh 'mvn package -Dmirage2.on=true -P !dspace-lni,!dspace-sword,!dspace-jspui,!dspace-rdf'
            }
        }
        stage('Copy files to server') {
            steps {
                sh 'echo "Copying files to server"'
                sh 'pwd'
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
                        to: 'pcb@unit.no',
                        attachlog: true,
                        compresslog: true,
                        recipientProviders: [[$class: 'CulpritsRecipientProvider']]
                )

            }
        }
    }
}

