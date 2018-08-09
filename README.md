# camel-wildfly-notification

## Requirements

- [Apache Maven 3.x](http://maven.apache.org)
- [FakeSMTP 2.0](http://nilhcem.com/FakeSMTP/)

## Preparing

Build the project source code

```
cd $PROJECT_ROOT
mvn clean install
```

## Deploying to a standalone WidlFly server

```
cd $PROJECT_ROOT
mvn wildfly:deploy
```

## Running the example in OpenShift

It is assumed that:

- OpenShift platform is already running, if not you can find details how to [Install OpenShift at your site](https://docs.openshift.com/container-platform/3.9/install_config/index.html).
- Your system is configured for Fabric8 Maven Workflow, if not you can find a [Get Started Guide](https://access.redhat.com/documentation/en-us/red_hat_fuse/7.0/html/fuse_on_openshift_guide/)

Issue the following commands:

```
oc login
oc new-project fuse
oc create -f src/main/kube/serviceaccount.yml
oc create -f src/main/kube/configmap.yml
oc create -f src/main/kube/secret.yml
oc secrets add sa/camel-wildfly-notification-sa secret/camel-wildfly-notification-secret
oc policy add-role-to-user view system:serviceaccount:fuse:camel-wildfly-notification-sa
mvn -Popenshift fabric8:deploy
```

## Testing the code

There is a SoapUI project located in the `src/test/soapui` folder that can be used to send in requests.

You will also need to start the FakeSMTP server or configure the application's properties to point to a real SMTP server.

## Notes

There is a properties file `src/main/kube/openshift-application.properties` file that contains OpenShift secret properties used by the application. If you need to change them, you can do so and then Base64 encode the file (ie, `base64 -i src/main/kube/openshift-application.properties`), and place the output of that in the 'application.properties' section of the `src/main/kube/secret.yml` file.