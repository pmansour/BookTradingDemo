package bookTrading.buyer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;

public class BookBuyerAgent extends Agent {
	private static final long serialVersionUID = -2179361359046163266L;

	// the agents that are selling books
	private List<AID> sellers;
	// the GUI that shows the state of this buyer
	private BookBuyerGUI gui;
	
	@Override
	protected void setup() {
		// print a welcome message
		System.out.println("BBA " + getAID().getName() + " ready.");
		
		// load the sellers from the command-line arguments
		Object[] arguments = getArguments();
		if(arguments != null) {
			sellers = new ArrayList<AID>(arguments.length);
			for(Object seller : arguments) {
				// create the sellers' AIDs from their local names
				sellers.add(new AID((String) seller, AID.ISLOCALNAME));
			}
		}
		
		// show the GUI
		//gui = new BookBuyerGUI(this);
		gui.show();
	}
	
	@Override
	protected void takeDown() {
		// get rid of the GUI if needed
		if(gui != null) {
			gui = null;
		}
		
		// print a goodbye message
		System.out.println("BBA " + getAID().getName() + " terminated.");
	}
	
	public void purchase(String bookTitle, int maxPrice, Date deadline) {
		//TODO
	}
}
