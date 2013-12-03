import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Arrays;
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
	public static final boolean AGENTCONTAINER = false;
	public static final String MAINHOST = "localhost";
	public static final String MAINPORT = "1099";
	public static final List<String> BUYERS = new ArrayList<String>();
	public static final List<String> SELLERS = new ArrayList<String>();
	
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
	public static Settings interpretArguments(CommandLine cl) {
		Settings settings = new Settings();
		
		// if the agent container argument is there then run that way
		settings.setAgentContainer(cl.hasOption(S_AGENTCONTAINER));
		// host and port stuff
		if(cl.hasOption(S_MAINHOST)) {
			settings.setMainHost(cl.getOptionValue(S_MAINHOST));
		}
		if(cl.hasOption(S_MAINPORT)) {
			settings.setMainPort(cl.getOptionValue(S_MAINPORT));
		}
		// buyer and seller agents
		if(cl.hasOption(S_BUYERS)) {
			settings.setBuyers(Arrays.asList(cl.getOptionValue(S_BUYERS).split(";")));
		}
		if(cl.hasOption(S_SELLERS)) {
			settings.setSellers(Arrays.asList(cl.getOptionValue(S_SELLERS).split(";")));
		}
		
		// return the overriden settings
		return settings;
	}
	
	private ContainerController container;
	private List<ExternalAgent> agents;
	private Map<String, BookAgentInterpreter> interpreters;
	private Thread interpreterThread;
	
	/**
	 * Start a new container with the given settings.
	 */
	private void startContainer(Settings settings) {
		// get the JADE runtime singleton instance
		Runtime rt = Runtime.instance();
		
		// prepare the settings for the platform that we want to get onto
		Profile p = new ProfileImpl();
		p.setParameter(Profile.MAIN_HOST, settings.getMainHost());
		p.setParameter(Profile.MAIN_PORT, settings.getMainPort());
		
		// create a container
		container = settings.isAgentContainer() ? rt.createAgentContainer(p) : rt.createMainContainer(p);
	}
	
	public AgentEnvironment(Settings settings) {
		// create a new container
		startContainer(settings);

		// create some new agents and interpreters
		agents = new ArrayList<ExternalAgent>(settings.getBuyers().size() + settings.getSellers().size());
		interpreters = new HashMap<String, BookAgentInterpreter>(agents.size());
		for(String buyerName : settings.getBuyers()) {
			ExternalAgent buyer;
			// create a new external agent for each buyer
			buyer = new ExternalAgent(container, buyerName, ExternalAgent.BUYER_CLASS_NAME);
			// add them to the list of agents
			agents.add(buyer);
			// create and add an interpreter for them
			interpreters.put(buyerName.toLowerCase(), new BookBuyerInterpreter(buyer));
		}
		for(String sellerName : settings.getSellers()) {
			ExternalAgent seller;
			// create a new external agent for each seller
			seller = new ExternalAgent(container, sellerName, ExternalAgent.SELLER_CLASS_NAME);
			// add them to the list of agents
			agents.add(seller);
			// create and add an interpreter for them
			interpreters.put(sellerName.toLowerCase(), new BookSellerInterpreter(seller));
		}

		// start the RMA
		agents.add(new ExternalAgent(container, "rma" + (settings.isAgentContainer() ? "-agent" : ""), "jade.tools.rma.rma"));

		// start interpreting user input
		interpreterThread = new Thread(new Runnable() {

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

		});
		interpreterThread.start();
	}
	
	public void kill() {
		// stop the interpreter thread
		interpreterThread.interrupt();
		
		// kill the agents
		for(ExternalAgent agent : agents) {
			agent.kill();
		}
		
		// kill the container
		try {
			container.kill();
		} catch (StaleProxyException e) {
			// do nothing
		}
		
		// free all the memory
		agents = null;
		container = null;
		interpreters = null;
		interpreterThread = null;
	}

	public static void main(String[] args) {		
		// work through the command-line arguments
		try {
			// read and parse them
			Options options = defineOptions();
			CommandLine cl = parseArguments(options, args);
			// start a new environment based on them
			new AgentEnvironment(interpretArguments(cl));
		} catch(ParseException e) {
			System.err.println("Invalid arguments provided! Type -h for help.");
			return;
		}
	}
	
	// a bean for the settings of the environment
	static class Settings {
		// default settings
		private boolean agentContainer = AGENTCONTAINER;
		private String mainHost = MAINHOST;
		private String mainPort = MAINPORT;
		private List<String> buyers = BUYERS;
		private List<String> sellers = SELLERS;
		
		
		public boolean isAgentContainer() {
			return agentContainer;
		}
		public void setAgentContainer(boolean agentContainer) {
			this.agentContainer = agentContainer;
		}
		public String getMainHost() {
			return mainHost;
		}
		public void setMainHost(String mainHost) {
			this.mainHost = mainHost;
		}
		public String getMainPort() {
			return mainPort;
		}
		public void setMainPort(String mainPort) {
			this.mainPort = mainPort;
		}
		public List<String> getBuyers() {
			return buyers;
		}
		public void setBuyers(List<String> buyers) {
			this.buyers = buyers;
		}
		public List<String> getSellers() {
			return sellers;
		}
		public void setSellers(List<String> sellers) {
			this.sellers = sellers;
		}		
	}
}
