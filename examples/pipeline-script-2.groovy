// Simple script with one step and ServiceNow Parameters

pipeline {
    agent any

    parameters {
            snParam(credentialsForPublishedApp: "88dbbe69-0e00-4dd5-838b-2fbd8dfedeb4", instanceForPublishedAppUrl: "https://cicdjenkinsapppublish.service-now.com", appScope: "x_sofse_cicdjenkins")
    }

    stages {
        stage('preparation') {
            steps {
                echo "${params.snParam}"

                snApplyChanges()
            }
        }
    }
}