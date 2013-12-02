import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import bookTrading.IO.interpreter.BookAgentInterpreter;
import bookTrading.IO.interpreter.BookBuyerInterpreter;
import bookTrading.IO.interpreter.BookSellerInterpreter;


/**
 * Command-line arguments:
 * 	-agentcontainer
 * 		if this is specified, run the platform as an agent container and
 * 		connect to another main container; otherwise, run as a main container.
 * 	-mainhost [hostname]		
 * 		the host name of the MAIN container to connect to (default is
 * 		localhost)
 * 	-mainport [port]
 * 		the port number of the MAIN container to connect to (default 1099)
 *	-buyers [buyer1;buyer2]
 *		any buyer agents to instantiate (separated by semi-colons)
 *	-sellers [seller1;seller2]
 *		any seller agents to instantiate (separated by semi-colons)
 * @author peter
 *
 */
public class AgentEnvironment {
	// arguments
	public static final String S_AGENTCONTAINER = "ac";
	public static final String L_AGENTCONTAINER = "agentcontainer";
	public static final String S_MAINHOST = "mh";
	public static final String L_MAINHOST = "mainhost";
	public static final String S_MAINPORT = "mp";
	public static final String L_MAINPORT = "mainport";
	public static final String S_BUYERS = "b";
	public static final String L_BUYERS = "buyers";
	public static final String S_SELLERS = "s";
	public static final String L_SELLERS = "sellers";
	// default values
	public static boolean AGENTCONTAINER = false;
	public static String MAINHOST = "localhost";
	public static String MAINPORT = "1099";
	public static List<String> BUYERS = new ArrayList<String>();
	public static List<String> SELLERS = new ArrayList<String>();
	
	public static Options defineOptions() {
		// create Options object
		Options options = new Options();

		// add the options
		options.addOption(
					S_AGENTCONTAINER,
					L_AGENTCONTAINER,
					false,
					"run the platform as an agent container (rather than a main container)"
				);
		options.addOption(
					S_MAINHOST,
					L_MAINHOST,
					true,
					"the host name of the main container to connect to (default is " + MAINHOST + ")"
				);
		options.addOption(
					S_MAINPORT,
					L_MAINPORT,
					true,
					"the port number of the main container to connect to (default is " + MAINPORT + ")"
				);
		options.addOption(
					S_BUYERS,
					L_BUYERS,
					true,
					"any buyer agents to instantiate (separated by ;) - eg. buyer1;buyer2;buyer3"
				);
		options.addOption(
					S_SELLERS,
					L_SELLERS,
					true,
					"any seller agents to instantiate (separated by ;) - eg. seller1;seller2;seller3"
				);
		
		// return these options
		return options;
	}
	public static CommandLine parseArguments(Options options, String args[]) throws ParseException {
		// very simply, parse the arguments and return an object containing them
		CommandLineParser parser = new BasicParser();
		return parser.parse(options, args);
	}
	public static void interpretArguments(CommandLine cl) {
		// if the agent container argument is there then run that way
		AGENTCONTAINER = cl.hasOption(S_AGENTCONTAINER);
		// host and port stuff
		if(cl.hasOption(S_MAINHOST)) {
			MAINHOST = cl.getOptionValue(S_MAINHOST);
		}
		if(cl.hasOption(S_MAINPORT)) {
			MAINPORT = cl.getOptionValue(S_MAINPORT);
		}
		// buyer and seller agents
		if(cl.hasOption(S_BUYERS)) {
			for(String buyer : cl.getOptionValue(S_BUYERS).split(";")) {
				BUYERS.add(buyer);
			}
		}
		if(cl.hasOption(S_SELLERS)) {
			for(String seller : cl.getOptionValue(S_SELLERS).split(";")) {
				SELLERS.add(seller);
			}
		}
	}
	
	/**
	 * Start a new container with the given settings.
	 */
	public static ContainerController startContainer() {
		// get the JADE runtime singleton instance
		Runtime rt = Runtime.instance();
		
		// prepare the settings for the platform that we want to get onto
		Profile p = new ProfileImpl();
		p.setParameter(Profile.MAIN_HOST, MAINHOST);
		p.setParameter(Profile.MAIN_PORT, MAINPORT);
		
		// create and return a container
		return AGENTCONTAINER ? rt.createAgentContainer(p) : rt.createMainContainer(p);
	}

	public static void main(String[] args) {
		ContainerController container;
		List<ExternalAgent> agents;
		final Map<String, BookAgentInterpreter> interpreters;
		
		// work through the command-line arguments
		try {
			Options options = defineOptions();
			CommandLine cl = parseArguments(options, args);
			interpretArguments(cl);
		} catch(ParseException e) {
			System.err.println("Invalid arguments provided! Type -h for help.");
			return;
		}
		
		// create a new container
		container = startContainer();
		
		// create some new agents and interpreters
		agents = new ArrayList<ExternalAgent>(BUYERS.size() + SELLERS.size());
		interpreters = new HashMap<String, BookAgentInterpreter>(agents.size());
		for(String buyerName : BUYERS) {
			ExternalAgent buyer;
			// create a new external agent for each buyer
			buyer = new ExternalAgent(container, buyerName, ExternalAgent.BUYER_CLASS_NAME);
			// add them to the list of agents
			agents.add(buyer);
			// create and add an interpreter for them
			interpreters.put(buyerName.toLowerCase(), new BookBuyerInterpreter(buyer));
		}
		for(String sellerName : SELLERS) {
			ExternalAgent seller;
			// create a new external agent for each seller
			seller = new ExternalAgent(container, sellerName, ExternalAgent.SELLER_CLASS_NAME);
			// add them to the list of agents
			agents.add(seller);
			// create and add an interpreter for them
			interpreters.put(sellerName.toLowerCase(), new BookSellerInterpreter(seller));
		}
		
		// start the RMA
		new ExternalAgent(container, "rma" + (AGENTCONTAINER ? "-agent" : ""), "jade.tools.rma.rma");
		
		// start interpreting user input
		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean continuePolling = true;
				
				// get input from stdin
				Scanner s = new Scanner(System.in);
				
				// keep interpreting until there's no more input, or until
				// neither agents want to continue
				while(s.hasNextLine() && continuePolling) {
					try {
						// get the next line
						String line = s.nextLine();
						
						// if the line includes a colon (:)
						if(line.contains(":")) {
							// split it at the colon
							String[] tokens = line.split(":");
							
							// the first token should say which agent is meant by this message
							String agent = tokens[0].toLowerCase();
							
							// let the agent meant by it interpret it
							continuePolling = interpreters.get(agent).interpret(tokens[1]);
						// otherwise
						} else {
							// let all the agents interpret it
							for(BookAgentInterpreter interpreter : interpreters.values()) {
								if(!interpreter.interpret(line)) {
									continuePolling = false;
								}
							}
						}
					} catch(Exception e) {
						// do nothing
					}
				}
				
				// close the scanner
				s.close();
			}
			
		}).start();
	}
}
