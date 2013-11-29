package bookTrading.IO.gui;

import jade.core.Agent;

/**
 * The GUI that will be used to show the status of a book-buyer/book-seller agent.
 * @author peter
 *
 */
public abstract class BookAgentGUI {

	protected Agent agent;
	protected String name;
	
	/**
	 * Create a new GUI for the given agent.
	 */
	public BookAgentGUI(Agent agent) {
		this.agent = agent;
		this.name = agent.getAID().getLocalName();
	}
	
	public void show() {
		// print the welcome and usage messages
		printWelcomeMessage();
		printUsageMessage();
	}
	
	public void hide() {
		// don't need to do anything
	}
		
	public void notifyUser(String message) {
		// just print to System.out
		System.out.println(name + ": " + message);		
	}


	protected abstract void printWelcomeMessage();
	protected abstract void printUsageMessage();
	
}