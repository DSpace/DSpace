pipeline {
    agent any

    tools {
        maven 'Maven 3.6.1'
    }

    environment {
        VERSION = "${env.BRANCH_NAME}".replaceAll('/', '_').toLowerCase()
        CUSTOMZ = "customizations"
		SLACK_CHANNEL = '#sandbox_playground'
    }

    stages {

        stage('Provide input parameters to confirm deployment') {
            steps {
                script {
                    try {
                        timeout(activity: true, time: 120, unit: 'SECONDS') {
                            inputResult = input(id: 'phaseInput', message: 'Velg parametre', parameters: [
                                    choice(choices: ["produksjon", "utvikle", "test"], name: 'devstep', description: 'Utviklingsfase:')
                            ])
                        }
                    } catch (err) {
                        echo "Release aborted"
                        throw err
                    }
					if ( inputResult == 'produksjon' )
						SLACK_CHANNEL = '#brage'
					slackSend channel: SLACK_CHANNEL, iconEmoji: ':information_source:', message: 'Deployment av alle Brage-instanser på *' + inputResult + '* starter', username: 'BrageDeployment', tokenCredentialId: 'brage_slack', teamDomain: 'unit-norge'
                }
            }
        }

        stage('Checkout Brage6 customizations') {
            steps {
                script {
                    brageVars = checkout scm
                    dir("${CUSTOMZ}") {
                        //configVars = checkout scm
                        git url: 'git@git.bibsys.no:team-rosa/brage6-customizations.git'
                    }
                }
            }
        }

        stage('Maven Build') {
            steps {
				slackSend channel: SLACK_CHANNEL, iconEmoji: ':information_source:', message: 'Bygger applikasjonen..', username: 'BrageDeployment', tokenCredentialId: 'brage_slack', teamDomain: 'unit-norge'
                echo "Building with maven"
                sh 'mvn package -Dmirage2.on=true -P !dspace-lni,!dspace-sword,!dspace-jspui,!dspace-rdf'
            }
        }

        stage('Deploy Brage') {
            steps {
				slackSend channel: SLACK_CHANNEL, iconEmoji: ':information_source:', message: 'Bygging ferdig. Klargjør installasjonspakke..', username: 'BrageDeployment', tokenCredentialId: 'brage_slack', teamDomain: 'unit-norge'
                println("Deploying branch $VERSION to ${inputResult}")
                dir("${env.WORKSPACE}/deployscripts") {
                    withCredentials([string(credentialsId: 'brage_vault_' + inputResult, variable: 'VAULTSECRET')]) {
                        ansiblePlaybook(
                                playbook: 'deploy-brage-all.yml',
                                inventory: 'hosts',
								forks: 5,
                                extraVars: [
                                        fase             : inputResult,
                                        jenkins_workspace: env.WORKSPACE,
										slack_channel    : SLACK_CHANNEL,
                                        vault_secret     : "$VAULTSECRET"
                                ]
                        )
                    }
                }
				slackSend channel: SLACK_CHANNEL, iconEmoji: ':information_source:', message: 'Installasjon ferdig. Ny versjon av Brage er rullet ut til ' + inputResult, username: 'BrageDeployment', tokenCredentialId: 'brage_slack', teamDomain: 'unit-norge'
            }
        }

        stage('Cleanup') {
            steps {
                cleanWs()
            }
        }
    }

}

