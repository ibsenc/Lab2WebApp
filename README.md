# Lab2WebApp
## CS6650 Fall Lab2 Serverlet

### Serverlet Setup:
1. Git clone this repository. It is recommended to use this serverlet on Intellij Ultimate Edition.
2. Ensure Tomcat server is set up using the instructions [here](https://github.com/gortonator/bsds-6650/blob/master/labs/lab-2.md#step-4-install-tomcat-locally-and-set-up-tomcat-in-intellij).
3. For remote API requests, set up an EC2 instance with a Tomcat server. See [Lab1](https://github.com/gortonator/bsds-6650/blob/master/labs/lab-1.md) for more information. Then, deploy the war file to the EC2 instance using [these instructions](https://github.com/gortonator/bsds-6650/blob/master/labs/lab-2.md#step-5-deploy-war-file-to-tomcat-server-in-ec2).

### To Access POST and GET endpoints:
#### Locally:
Visit `http://localhost:8081/Lab2WebApp_war_exploded/skiers/12/seasons/2019/day/1/skier/123`

#### Via AWS EC2:
Visit `http://[EC2 PUBLIC IP]:8081/Lab2WebApp_war/skiers/12/seasons/2019/day/1/skier/123`

Example: `http://34.209.104.204:8081/Lab2WebApp_war/skiers/12/seasons/2019/day/1/skier/123`

### To deploy an updated war file to EC2:
1. Delete the `Lab2WebApp_war.war` file from path `out/artifacts/Lab2WebApp/`.
2. Open the Build menu and click Build Artifacts and click "Build" for "Lab2WebApp:war". A WAR file called "Lab2WebApp_war.war" will be created in `out/artifacts/Lab2WebApp/`.
3. Run the following command with appropriate paths to EC2 instance pem file, the Lab2WebApp application, and the Tomcat `webapps` file within the EC2 instance. Note: the path to the Tomcat `webapps` file may be different depending on the Tomcat version.


For example (my pem file is located in the same directory the serverlet. I am using Tomcat9.):
sudo scp -i ./lab_1_key_pair.pem ./lab2webapp/Lab2WebApp/out/artifacts/Lab2WebApp_war/Lab2WebApp_war.war ec2-user@ec2-35-89-135-255.us-west-2.compute.amazonaws.com:/usr/share/tomcat/webapps
