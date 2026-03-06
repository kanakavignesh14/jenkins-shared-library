def call(Map configMap) {
    pipeline {
        agent {
            node {
                label 'AGENT-1'
            }
        }

        environment {
            COURSE = "Jenkins"
            appVersion = configMap.get("appVersion")
            ACC_ID = "369012866895"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
            deploy_to = configMap.get("deploy_to")
            REGION = "us-east-1"
        }

        options {
            timeout(time: 30, unit: 'MINUTES')
            disableConcurrentBuilds()
        }

        stages {
            stage('Deploy') {
                steps {
                    script {
                        withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                            sh """
                                set -e
                                echo "${appVersion}"
                                aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}
                                kubectl get nodes
                                sed -i "s/IMAGE_VERSION/${appVersion}/g" values.yaml
                                helm upgrade --install ${COMPONENT} \
                                 -f valules-${deploy_to}.yaml \
                                 -n ${PROJECT} \
                                 --rollback-on-failure \
                                 --wait --timeout=10m .
                            """
                        }
                    }
                }
            }

            stage('Functional Tests') {
                when {
                    expression { env.deploy_to == 'dev' }
                }
                steps {
                    script {
                        sh 'echo "functional test cases in dev environment"'
                    }
                }
            }
        }

        post {
            always {
                echo 'I will always say Hello again!'
                cleanWs()
            }

            success {
                script {
                    withCredentials([string(credentialsId: 'slack-token', variable: 'SLACK_WEBHOOK')]) {
                        def payload = """
                        {
                            "attachments": [
                                {
                                    "color": "#2eb886",
                                    "title": "✅ Jenkins Build Successful",
                                    "fields": [
                                        {"title": "Job Name", "value": "${env.JOB_NAME}", "short": true},
                                        {"title": "Build Number", "value": "${env.BUILD_NUMBER}", "short": true},
                                        {"title": "Status", "value": "SUCCESS", "short": true},
                                        {"title": "Build URL", "value": "${env.BUILD_URL}", "short": false}
                                    ],
                                    "footer": "Jenkins CI",
                                    "ts": ${System.currentTimeMillis() / 1000}
                                }
                            ]
                        }
                        """
                        sh "curl -X POST -H 'Content-type: application/json' --data '${payload}' ${SLACK_WEBHOOK}"
                    }
                }
            } // This brace was previously duplicated, closing 'post' early

            failure {
                echo 'I will run if failure'
            }

            aborted {
                echo 'pipeline is aborted'
            }
        } // End of post
    } // End of pipeline
}