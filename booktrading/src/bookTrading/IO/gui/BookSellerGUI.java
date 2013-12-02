package bookTrading.IO.gui;

import bookTrading.IO.interpreter.BookSellerInterpreter;
import jade.core.Agent;

public class BookSellerGUI extends BookAgentGUI {
	
	public BookSellerGUI(Agent agent) {
		super(agent);
	}

	@Override
	protected void printWelcomeMessage() {
		notifyUser("Hi! My name is " + name + ", and I can sell books!");
	}

	@Override
	protected void printUsageMessage() {
		notifyUser("To sell a book, please enter a line in the following form:");
		notifyUser(BookSellerInterpreter.getUsageMessage());
	}

}