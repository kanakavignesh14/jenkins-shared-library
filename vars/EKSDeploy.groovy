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
                            aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}
                            kubectl get nodes
                            sed -i "s/IMAGE_VERSION/${appVersion}/g" values.yaml
                            helm upgrade --install ${COMPONENT} -f valules-${deploy_to}.yaml -n ${PROJECT} --atomic --wait --timeout=5m .
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
                    sh """
                        echo "functional test cases in dev environment"
                    """
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
            echo 'I will run if success'
        }

        failure {
            echo 'I will run if failure'
        }

        aborted {
            echo 'pipeline is aborted'
        }

    }

}
}