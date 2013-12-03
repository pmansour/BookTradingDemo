import java.util.Date;

import bookTrading.buyer.BookBuyer;
import bookTrading.common.BookInfo;
import bookTrading.ontology.Book;
import bookTrading.seller.BookSeller;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class ExternalAgent implements BookBuyer, BookSeller {
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
	 * Start a new AgentController for a given agent.
	 * @param agentName The desired name for the agent.
	 * @param agentClass The class of the agent to start.
	 */
	private void startAgent(String agentName, String agentClass) {
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
	public void buy(Book book, double maxPrice, Date deadline) {
		BookInfo info = new BookInfo(book, maxPrice, deadline);
		try {
			agent.putO2AObject(info, false);
		} catch (StaleProxyException e) {
			e.printStackTrace(System.err);
		}
	}
	/**
	 * Only applicable for seller agents. Send a BookInfo object to the agent.
	 */
	public void sell(Book book, double initPrice, double minPrice, Date deadline) {
		BookInfo info = new BookInfo(book, initPrice, minPrice, deadline);
		try {
			agent.putO2AObject(info, false);
		} catch (StaleProxyException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public void kill() {
		try {
			agent.kill();
		} catch (StaleProxyException e) {
			// do nothing
		}
	}
}
