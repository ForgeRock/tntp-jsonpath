/*
 * This code is to be used exclusively in connection with ForgeRock’s software or services. 
 * ForgeRock only offers ForgeRock software or services to legal entities who have entered 
 * into a binding license agreement with ForgeRock. 
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
			System.out.println("Top of try");
			NodeState nodeState = context.getStateFor(this);
			Set<String> keys = config.jpToSSMapper().keySet();
			System.out.println("keys: " + keys);
			for (Iterator<String> i = keys.iterator(); i.hasNext();) {
				// See if Key is JSON Path
				// See if Value is JSON Path
				// Do JSON.read() for both then put Value into Key

				String toSS = i.next();
				System.out.println("toSS: " + toSS);
				System.out.println("\n\n");

				String thisJPath = config.jpToSSMapper().get(toSS);
				System.out.println("thisJPath: " + thisJPath);
				System.out.println("\n\n");

//				if(thisJPath.startsWith("\"") && !thisJPath.contains("$")){
//					System.out.println("thisJPath starts with \"...");
//					nodeState.putShared(toSS,thisJPath);
//					continue;
//				}
//				if(toSS.contains("$") && thisJPath.startsWith("\"")){
//					System.out.println("Contains $ and value starts with {\"} toSS: "+ toSS + " thisJPath: " + thisJPath);
//					nodeState.putShared(toSS,thisJPath);
//					continue;
//				}
				//json.country.state = "Indiana";
				JsonValue thisJV = nodeState.get(thisJPath.substring(0, thisJPath.indexOf('.')));
				System.out.println("thisJV: " + thisJV);
				System.out.println("\n\n");

				Object document = Configuration.defaultConfiguration().jsonProvider().parse(thisJV.toString());
				System.out.println("Document: " + document);
				System.out.println("\n\n");

				Object val = JsonPath.read(document, thisJPath.substring(thisJPath.indexOf('.') + 1, thisJPath.length()));
				System.out.println("val: "+ val);
				System.out.println("\n\n");

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

			List<Outcome> outcomes = new ArrayList<>();

			ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, JSONPath.class.getClassLoader());

			Map<String,Object> keys = nodeAttributes.get("jpToSSMapper").required().asMap();
			Set<String> keySet = keys.keySet();

			for (Iterator<String> i = keySet.iterator(); i.hasNext();) {
				String toSS = i.next();

				Outcome outcome = new Outcome(toSS, toSS);
				System.out.println("outcome.id: " + outcome.id + " outcome.displayName: " + outcome.displayName);
				outcomes.add(outcome);
			}

			Outcome errorOutcome = new Outcome(ERROR, bundle.getString("ErrorOutcome"));
			outcomes.add(errorOutcome);
			return outcomes;
		}
	}
}
