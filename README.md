> # pods-health-check-service

This repository will be used to notify errors or exceptions raised in open shift pods. The main objective of this application is to notify about the open shift pods status when an error occur.


> # Functionality

- [x] Takes configurations as an input file.
- [x] Cron Job Scheduler implementation.
- [x] Automatically trigger/notify email when an error occurred due to kafka certificates change, Cassandra certificates change, password change, Topic name change etc.


> # Setup & Configuration

- [x] First, clone pods-health-check-service project on your local computer

  $ https://github.com/Vamshi402/pods-health-check-service


- [x] This project has an input file where you can provide input parameters to run this project. Below are the input parameters required:

      1. OpenShift URL (Ex:https://ocp-ctc-core-nonprod.optum.com:443)
      2. OpenShift Project Name (Ex:nonprod-c360-cross-carrier-eligibility-processor-0)
      3. OpenShift login username
      4. OpenShift login password (password should be decrypt using AES_KEY used in application.yml file.
      5. DL/Individual email id (email id to sent notification about the error pod status)


- [x] Use **mvn clean install** command to build the project

- [x] Once the build is completed, run the application