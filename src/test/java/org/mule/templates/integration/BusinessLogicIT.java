/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.modules.salesforce.bulk.EnrichedUpsertResult;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.builders.SfdcObjectBuilder;
import org.mule.templates.db.MySQLDbCreator;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Mule Template that make calls to external systems.
 */
public class BusinessLogicIT extends AbstractTemplateTestCase {
	protected static final int TIMEOUT = 60;
	private static final Logger log = Logger.getLogger(BusinessLogicIT.class);
	private static final String POLL_FLOW_NAME = "triggerFlow";
	private static final String ACCOUNT_NAME = "Account Test Name";
	private static final String ACCOUNT_NUMBER = "123456789";
	private static final String ACCOUNT_PHONE = "+421";
	
	private BatchTestHelper helper;
	private Map<String, Object> account;
	private SubflowInterceptingChainLifecycleWrapper selectAccountFromDBFlow;
	private SubflowInterceptingChainLifecycleWrapper deleteAccountFromSalesforce;
	
	private static final String PATH_TO_TEST_PROPERTIES = "./src/test/resources/mule.test.properties";
	private static final String PATH_TO_SQL_SCRIPT = "src/main/resources/account.sql";
	private static final String DATABASE_NAME = "SFDC2DBAccountBroadcast" + new Long(new Date().getTime()).toString();
	private static final MySQLDbCreator DBCREATOR = new MySQLDbCreator(DATABASE_NAME, PATH_TO_SQL_SCRIPT, PATH_TO_TEST_PROPERTIES);

	@BeforeClass
	public static void init() {
		System.setProperty("page.size", "1000");
		System.setProperty("poll.frequencyMillis", "10000");
		System.setProperty("poll.startDelayMillis", "20000");
		System.setProperty("watermark.default.expression",
				"#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
	
		DBCREATOR.setUpDatabase();
		System.setProperty("database.url", DBCREATOR.getDatabaseUrlWithName());
	}

	@Before
	public void setUp() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		helper = new BatchTestHelper(muleContext);

		selectAccountFromDBFlow = getSubFlow("selectAccountFromDB");
		
		deleteAccountFromSalesforce = getSubFlow("deleteAccountFromSalesforce");
		deleteAccountFromSalesforce.initialise();

		// prepare test data
		account = createSalesforceAccount();
		insertSalesforceAccount(account);
	}

	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		// delete previously created account from DB by matching ID
		final Map<String, Object> acc = new HashMap<String, Object>();
		acc.put("Name", account.get("Name"));
		acc.put("Id", account.get("Id"));

		deleteAccountFromDB(acc);
		deleteAccountFromSalesforce(acc);
		
		DBCREATOR.tearDownDataBase();
	}

	@Test
	public void testMainFlow() throws Exception {
		// Run poll and wait for it to run
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();

		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();

		// Execute selectAccountFromDB sublow
		final MuleEvent event = selectAccountFromDBFlow.process(getTestEvent(account, MessageExchangePattern.REQUEST_RESPONSE));
		final List<Map<String, Object>> payload = (List<Map<String, Object>>) event.getMessage().getPayload();

		// print result
		for (Map<String, Object> acc : payload){
			log.info("selectAccountFromDB response: " + acc);
		}

		// Account previously created in Salesforce should be present in database
		Assert.assertEquals("The account should have been sync", 1, payload.size());
		Assert.assertEquals("The account name should match", account.get("Id"), payload.get(0).get("salesforceId"));
		Assert.assertEquals("The account name should match", account.get("Name"), payload.get(0).get("name"));
		Assert.assertEquals("The account number should match", account.get("AccountNumber"), payload.get(0).get("accountNumber"));
	}

	@SuppressWarnings("unchecked")
	private void insertSalesforceAccount(final Map<String, Object> account) throws Exception {
		final SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("insertSalesforceAccountSubFlow");
		flow.initialise();

		final MuleEvent event = flow.process(getTestEvent(account, MessageExchangePattern.REQUEST_RESPONSE));
		final List<EnrichedUpsertResult> result = (List<EnrichedUpsertResult>) event.getMessage().getPayload();

		// store Id into our account
		for (EnrichedUpsertResult item : result) {
			log.info("response from insertSalesforceAccountSubFlow: " + item);
			account.put("Id", item.getId());
			account.put("LastModifiedDate", item.getPayload().getField("LastModifiedDate"));
		}
	}

	private void deleteAccountFromSalesforce(final Map<String, Object> acc) throws Exception {

		List<Object> idList = new ArrayList<Object>();
		idList.add(acc.get("Id"));

		deleteAccountFromSalesforce.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	private void deleteAccountFromDB(final Map<String, Object> account) throws Exception {
		final SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteAccountFromDB");
		flow.initialise();

		final MuleEvent event = flow.process(getTestEvent(account, MessageExchangePattern.REQUEST_RESPONSE));
		final Object result = event.getMessage().getPayload();
		log.info("deleteAccountFromDB result: " + result);
	}

	private Map<String, Object> createSalesforceAccount() {
		final SfdcObjectBuilder builder = new SfdcObjectBuilder();
		final Map<String, Object> account = builder
				.with("Name", ACCOUNT_NAME + System.currentTimeMillis())
				.with("AccountNumber", ACCOUNT_NUMBER)
				.with("Phone", ACCOUNT_PHONE).build();
		
		return account;
	}
}
