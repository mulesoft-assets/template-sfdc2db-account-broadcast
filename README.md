
# Anypoint Template: Salesforce to Database Account Broadcast	

<!-- Header (start) -->

<!-- Header (end) -->

# License Agreement
This template is subject to the conditions of the 
<a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>.
Review the terms of the license before downloading and using this template. You can use this template for free 
with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case
<!-- Use Case (start) -->
As a Salesforce admin I want to synchronize Accounts from Salesforce to Database.

This Template should serve as a foundation for setting an online sync of Accounts from one Salesforce instance to database. Every time there is new Account or a change in an already existing one, the integration will poll for changes in Salesforce source instance and it will be responsible for updating the Account on the target database table.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this Anypoint Template leverages the [Batch Module](http://www.mulesoft.org/documentation/display/current/Batch+Processing) and [Outbound messaging](https://www.salesforce.com/us/developer/docs/api/Content/sforce_api_om_outboundmessaging.htm)
The batch job is divided into Process and On Complete stages.
The integration is triggered by a scheduler defined in the flow that is going to trigger the application, querying newest Salesforce updates/creations matching a filter criteria and executing the batch job.
During the Process stage, each Salesforce Account will be filtered depending on, if it has an existing matching account in the database
The last step of the Process stage will group the Accounts and insert/update them in database.
Finally during the On Complete stage the Template will log output statistics data into the console.

The integration could be also triggered by HTTP inbound connector defined in the flow that is going to trigger the application and executing the batch job with received message from Salesforce source instance.
Outbound messaging in Salesforce allows you to specify that changes to fields within Salesforce can cause messages with field values to be sent to designated external servers.
Outbound messaging is part of the workflow rule functionality in Salesforce. Workflow rules watch for specific kinds of field changes and trigger automatic Salesforce actions. In this case sending accounts as an outbound message to Mule Http inbound connector,
which will then further process this message and create Accounts in a database.
<!-- Use Case (end) -->

# Considerations

<!-- Considerations (start) -->
To make this Anypoint Template run, there are certain preconditions that must be considered. All of them deal with the preparations in both source (Salesforce) and destination (Database) systems, that must be made in order for all to run smoothly. 
**Failling to do so could lead to unexpected behavior of the template.**

This particular Anypoint Template illustrate the broadcast use case between Salesforce and a Database, thus it requires a Database instance to work.
The Anypoint Template comes packaged with a SQL script to create the Database table that uses. It is the user responsability to use that script to create the table in an available schema and change the configuration accordingly. The SQL script file can be found in [src/main/resources/account.sql] (../master/src/main/resources/account.sql)
<!-- Considerations (end) -->

## DB Considerations

To get this template to work:

This template may use date time or timestamp fields from the database to do comparisons and take further actions.
While the template handles the time zone by sending all such fields in a neutral time zone, it cannot handle time offsets.
We define time offsets as the time difference that may surface between date time and timestamp fields from different systems due to a differences in the system's internal clock.
Take this in consideration and take the actions needed to avoid the time offset.


### As a Data Destination

There are no considerations with using a database as a data destination.

## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to use a custom field, be sure to append the '__c' after the custom field name. Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```











# Run it!
Simple steps to get Salesforce to Database Account Broadcast running.
<!-- Run it (start) -->
See below.
<!-- Run it (end) -->

## Running On Premises
In this section we help you run your template on your computer.
<!-- Running on premise (start) -->

<!-- Running on premise (end) -->

### Where to Download Anypoint Studio and the Mule Runtime
If you are a newcomer to Mule, here is where to get the tools.

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)
<!-- Where to download (start) -->

<!-- Where to download (end) -->

### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.
<!-- Importing into Studio (start) -->

<!-- Importing into Studio (end) -->

### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`
+ Click `Mule Application (configure)`
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
+ Click `Run`
<!-- Running on Studio (start) -->
Once you have imported your Anypoint Template into Anypoint Studio you need to follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources
+ Fill out all the properties required as per the examples in the section [Properties to be configured](#propertiestobeconfigured)
+ Add dependency for your Database driver to the pom.xml or simply add external jar to the build path and rebuild project
+ Configure GenericDatabaseConnector in Global Elements section of the config flow to use your database specific driver. Classpath to the driver needs to be supplied here.
+ By default this template relies on existing table **"Account"** in the database of your choice, so it will perform sql statements against this table, but feel free to customize prepared statements to use different database table or columns.
+ Once that is done, right click on you Anypoint Template project folder 
+ Hover you mouse over `"Run as"`
+ Click on  `"Mule Application"`
<!-- Running on Studio (end) -->

### Running on Mule Standalone
Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 


## Running on CloudHub
While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.
<!-- Running on Cloudhub (start) -->
While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 
Follow other steps defined [here](#runonpremise) and once your app is all set and started, there is no need to do anything else. Every time a Account is created or modified, it will be automatically synchronised to supplied database table.

Once your app is all set and started and switched for outbound messaging processing, you will need to define Salesforce outbound messaging and a simple workflow rule. [This article will show you how to accomplish this](https://www.salesforce.com/us/developer/docs/api/Content/sforce_api_om_outboundmessaging_setting_up.htm)
The most important setting here is the `Endpoint URL` which needs to point to your application running on Cloudbhub, eg. `http://yourapp.cloudhub.io:80`. Additionaly, try to add just few fields to the `Fields to Send` to keep it simple for begin.
Once this all is done every time when you will make a change on Account in source Salesforce org. This account will be sent as a SOAP message to the Http endpoint of running application in Cloudhub.
<!-- Running on Cloudhub (end) -->

### Deploying your Anypoint Template on CloudHub
Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this
<!-- Deploying on Cloudhub (start) -->

<!-- Deploying on Cloudhub (end) -->

## Properties to Configure
To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
<!-- Application Configuration (start) -->
**Application configuration**

+ http.port `9090`
+ page.size `200`
+ scheduler.frequency `10000`
+ scheduler.startDelay `100`
+ watermark.default.expression `YESTERDAY`
+ trigger.policy `push` | `poll`

**Database Connector configuration**

+ db.host `localhost`
+ db.port `3306`
+ db.user `user-name1`
+ db.password `user-password1`
+ db.databasename `dbname1`

If it is required to connect to a different Database there should be provided the jar for the library and changed the value of that field in the connector.

**Salesforce Connector configuration**

+ sfdc.a.username `joan.baez@orgb`
+ sfdc.a.password `JoanBaez456`
+ sfdc.a.securityToken `ces56arl7apQs56XTddf34X`
<!-- Application Configuration (end) -->

# API Calls
<!-- API Calls (start) -->
Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. Account Broadcast Template calls to the API can be calculated using the formula:

***1 + X + X / 200***

Being ***X*** the number of Accounts to be synchronized on each run. 

The division by ***200*** is because, by default, Accounts are gathered in groups of 200 for each Upsert API Call in the commit step. Also consider that this calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 api calls will be made (1 + 10 + 1).
<!-- API Calls (end) -->

# Customize It!
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml<!-- Customize it (start) -->

<!-- Customize it (end) -->

## config.xml
Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the *Global Element* tab.
<!-- Config XML (start) -->

<!-- Config XML (end) -->

## businessLogic.xml
Functional aspect of the Anypoint Template is implemented on this XML, directed by a batch job that will be responsible for creations/updates. The several message processors constitute four high level actions that fully implement the logic of this Anypoint Template:

1. During the Input stage the Template will query Salesforce for all the existing Accounts that match the filtering criteria.
2. During the Process stage, each Salesforce Account will be checked by name against the Database, if it has an existing matching objects in database.
3. The choice routing element will then decide whether to perform update operation on selected database columns or to perform insert operation
4. Finally during the On Complete stage the Template will log output statistics data into the console.
<!-- Business Logic XML (start) -->

<!-- Business Logic XML (end) -->

## endpoints.xml
This file is conformed by three Flows.

The first one we'll call it **push** flow. This one contains an HTTP endpoint that will be listening for Saleforce's outbound messages. Each of them will be processed and thus update/create Accounts, and then executing the batch job process.

The second one we'll call it **scheduler** flow. This one contains the Scheduler endpoint that will periodically trigger **sfdcQuery** flow and then executing the batch job process.

The third one we'll call it **sfdcQuery** flow. This one contains watermarking logic that will be querying Salesforce for updated/created Accounts that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored by using Objectstore Component and updated after each Salesforce query.

The property **trigger.policy** is the one in charge of defining from which endpoint the Template will receive the data. When the push policy is selected, the http inbound connector is used for Salesforce's outbound messaging and polling mechanism is ignored. The property can only assume one of two values `push` or `poll` any other value will result in the Template ignoring all messages.
<!-- Endpoints XML (start) -->

<!-- Endpoints XML (end) -->

## errorHandling.xml
This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.
<!-- Error Handling XML (start) -->

<!-- Error Handling XML (end) -->

<!-- Extras (start) -->

<!-- Extras (end) -->
