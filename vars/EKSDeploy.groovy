def call (Map configMap){
pipeline {
    // These are pre-build sections
    agent {
        node {
            label 'AGENT-1'
        }
    }
    environment {
        COURSE = "Jenkins"
        appVersion = configMap.get("appVersion")
        ACC_ID = "58633703"
        PROJECT = configMap.get("project")
        COMPONENT = configMap.get("component")
        deploy_to = configMap.get("deploy_to")
        REGION = "us-east-1"
    }
    options {
        timeout(time: 30, unit: 'MINUTES') 
        disableConcurrentBuilds()
    }
   /* parameters {
        string(name: 'appVersion', description: 'Which app version you want to deploy')
        choice(name: 'deploy_to', choices: ['dev', 'qa', 'prod'], description: 'Pick something')
    }*/
    // This is build section
    stages {
        
        stage('Deploy') {
            steps {
                script{
                    withAWS(region:'us-east-1',credentials:'aws-creds') {
                        sh """
                            set -e
                            aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}
                            kubectl get nodes
                            sed -i "s/IMAGE_VERSION/${appVersion}/g" values.yaml 
                            helm upgrade --install ${component} -f values-${deploy_to}.yaml -n ${PROJECT} --atomic --wait --timeout=5m .
                    
                        """
                    }
                }
            }
        }
        
    }
/* tht helm command need appverison and deploy area so we gving it here appVersion coming from catalogue CI to here and 
deplpyy to is jus variable will fix here" but will habe default value as dev */

   /* here we doing BUILD ONCE AND RUN ANYWHERE IMAGE" */

   /* helm upgrade --install --> first time it will install secound time it will use ugrade" 

    helm upgrade --install $component -f values-${deploy_to}.yaml -n ${PROJECT} */
        

    post{
        always{
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