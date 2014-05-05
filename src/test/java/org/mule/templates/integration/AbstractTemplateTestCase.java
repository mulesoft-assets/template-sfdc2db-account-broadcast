package org.mule.templates.integration;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Rule;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.context.notification.NotificationException;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.templates.builders.SfdcObjectBuilder;
import org.mule.templates.test.utils.ListenerProbe;
import org.mule.templates.test.utils.PipelineSynchronizeListener;
import org.mule.transport.NullPayload;

/**
 * This is the base test class for Anypoint Templates integration tests.
 * 
 * @author damiansima
 */
public class AbstractTemplateTestCase extends FunctionalTestCase {
	private static final String MAPPINGS_FOLDER_PATH = "./mappings";
	private static final String TEST_FLOWS_FOLDER_PATH = "./src/test/resources/flows/";
	private static final String MULE_DEPLOY_PROPERTIES_PATH = "./src/main/app/mule-deploy.properties";

	protected static final int TIMEOUT_SEC = 120;
	protected static final String POLL_FLOW_NAME = "triggerFlow";
	protected static final String TEMPLATE_NAME = "user-broadcast";

	protected final Prober pollProber = new PollingProber(60000, 1000l);
	protected final PipelineSynchronizeListener pipelineListener = new PipelineSynchronizeListener(POLL_FLOW_NAME);

	protected SubflowInterceptingChainLifecycleWrapper retrieveContactFromBFlow;
	protected SubflowInterceptingChainLifecycleWrapper retrieveAccountFlowFromB;

	@Rule
	public DynamicPort port = new DynamicPort("http.port");

	@Override
	protected String getConfigResources() {
		String resources = "";
		try {
			Properties props = new Properties();
			props.load(new FileInputStream(MULE_DEPLOY_PROPERTIES_PATH));
			resources = props.getProperty("config.resources");
		} catch (Exception e) {
			throw new IllegalStateException(
					"Could not find mule-deploy.properties file on classpath. Please add any of those files or override the getConfigResources() method to provide the resources by your own.");
		}

		return resources + getTestFlows();
	}

	protected String getTestFlows() {
		StringBuilder resources = new StringBuilder();

		File testFlowsFolder = new File(TEST_FLOWS_FOLDER_PATH);
		File[] listOfFiles = testFlowsFolder.listFiles();
		if (listOfFiles != null) {
			for (File f : listOfFiles) {
				if (f.isFile() && f.getName()
									.endsWith("xml")) {
					resources.append(",")
								.append(TEST_FLOWS_FOLDER_PATH)
								.append(f.getName());
				}
			}
			return resources.toString();
		} else {
			return "";
		}
	}

	@Override
	protected Properties getStartUpProperties() {
		Properties properties = new Properties(super.getStartUpProperties());

		String pathToResource = MAPPINGS_FOLDER_PATH;
		File graphFile = new File(pathToResource);

		properties.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, graphFile.getAbsolutePath());

		return properties;
	}

	protected void registerListeners() throws NotificationException {
		muleContext.registerListener(pipelineListener);
	}

	protected void waitForPollToRun() {
		System.out.println("Waiting for poll to run ones...");
		pollProber.check(new ListenerProbe(pipelineListener));
		System.out.println("Poll flow done");
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> invokeRetrieveFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, Object> payload) throws Exception {
		MuleEvent event = flow.process(getTestEvent(payload, MessageExchangePattern.REQUEST_RESPONSE));
		Object resultPayload = event.getMessage()
									.getPayload();

		if (resultPayload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, Object>) resultPayload;
		}
	}

	protected Map<String, Object> createContact(String orgId, int sequence) {
		return SfdcObjectBuilder.aContact()
								.with("FirstName", "FirstName_" + sequence)
								.with("LastName", buildUniqueName(TEMPLATE_NAME, "LastName_" + sequence + "_"))
								.with("Email", buildUniqueEmail("some.email." + sequence))
								.with("Description", "Some fake description")
								.with("MailingCity", "Denver")
								.with("MailingCountry", "US")
								.with("MobilePhone", "123456789")
								.with("Department", "department_" + sequence + "_" + orgId)
								.with("Phone", "123456789")
								.with("Title", "Dr")
								.build();
	}

	protected void deleteTestContactsFromSandBoxA(List<Map<String, Object>> createdContactsInA) throws InitialisationException, MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper deleteContactFromAFlow = getSubFlow("deleteContactFromAFlow");
		deleteContactFromAFlow.initialise();

		deleteTestEntityFromSandBox(deleteContactFromAFlow, createdContactsInA);
	}

	protected void deleteTestContactsFromSandBoxB(List<Map<String, Object>> createdContactsInA) throws InitialisationException, MuleException, Exception {
		List<Map<String, Object>> createdContactsInB = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> c : createdContactsInA) {
			Map<String, Object> contact = invokeRetrieveFlow(retrieveContactFromBFlow, c);
			if (contact != null) {
				createdContactsInB.add(contact);
			}
		}
		SubflowInterceptingChainLifecycleWrapper deleteContactFromBFlow = getSubFlow("deleteContactFromBFlow");
		deleteContactFromBFlow.initialise();
		deleteTestEntityFromSandBox(deleteContactFromBFlow, createdContactsInB);
	}

	protected void deleteTestAccountsFromSandBoxA(List<Map<String, Object>> createdAccountsInA) throws InitialisationException, MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper deleteAccountFromAFlow = getSubFlow("deleteAccountFromAFlow");
		deleteAccountFromAFlow.initialise();

		deleteTestEntityFromSandBox(deleteAccountFromAFlow, createdAccountsInA);
	}

	protected void deleteTestAccountsFromSandBoxB(List<Map<String, Object>> createdAccountsInA) throws InitialisationException, MuleException, Exception {
		List<Map<String, Object>> createdAccountsInB = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> a : createdAccountsInA) {
			Map<String, Object> account = invokeRetrieveFlow(retrieveAccountFlowFromB, a);
			if (account != null) {
				createdAccountsInB.add(account);
			}
		}

		SubflowInterceptingChainLifecycleWrapper deleteAccountFromBFlow = getSubFlow("deleteAccountFromBFlow");
		deleteAccountFromBFlow.initialise();
		deleteTestEntityFromSandBox(deleteAccountFromBFlow, createdAccountsInB);
	}

	protected void deleteTestEntityFromSandBox(SubflowInterceptingChainLifecycleWrapper deleteFlow, List<Map<String, Object>> entitities) throws MuleException, Exception {
		List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : entitities) {
			idList.add(c.get("Id")
						.toString());
		}
		deleteFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	protected String buildUniqueName(String templateName, String name) {
		String timeStamp = new Long(new Date().getTime()).toString();

		StringBuilder builder = new StringBuilder();
		builder.append(name);
		builder.append(templateName);
		builder.append(timeStamp);

		return builder.toString();
	}

	protected String buildUniqueEmail(String user) {
		String server = "fakemail";
		String timeStamp = new Long(new Date().getTime()).toString();

		StringBuilder builder = new StringBuilder();
		builder.append(user);
		builder.append(".");
		builder.append(timeStamp);
		builder.append("@");
		builder.append(server);
		builder.append(TEMPLATE_NAME);
		builder.append(".com");

		return builder.toString();

	}

}