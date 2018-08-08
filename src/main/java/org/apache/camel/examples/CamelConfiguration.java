/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.properties.PropertiesComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ContextName("camel-context")
public class CamelConfiguration extends RouteBuilder {

  private static final Logger log = LoggerFactory.getLogger(CamelConfiguration.class);

  @Inject
  @ContextName("camel-context")
  private CamelContext camelContext;
  
  @PostConstruct
  private void initializePropertyPlaceholder() {
    PropertiesComponent properties = camelContext.getComponent("properties", PropertiesComponent.class);

    List<String> propertyLocations = new ArrayList<>();
    propertyLocations.add("classpath:/application.properties;optional=true");
    propertyLocations.add("file:${user.home}/application.properties;optional=true");
    propertyLocations.add("file:${camel.config.location}/application.properties;optional=true");
    if (System.getProperty("camel.config.locations") != null) {
      for (String location : System.getProperty("camel.config.locations").split(",")) {
        propertyLocations.add("file:" + location + "/application.properties;optional=true");
      }
    }
    propertyLocations.add("file:${env:CAMEL_CONFIG_LOCATION}/application.properties;optional=true");
    if (System.getenv("CAMEL_CONFIG_LOCATIONS") != null) {
      for (String location : System.getenv("CAMEL_CONFIG_LOCATIONS").split(",")) {
        propertyLocations.add("file:" + location + "/application.properties;optional=true");
      }
    }
    properties.setLocations(propertyLocations);
    
    Properties overrideProperties = new Properties();
    overrideProperties.putAll(System.getenv());
    overrideProperties.putAll(System.getProperties());
    properties.setOverrideProperties(overrideProperties);
  }
  
  @Produces
  @Named("jmsConnectionFactory")
  private ConnectionFactory jmsConnectionFactory() throws NamingException {
    InitialContext context = null;
    try {
      context = new InitialContext();
      return (ConnectionFactory) context.lookup("java:/ConnectionFactory");
    } finally {
      if (context != null) context.close();
    }
  }
  
  @Override
  public void configure() throws Exception {
    
    from("direct:sendNotification")
      .log(LoggingLevel.INFO, log, "Received a notification: [${body}]")
      .setHeader("NotificationRecipientList").groovy("request.body?.recipients?.join(';')")
      .setBody().simple("${body.notification}")
      .to(ExchangePattern.InOnly, "jms:queue:NotificationQueue?connectionFactory=#jmsConnectionFactory")
    ;
    
    from("jms:queue:NotificationQueue?connectionFactory=#jmsConnectionFactory&acknowledgementModeName=CLIENT_ACKNOWLEDGE&disableReplyTo=true")
      .log(LoggingLevel.INFO, log, "Sending notification: recipients=[${header.NotificationRecipientList}], notification=[${body}]")
      .removeHeaders(".*", "Notification.*")
      .setHeader("To").header("NotificationRecipientList")
      .to("{{camel.mail.protocol}}:{{camel.mail.host}}:{{camel.mail.port}}?username=RAW({{camel.mail.username}})&password=RAW({{camel.mail.password}})&from=RAW({{camel.mail.from}})&subject=RAW({{camel.mail.subject}})")
    ;
  }
}
