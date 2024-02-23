/*
 * This code is to be used exclusively in connection with ForgeRock’s software or services. 
 * ForgeRock only offers ForgeRock software or services to legal entities who have entered 
 * into a binding license agreement with ForgeRock. 
 */

package org.forgerock.am.marketplace.jsonpath;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
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
	private static final String SUCCESS = "SUCCESS";
	private static final String ERROR = "ERROR";
	private static final String BUNDLE = JSONPath.class.getName();


    /**
     * Configuration for the node.
     */
    public interface Config {
		@Attribute(order = 100)
		Map<String, String> jpToSSMapper();	
    }

    @Inject
    public JSONPath(@Assisted Config config) {
        this.config = config;
    }

    @Override
	public Action process(TreeContext context) {
		try {
			NodeState nodeState = context.getStateFor(this);
			Set<String> keys = config.jpToSSMapper().keySet();
			String static_value = " ";

			for (Iterator<String> i = keys.iterator(); i.hasNext();) {
				String toSS = i.next();
				String thisJPath = config.jpToSSMapper().get(toSS);
				if(thisJPath.startsWith("\"")){
					nodeState.putShared(toSS,thisJPath);
					break;
				}
				JsonValue thisJV = nodeState.get(thisJPath.substring(0, thisJPath.indexOf('.')));

				Object document = Configuration.defaultConfiguration().jsonProvider().parse(thisJV.toString());

				Object val = JsonPath.read(document, thisJPath.substring(thisJPath.indexOf('.') + 1, thisJPath.length()));
				
				nodeState.putShared(toSS, val);
			}
			return Action.goTo(SUCCESS).build();
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

			List<Outcome> outcomes;
			ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, JSONPath.class.getClassLoader());

			try {
				outcomes = nodeAttributes.get("jpToSSMapper").required()
						.asList(String.class)
						.stream()
						.map(choice -> new Outcome(choice, choice))
						.collect(Collectors.toList());
			} catch (JsonValueException e) {
				outcomes = emptyList();
			}

			if (outcomes == null) outcomes = emptyList();

			Map<String,Object> keys = nodeAttributes.get("jpToSSMapper").required().asMap();
			System.out.println("new keys: " + keys);
			Set<String> keySet = keys.keySet();
			System.out.println("new keySet: " + keySet);

			for (Iterator<String> i = keySet.iterator(); i.hasNext();) {
				System.out.println("At top of for loop...");
				String toSS = i.next();
				System.out.println("After with toSS..." + toSS);
				outcomes.add(new Outcome(toSS, toSS));
				System.out.println("Inside for loop with: " + toSS);
			}

			System.out.println("Below for loop...");
			outcomes.add(new Outcome(ERROR, bundle.getString("ErrorOutcome")));
			System.out.println("Below outcomes: " + outcomes);
			return outcomes;
		}
	}
}
