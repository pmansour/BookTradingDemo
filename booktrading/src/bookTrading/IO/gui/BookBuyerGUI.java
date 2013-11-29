package bookTrading.IO.gui;

import jade.core.Agent;

public class BookBuyerGUI extends BookAgentGUI {
	
	public BookBuyerGUI(Agent agent) {
		super(agent);
	}

	@Override
	protected void printWelcomeMessage() {
		notifyUser("Hi! My name is " + name + ", and I can buy books!");
	}

	@Override
	protected void printUsageMessage() {
		notifyUser("To purchase a book, please enter a line in the following form:");
		notifyUser("buy,[book title],[maximum price],[deadline (in seconds)]");
	}

}