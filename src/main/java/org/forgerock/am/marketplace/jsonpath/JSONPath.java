/*
 * This code is to be used exclusively in connection with Ping Identity Corporation software or services.
 * Ping Identity Corporation only offers such software or services to legal entities who have entered
 * into a binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.am.marketplace.jsonpath;

import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import static java.util.Collections.emptyList;


@Node.Metadata(	outcomeProvider	= JSONPath.JSONPathOutcomeProvider.class, 
				tags			= { "marketplace", "trustnetwork"},
				configClass		= JSONPath.Config.class)

public class JSONPath extends AbstractDecisionNode {

    private final Logger logger = LoggerFactory.getLogger(JSONPath.class);
    private final Config config;
	private String loggerPrefix = "[JSONPath]" + JSONPathPlugin.logAppender;
	private static final String NEXT = "NEXT";
	private static final String ERROR = "ERROR";
	private static final String BUNDLE = JSONPath.class.getName();


    /**
     * Configuration for the node.
     */
    public interface Config {
		@Attribute(order = 100)
			Map<String, String> insertToSS();

		@Attribute(order = 200)
			Map<String, String> jpToOutcomeMapper();
    }

    @Inject
    public JSONPath(@Assisted Config config) {
        this.config = config;
    }

    @Override
	public Action process(TreeContext context) {
		try {
			NodeState nodeState = context.getStateFor(this);
			//Insert into SS
			Set<String> keys = config.insertToSS().keySet();

			for (Iterator<String> i = keys.iterator(); i.hasNext();) {
				String toSS = i.next();
				String val = config.insertToSS().get(toSS);
				nodeState.putShared(toSS, val);
			}

			Set<String> Jkeys = config.jpToOutcomeMapper().keySet();
			int matches = 0;
			for (Iterator<String> i = Jkeys.iterator(); i.hasNext();) {
				String toSS = i.next();
				String thisJPath = config.jpToOutcomeMapper().get(toSS);
				JsonValue thisJV = nodeState.get(thisJPath.substring(0, thisJPath.indexOf('.')));
				Object document = Configuration.defaultConfiguration().jsonProvider().parse(thisJV.toString());
				Object val = JsonPath.read(document, thisJPath.substring(thisJPath.indexOf('.') + 1, thisJPath.length()));

				if(val != null){
					matches = matches + 1;
				}

				nodeState.putShared(toSS, val);

			}
			String outcome = calculateOutcome(config.jpToOutcomeMapper(), context);
			logger.debug("Outcome: " + outcome);
			if(matches > 1){
				logger.error("More than one filter matched. Going to error...");
				outcome = ERROR;
			}
			return Action.goTo(outcome).build();
		} catch (Exception ex) {
			String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
			logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
			context.getStateFor(this).putShared(loggerPrefix + "Exception", new Date() + ": " + ex.getMessage());
			context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
			return Action.goTo(ERROR).withHeader("Error occurred").withErrorMessage(ex.getMessage()).build();
		}
	}


	/**
	 * Defines the possible outcomes from this node.
	 */
	public static class JSONPathOutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
		@Override
		public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
			List<Outcome> outcomes = new ArrayList<>();
			ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, JSONPath.class.getClassLoader());

			try {
				outcomes = nodeAttributes.get("jpToOutcomeMapper").required()
						.asList(String.class)
						.stream()
						.map(choice -> new Outcome(choice, choice))
						.collect(Collectors.toList());
			} catch (JsonValueException e) {
				outcomes =  new ArrayList<>();
			}

			if (outcomes == null) outcomes = new ArrayList<>();

			if (nodeAttributes!= null && nodeAttributes.get("jpToOutcomeMapper")!=null &&  nodeAttributes.get("jpToOutcomeMapper").isNotNull()) {
				Map<String, Object> keys = nodeAttributes.get("jpToOutcomeMapper").required().asMap();
				Set<String> keySet = keys.keySet();
				for (Iterator<String> i = keySet.iterator(); i.hasNext();) {
					String toSS = i.next();
					outcomes.add(new Outcome(toSS, toSS));
				}
			}
			outcomes.add(new Outcome(NEXT, bundle.getString("NextOutcome")));
			outcomes.add(new Outcome(ERROR, bundle.getString("ErrorOutcome")));

			return outcomes;
		}
	}

private String calculateOutcome(Map <String,String> outcomes,  TreeContext context) {
	String result = null;
	NodeState nodeState = context.getStateFor(this);
	Set<String> Jkeys = config.jpToOutcomeMapper().keySet();

	for (Iterator<String> i = Jkeys.iterator(); i.hasNext();) {
		String toSS = i.next();

		String thisJPath = config.jpToOutcomeMapper().get(toSS);

		JsonValue thisJV = nodeState.get(thisJPath.substring(0, thisJPath.indexOf('.')));

		Object document = Configuration.defaultConfiguration().jsonProvider().parse(thisJV.toString());

		List<String> vals = JsonPath.read(document, thisJPath.substring(thisJPath.indexOf('.') + 1, thisJPath.length()));

		if (vals.size() > 0) {
			result = toSS;
			break;  // Exit at first matching outcome
		}
	}

	if (result == null)
		return "NEXT";

	return result;
}
}

