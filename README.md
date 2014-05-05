# Anypoint Template: SFDC to DB User Broadcast

+ [Use Case](#usecase)
+ [Run it!](#runit)
    * [A few Considerations](#afewconsiderations)
    * [Running on premise](#runonpremise)
    * [Running on CloudHub](#runoncloudhub)
    * [Properties to be configured](#propertiestobeconfigured)
+ [API Calls](#apicalls)
+ [Customize It!](#customizeit)
    * [config.xml](#configxml)
    * [endpoints.xml](#endpointsxml)
    * [businessLogic.xml](#businesslogicxml)
    * [errorHandling.xml](#errorhandlingxml)
+ [Testing the Template](#testingthetemplate)
 

# Use Case <a name="usecase"/>
As a Salesforce admin I want to syncronize Users between Salesfoce org and database.

This Template should serve as a foundation for setting an online sync of Users from one SalesForce instance to database. Everytime there is a new User or a change in an already existing one, the integration will poll for changes in SalesForce source instance and it will be responsible for updating the User on the target database table.

What about Passwords? When the User is updated in the target instance, the password is not changed and therefore there is nothing to concern about in this case. Password set in case of User creation is not being covered by this template considering that many different approaches can be selected.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this Template leverage the [Batch Module](http://www.mulesoft.org/documentation/display/current/Batch+Processing).
The batch job is divided in Input, Process and On Complete stages.
The integration is triggered by a poll defined in the flow that is going to trigger the application, querying newest SalesForce updates/creations matching a filter criteria and executing the batch job.
During the Process stage, each SFDC User will be filtered depending on, if it has an existing matching user in the database
The last step of the Process stage will group the users and insert/update them in database.
Finally during the On Complete stage the Template will logoutput statistics data into the console.

# Run it!

Simple steps to get SFDC to Database User Broadcast running.

## A few Considerations <a name="afewconsiderations" />

There are a couple of things you should take into account before running this kick:

1. **Users cannot be deleted in SalesForce:** For now, the only thing to do regarding users removal is disabling/deactivating them, but this won't make the username available for a new user.
2. **Each user needs to be associated to a Profile:** SalesForce's profiles are what define the permissions the user will have for manipulating data and other users. Each SalesForce account has its own profiles. In this kick you will find a processor labeled *assignProfileId and Username to the User* where to map your Profile Ids from the source account to the ones in the target account. Note that for the integration test to run properly, you should change the constant *DEFAULT_PROFILE_ID* in *BusinessLogicTestIT* to one that's valid in your source test organization.
3. **Working with sandboxes for the same account**: Although each sandbox should be a completely different environment, Usernames cannot be repeated in different sandboxes, i.e. if you have a user with username *bob.dylan* in *sandbox A*, you will not be able to create another user with username *bob.dylan* in *sandbox B*. If you are indeed working with Sandboxes for the same SalesForce account you will need to map the source username to a different one in the target sandbox, for this purpose, please refer to the processor labeled *assign ProfileId and Username to the User*.

## Running on premise <a name="runonpremise"/>

In this section we detail the way you have to run you Anypoint Temple on you computer.


### Running on Studio <a name="runonstudio"/>
Once you have imported your Anypoint Template into Anypoint Studio you need to follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources
+ Complete all the properties required as per the examples in the section [Properties to be configured](#propertiestobeconfigured)
+ Add dependency for your Database driver to the pom.xml or simplt add external jar to the build path and rebuild project
+ Configure GenericDatabaseConnector in Global Elements section of the config flow to use your database specific driver. Classpath to the driver needs to be supplied here.
+ By default this template relies on existing table **"user"** in the database of your choice, so it will perform sql statements against this table, but feel free to customize prepared statements to use different database table or columns.
+ Once that is done, right click on you Anypoint Template project folder 
+ Hover you mouse over `"Run as"`
+ Click on  `"Mule Application"`


### Running on Mule ESB stand alone  <a name="runonmuleesbstandalone"/>
+ Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties)
+ Follow other steps defined [here](#runonpremise) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.
+ Once your app is all set and started, there is no need to do anything else. The application will poll SalesForce to know if there are any newly created or updated objects and synchronice them.

## Running on CloudHub <a name="runoncloudhub"/>

While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 
Follow other steps defined [here](#runonpremise) and once your app is all set and started, there is no need to do anything else. Every time a User is created or modified, it will be automatically synchronised to supplied database table as long as it has an Email.

## Properties to be configured (With examples) <a name="propertiestobeconfigured"/>
In order to use this Template you need to configure properties (Credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. Detail list with examples:

### Application configuration
+ http.port `9090` 
+ poll.frequencyMillis `60000`
+ poll.startDelayMillis `0`
+ watermark.defaultExpression `YESTERDAY`


#### SalesForce Connector configuration for company A
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.a.url `https://login.salesforce.com/services/Soap/u/28.0`

#### Dabase connection url
+ database.url=`jdbc:postgresql://localhost:5432/mule?user=postgres&password=postgres`

# API Calls <a name="apicalls"/>

SalesForce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. User Broadcast Template calls to the API can be calculated using the formula:

*** 1 ***

# Customize It!<a name="customizeit"/>

This brief guide intends to give a high level idea of how this Template is built and how you can change it according to your needs.
As mule applications are based on XML files, this page will be organized by describing all the XML that conform the Template.
Of course more files will be found such as Test Classes and [Mule Application Files](http://www.mulesoft.org/documentation/display/current/Application+Format), but to keep it simple we will focus on the XMLs.

Here is a list of the main XML files you'll find in this application:

* [config.xml](#configxml)
* [endpoints.xml](#endpointsxml)
* [businessLogic.xml](#businesslogicxml)
* [errorHandling.xml](#errorhandlingxml)


## config.xml<a name="configxml"/>
Configuration for Connectors, prepared sql statements and [Properties Place Holders](http://www.mulesoft.org/documentation/display/current/Configuring+Properties) are set in this file. **Even you can change the configuration here, all parameters that can be modified here are in properties file, and this is the recommended place to do it so.** Of course if you want to do core changes to the logic you will probably need to modify this file.

In the visual editor they can be found on the *Global Element* tab.


## businessLogic.xml<a name="businesslogicxml"/>
Functional aspect of the Template is implemented on this XML, directed by one flow that will poll for SalesForce creations/updates. The severeal message processors constitute four high level actions that fully implement the logic of this Template:

1. During the Input stage the Template will go to the SalesForce Org A and query all the existing users that match the filter criteria.
2. During the Process stage, each SFDC User will checked by email against database, if it has an existing matching user in database.
3. The choice routing element will then decide whether to perform update on selected database columns or peform insert
Finally during the On Complete stage the Template will logoutput statistics data into the console.

## endpoints.xml<a name="endpointsxml"/>
This is file is not used in this particular Template, but you'll oftenly find flows containing the inbound endpoints to start the integration.

## errorHandling.xml<a name="errorhandlingxml"/>
Contains a [Catch Exception Strategy](http://www.mulesoft.org/documentation/display/current/Catch+Exception+Strategy) that is only Logging the exception thrown (If so). As you imagine, this is the right place to handle how your integration will react depending on the different exceptions.




# Testing the Template <a name="testingthetemplate"/>

You will notice that the Template has been shipped with test.

**Consideration:** This template has only Integration Tests with a particular aspect compared to other templates. Users in SalesForce cannot be deleted in the UI or in the API so keep in mind that after running integration test, new user with some hardcoded attributes will be created in Salesforce.

You can run any of them by just doing right click on the class and clicking on run as Junit test.

Do bear in mind that you'll have to tell the test classes which property file to use.
For you convinience you can add a file mule.test.properties and locate it in "src/test/resources".
In the run configurations of the test just make sure to add the following property:

+ -Dmule.env=test
