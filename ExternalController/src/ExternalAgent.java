import java.util.Date;

import bookTrading.common.BookInfo;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;

public class ExternalAgent {
	public static final String BUYER_CLASS_NAME = "bookTrading.buyer.BookBuyerAgent";
	public static final String SELLER_CLASS_NAME = "bookTrading.seller.BookSellerAgent";

	private AgentController agent;
	private ContainerController container;
	
	/**
	 * Start the agent on a given container.
	 */
	public ExternalAgent(ContainerController container, String agentName, String agentClass) {
		this.container = container;
		startAgent(agentName, agentClass);
	}
	/**
	 * Start the agent on a new container.
	 */
	public ExternalAgent(String host, String port, boolean main, String agentName, String agentClass) {
		this.container = startContainer(host, port, main);
		startAgent(agentName, agentClass);
	}
	
	/**
	 * Start a new container with the given settings.
	 * @param host The host at which the main container lives.
	 * @param port The port at which the main container lives.
	 * @param main Whether we should start a main or an agent container.
	 */
	public static ContainerController startContainer(String host, String port, boolean main) {
		// get the JADE runtime singleton instance
		Runtime rt = Runtime.instance();
		
		// prepare the settings for the platform that we want to get onto
		Profile p = new ProfileImpl();
		p.setParameter(Profile.MAIN_HOST, host);
		p.setParameter(Profile.MAIN_PORT, port);
		
		// create a container for the book buyer agent
		return main ? rt.createMainContainer(p) : rt.createAgentContainer(p);		 
	}
	
	/**
	 * Start a new AgentController for a given agent.
	 * @param agentName The desired name for the agent.
	 * @param agentClass The class of the agent to start.
	 */
	public void startAgent(String agentName, String agentClass) {
		// create and start the agent
		try {
			agent = container.createNewAgent(agentName, agentClass, null);
			agent.start();
		} catch(StaleProxyException e) {
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Only applicable for buyer agents. Send a BookInfo object to the agent.
	 */
	public void buyBook(String title, Date deadline, int maxPrice) {
		BookInfo info = new BookInfo(title, deadline, maxPrice);
		try {
			agent.putO2AObject(info, false);
		} catch (StaleProxyException e) {
			e.printStackTrace(System.err);
		}
	}
	/**
	 * Only applicable for seller agents. Send a BookInfo object to the agent.
	 */
	public void sellBook(String title, Date deadline, int initPrice, int minPrice) {
		BookInfo info = new BookInfo(title, deadline, initPrice, minPrice);
		try {
			agent.putO2AObject(info, false);
		} catch (StaleProxyException e) {
			e.printStackTrace(System.err);
		}
	}
}
