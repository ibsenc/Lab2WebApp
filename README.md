# Lab2WebApp
## CS6650 Fall Lab2 Serverlet

### Serverlet Setup:
1. Git clone this repository. It is recommended to use this serverlet on Intellij Ultimate Edition.
2. Ensure Tomcat server is set up using these instructions: [Step 4: Install Tomcat locally and set up Tomcat in IntelliJ](https://github.com/gortonator/bsds-6650/blob/master/labs/lab-2.md#step-4-install-tomcat-locally-and-set-up-tomcat-in-intellij).
3. For remote API requests, set up an EC2 instance with a Tomcat server. See [Lab1](https://github.com/gortonator/bsds-6650/blob/master/labs/lab-1.md) for more information.
4. Deploy the war file to the EC2 instance using these instructions: [Step 5: Deploy WAR file to Tomcat server in EC2](https://github.com/gortonator/bsds-6650/blob/master/labs/lab-2.md#step-5-deploy-war-file-to-tomcat-server-in-ec2). See section [To deploy an updated war file to EC2](https://github.com/ibsenc/Lab2WebApp/edit/main/README.md#to-deploy-an-updated-war-file-to-ec2) for an example and tips.

### To Access POST and GET endpoints:
#### Locally:
- Visit `http://localhost:8081/Lab2WebApp_war_exploded/skiers/9/seasons/2022/day/1/skier/123`

#### Via AWS EC2:
- Visit `http://[EC2 PUBLIC IP]:8081/Lab2WebApp_war/skiers/9/seasons/2022/day/1/skier/123`

- Example: `http://34.209.104.204:8081/Lab2WebApp_war/skiers/9/seasons/2022/day/1/skier/123`

### To deploy an updated war file to EC2:
1. Delete the `Lab2WebApp_war.war` file from path `out/artifacts/Lab2WebApp/`.
2. Open the Build menu and click Build Artifacts and click "Build" for "Lab2WebApp:war". A WAR file called "Lab2WebApp_war.war" will be created in `out/artifacts/Lab2WebApp/`.
3. Follow the instructions for [Step 5: Deploy WAR file to Tomcat server in EC2](https://github.com/gortonator/bsds-6650/blob/master/labs/lab-2.md#step-5-deploy-war-file-to-tomcat-server-in-ec2) and run the specified command with appropriate paths to EC2 instance pem file, the Lab2WebApp application, and the Tomcat `webapps` file within the EC2 instance. Note: the path to the Tomcat `webapps` file may be different depending on the Tomcat version.

For example, I will run the following (my pem file is located in the same directory the serverlet. I am using Tomcat9.):

`sudo scp -i ./lab_1_key_pair.pem ./lab2webapp/Lab2WebApp/out/artifacts/Lab2WebApp_war/Lab2WebApp_war.war ec2-user@ec2-35-89-135-255.us-west-2.compute.amazonaws.com:/usr/share/tomcat/webapps`

### Some Useful Tomcat Commands:
- To start the service: `sudo systemctl start tomcat`

- To stop the service: `sudo systemctl stop tomcat`

- To check the status of the service: `systemctl status tomcat.service`
