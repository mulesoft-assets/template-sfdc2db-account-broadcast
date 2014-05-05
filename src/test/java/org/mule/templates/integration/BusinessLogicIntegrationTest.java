package org.mule.templates.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
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

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Mule Template that make calls to external systems.
 */
public class BusinessLogicIntegrationTest extends AbstractTemplateTestCase {

	protected static final int TIMEOUT = 60;
	private static final Logger log = Logger.getLogger(BusinessLogicIntegrationTest.class);
	private static final String POLL_FLOW_NAME = "triggerFlow";
	private BatchTestHelper helper;
	private Map<String, Object> user = null;

	@BeforeClass
	public static void init() {
		System.setProperty("page.size", "1000");
		System.setProperty("poll.frequencyMillis", "10000");
		System.setProperty("poll.startDelayMillis", "20000");
		System.setProperty("watermark.default.expression",
				"#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
	}

	@Before
	public void setUp() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		helper = new BatchTestHelper(muleContext);

		// prepare test data
		user = createSalesforceUser();
		insertUserSalesforce(user);
	}

	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		// delete user from Salesforce
		// user could at least flagged as inactive

		// delete previously created user from db with matching email
		Map<String, Object> usr = new HashMap<String, Object>();
		usr.put("email", user.get("Email"));
		deleteUserFromDB(usr);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMainFlow() throws Exception {
		// Run poll and wait for it to run
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();

		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();

		// Prepare payload
		final String email = (String) user.get("Email");
		final Map<String, Object> userToRetrieveMail = new HashMap<String, Object>();
		userToRetrieveMail.put("Email", email);
		log.info("userToRetrieveMail: " + userToRetrieveMail);

		// Execute selectUserFromDB sublow
		SubflowInterceptingChainLifecycleWrapper selectUserFromDBFlow = getSubFlow("selectUserFromDB");
		final MuleEvent event = selectUserFromDBFlow.process(getTestEvent(userToRetrieveMail, MessageExchangePattern.REQUEST_RESPONSE));
		final List<Map<String, Object>> payload = (List<Map<String, Object>>) event.getMessage().getPayload();

		// print result
		for (Map<String, Object> usr : payload)
			log.info("selectUserFromDB response: " + usr);

		// User previously created in Salesforce should be present in database
		Assert.assertEquals("The user should have been sync", 1, payload.size());
		Assert.assertEquals("The user email should match", email, payload.get(0).get("email"));
	}

	@SuppressWarnings("unchecked")
	private void insertUserSalesforce(Map<String, Object> user) throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("insertUserSalesforceSubFlow");
		flow.initialise();

		final MuleEvent event = flow.process(getTestEvent(user, MessageExchangePattern.REQUEST_RESPONSE));
		final List<EnrichedUpsertResult> result = (List<EnrichedUpsertResult>) event.getMessage().getPayload();

		// store Id into our user
		for (EnrichedUpsertResult item : result) {
			log.info("response from insertUserSalesforceSubFlow: " + item);
			user.put("Id", item.getId());
		}
	}

	private void deleteUserFromDB(Map<String, Object> user) throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteUserDB");
		flow.initialise();

		MuleEvent event = flow.process(getTestEvent(user, MessageExchangePattern.REQUEST_RESPONSE));
		Object result = event.getMessage().getPayload();
		log.info("deleteUserDB result: " + result);
	}

	private Map<String, Object> createSalesforceUser() {
		final String name = "tst" + buildUniqueName(5);
		final String uniqueEmail = buildUniqueEmail(name);

		SfdcObjectBuilder builder = new SfdcObjectBuilder();
		final Map<String, Object> user = builder
				.with("Email", uniqueEmail)
				.with("UserName", uniqueEmail)
				.with("LastName", name)
				.with("FirstName", name)
				.with("Alias", name)
				.with("CommunityNickname", name)

				// hardcoded defaults
				.with("LocaleSidKey", "en_US")
				.with("LanguageLocaleKey", "en_US")
				.with("TimeZoneSidKey", "America/New_York")

				// id of the chatter external user profile
				.with("ProfileId", "00ed0000000GO9T")
				.with("EmailEncodingKey", "ISO-8859-1").build();
		return user;
	}

	private String buildUniqueName(int len) {
		return RandomStringUtils.randomAlphabetic(len).toLowerCase();
	}
}
