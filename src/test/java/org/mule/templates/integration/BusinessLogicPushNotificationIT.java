/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.db.MySQLDbCreator;
import org.mule.transformer.types.DataTypeFactory;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the
 * Anypoint Template that make calls to external systems.
 * 
 * @author Vlado Andoga
 */
@SuppressWarnings("unchecked")
public class BusinessLogicPushNotificationIT extends AbstractTemplateTestCase {
	
	private static final String PATH_TO_TEST_PROPERTIES = "./src/test/resources/mule.test.properties";
	private static final String PATH_TO_SQL_SCRIPT = "src/main/resources/account.sql";
	private static final String DATABASE_NAME = "SFDC2DBAccountBroadcast" + new Long(new Date().getTime()).toString();
	private static final MySQLDbCreator DBCREATOR = new MySQLDbCreator(DATABASE_NAME, PATH_TO_SQL_SCRIPT, PATH_TO_TEST_PROPERTIES);
	private static final int TIMEOUT_MILLIS = 60;
	
	private BatchTestHelper helper;
	private Flow triggerPushFlow;
	private SubflowInterceptingChainLifecycleWrapper selectAccountFromDBFlow;

	@BeforeClass
	public static void beforeClass() {
		DBCREATOR.setUpDatabase();
		System.setProperty("trigger.policy", "push");
		System.setProperty("database.url", DBCREATOR.getDatabaseUrlWithName());
	}

	@AfterClass
	public static void shutDown() {
		System.clearProperty("trigger.policy");
		DBCREATOR.tearDownDataBase();
	}

	@Before
	public void setUp() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		helper = new BatchTestHelper(muleContext);
		triggerPushFlow = getFlow("triggerPushFlow");
		selectAccountFromDBFlow = getSubFlow("selectAccountFromDB");
	}

	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
	}

	/**
	 * In test, we are creating new SOAP message to create an account. Account name is always generated
	 * to ensure, that flow correctly creates an account in the db. 
	 * @throws Exception when flow error occurred
	 */
	@Test
	public void testMainFlow() throws Exception {
		// Execution
		String accountName = buildUniqueName();
		HashMap<String, Object> account = new HashMap<String, Object>();
		account.put("Name", accountName);
				
		final MuleEvent testEvent = getTestEvent(null, triggerPushFlow);
		testEvent.getMessage().setPayload(buildRequest(accountName), DataTypeFactory.create(InputStream.class, "application/xml"));
		triggerPushFlow.process(testEvent);
		
		helper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
		helper.assertJobWasSuccessful();

		// Execute selectAccountFromDB sublow
		MuleEvent event = selectAccountFromDBFlow.process(getTestEvent(account, MessageExchangePattern.REQUEST_RESPONSE));
		List<Map<String, Object>> payload = (List<Map<String, Object>>) event.getMessage().getPayload();
		
		// Assertions
		Assert.assertNotNull(payload);
		Assert.assertEquals("Account Names should be equals", account.get("Name"), payload.get(0).get("name"));		
	}
	

	/**
	 * Builds the soap request as a string.
	 * @param name the name
	 * @return a soap message as string
	 */
	private String buildRequest(String accountName){
		StringBuilder req = new StringBuilder();
		req.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		req.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
		req.append(" <soapenv:Body>");
		req.append("  <notifications xmlns=\"http://soap.sforce.com/2005/09/outbound\">");
		req.append("   <OrganizationId>00Dd0000000dtDqEAI</OrganizationId>");
		req.append("   <ActionId>04kd0000000PCgvAAG</ActionId>");
		req.append("   <SessionId xsi:nil=\"true\"/>");
		req.append("   <EnterpriseUrl>https://na14.salesforce.com/services/Soap/c/30.0/00Dd0000000dtDq</EnterpriseUrl>");
		req.append("   <PartnerUrl>https://na14.salesforce.com/services/Soap/u/30.0/00Dd0000000dtDq</PartnerUrl>");
		req.append("   <Notification>");
		req.append("    <Id>04ld000000TzMKpAAN</Id>");
		req.append("    <sObject xsi:type=\"sf:Account\" xmlns:sf=\"urn:sobject.enterprise.soap.sforce.com\">");
		req.append("     <sf:Id>001d000001XD5XKAA1</sf:Id>");
		req.append("     <sf:AccountNumber>4564564</sf:AccountNumber>");
		req.append("     <sf:AnnualRevenue>10000.0</sf:AnnualRevenue>");
		req.append("     <sf:BillingCity>City</sf:BillingCity>");
		req.append("     <sf:BillingCountry>Country</sf:BillingCountry>");
		req.append("     <sf:BillingPostalCode>04001</sf:BillingPostalCode>");
		req.append("     <sf:BillingState>State</sf:BillingState>");
		req.append("     <sf:BillingStreet>Street</sf:BillingStreet>");
		req.append("     <sf:CreatedById>005d0000000yYC7AAM</sf:CreatedById>");
		req.append("     <sf:CreatedDate>2014-05-05T11:47:49.000Z</sf:CreatedDate>");
		req.append("     <sf:CustomerPriority__c>High</sf:CustomerPriority__c>");
		req.append("     <sf:Description>description</sf:Description>");
		req.append("     <sf:Fax>+421995555</sf:Fax>");
		req.append("     <sf:Industry>Apparel</sf:Industry>");
		req.append("     <sf:IsDeleted>false</sf:IsDeleted>");
		req.append("     <sf:LastModifiedById>005d0000000yYC7AAM</sf:LastModifiedById>");
		req.append("     <sf:LastModifiedDate>2014-06-02T13:00:00.000Z</sf:LastModifiedDate>");
		req.append("     <sf:LastReferencedDate>2014-05-19T11:02:14.000Z</sf:LastReferencedDate>");
		req.append("     <sf:LastViewedDate>2014-05-19T11:02:14.000Z</sf:LastViewedDate>");
		req.append("     <sf:Name>"+accountName+"</sf:Name>");
		req.append("     <sf:NumberOfEmployees>5000</sf:NumberOfEmployees>");
		req.append("     <sf:OwnerId>005d0000000yYC7AAM</sf:OwnerId>");
		req.append("     <sf:Ownership>Public</sf:Ownership>");
		req.append("     <sf:Phone>+421995555</sf:Phone>");
		req.append("     <sf:PhotoUrl>/services/images/photo/001d000001XD5XKAA1</sf:PhotoUrl>");
		req.append("     <sf:Rating>Hot</sf:Rating>");
		req.append("     <sf:SLA__c>Gold</sf:SLA__c>");
		req.append("     <sf:ShippingCity>Shipping City</sf:ShippingCity>");
		req.append("     <sf:ShippingCountry>Country</sf:ShippingCountry>");
		req.append("     <sf:ShippingPostalCode>04001</sf:ShippingPostalCode>");
		req.append("     <sf:ShippingState>Shipping State</sf:ShippingState>");
		req.append("     <sf:ShippingStreet>Shipping street</sf:ShippingStreet>");
		req.append("     <sf:Site>http://www.test.com</sf:Site>");
		req.append("     <sf:SystemModstamp>2014-05-19T11:02:14.000Z</sf:SystemModstamp>");
		req.append("     <sf:Type>Prospect</sf:Type>");
		req.append("     <sf:Website>http://www.test.com</sf:Website>");
		req.append("    </sObject>");
		req.append("   </Notification>");
		req.append("  </notifications>");
		req.append(" </soapenv:Body>");
		req.append("</soapenv:Envelope>");
		return req.toString();
	}
	
	/**
	 * Builds unique name based on current time stamp.
	 * @return a unique name as string
	 */
	private String buildUniqueName() {
		return TEMPLATE_NAME + "-" + System.currentTimeMillis() + "Account";
	}
	
}
