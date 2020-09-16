// Simple script with one step

pipeline {
    agent any

    stages {
        stage('preparation') {
            steps {
                snApplyChanges url: "https://cicdjenkinsapppublish.service-now.com", credentialsId: "88dbbe69-0e00-4dd5-838b-2fbd8dfedeb4", appScope: "x_sofse_cicdjenkins"
            }
        }
    }
}