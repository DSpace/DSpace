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

    options { buildDiscarder(logRotator(numToKeepStr: '5')) }

    stages {

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

        stage('Provide input parameters to confirm deployment') {
            steps {
                script {
                    def institusjoner = readYaml file: "${CUSTOMZ}/institusjoner.yml"
                    def kunder = []

                    institusjoner.each { prop, val ->
                        kunder << prop
                    }

                    try {
                        timeout(activity: true, time: 120, unit: 'SECONDS') {
                            inputResult = input(id: 'phaseInput', message: 'Velg parametre', parameters: [
                                    choice(choices: ["produksjon", "utvikle", "test"], name: 'devstep', description: 'Utviklingsfase:'),
                                    choice(choices: kunder, name: 'kunde', description: "Kunde:"),
                                    choice(choices: ["#brage", "#sandbox_playground"], name: 'slackchannel', description: "Slack kanal for output:")
                            ])
                        }
                    } catch (err) {
                        echo "Release aborted"
                        throw err
                    }
					SLACK_CHANNEL = inputResult.slackchannel
					slackSend channel: SLACK_CHANNEL, iconEmoji: ':information_source:', message: 'Deployment av Brage-instans `' + inputResult.kunde + '` på *' + inputResult.devstep + '* starter', username: 'BrageDeployment', tokenCredentialId: 'brage_slack', teamDomain: 'unit-norge'
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
                println("Deploying branch $VERSION for ${inputResult.kunde} to ${inputResult.devstep}")
                dir("${env.WORKSPACE}/deployscripts") {
                    withCredentials([string(credentialsId: 'brage_vault_' + inputResult.devstep, variable: 'VAULTSECRET')]) {
                        ansiblePlaybook(
                                playbook: 'deploy-brage.yml',
                                inventory: 'hosts',
                                extraVars: [
                                        fase             : inputResult.devstep,
                                        jenkins_workspace: env.WORKSPACE,
                                        kunde            : inputResult.kunde,
										slack_channel    : SLACK_CHANNEL,
                                        vault_secret     : "$VAULTSECRET"
                                ]
                        )
                    }
                }
				slackSend channel: SLACK_CHANNEL, iconEmoji: ':information_source:', message: 'Installasjon ferdig. Ny versjon av `' + inputResult.kunde + '` er rullet ut til *' + inputResult.devstep + '*', username: 'BrageDeployment', tokenCredentialId: 'brage_slack', teamDomain: 'unit-norge'
            }
        }

        stage('Cleanup') {
            steps {
                cleanWs()
            }
        }
    }

}

