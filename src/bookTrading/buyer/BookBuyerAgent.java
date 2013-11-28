package bookTrading.buyer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

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
		//TODO: gui = new BookBuyerGUI(this);
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
		addBehaviour(new PurchaseManager(this, bookTitle, maxPrice, deadline));
	}
	
	private class PurchaseManager extends TickerBehaviour {
		private static final long serialVersionUID = -8246238436156814059L;
		
		/** How often to tick and try to buy the book. */
		private static final long TICK_INTERVAL = 60000;
		/** What to tell the user if the book can't be purchased. */
		private static final String FAIL_MSG = "Cannot buy book %s";
		
		private String bookTitle;
		private int maxPrice;
		private long deadline, initTime, deltaT;
		
		public PurchaseManager(Agent agent, String bookTitle, int maxPrice, Date deadline) {
			super(agent, TICK_INTERVAL);
			
			// save the given arguments
			this.bookTitle = bookTitle;
			this.maxPrice = maxPrice;
			this.deadline = deadline.getTime();
			
			// come up with some times
			this.initTime = System.currentTimeMillis();
			this.deltaT = this.deadline - this.initTime;
		}

		@Override
		protected void onTick() {
			long currentTime = System.currentTimeMillis();
			
			// if the deadline expired
			if(currentTime > deadline) {
				// tell the user and stop trying
				gui.notifyUser(String.format(FAIL_MSG, bookTitle));
				stop();
			} else {
				// work out the acceptable price (max price * desperateness ratio)
				long elapsedTime = currentTime - initTime;
				int acceptablePrice = maxPrice * (int) (elapsedTime / deltaT);
				
				// start a new behaviour to get the book at this price
				myAgent.addBehaviour(new BookNegotiator(bookTitle, acceptablePrice, this));
			}
		}
		
	}
	
	/**
	 * So far, all this does is send a Call For Proposals (CFP) message to all registered
	 * seller agents.
	 * @author peter
	 *
	 */
	private class BookNegotiator extends OneShotBehaviour {
		
		private String bookTitle;
		private int price;
		private PurchaseManager pm;
		
		public BookNegotiator(String bookTitle, int acceptablePrice, PurchaseManager pm) {
			super();
			
			// save the given arguments
			this.bookTitle = bookTitle;
			this.price = acceptablePrice;
			this.pm = pm;
		}
		
		/** Send a CFP to all seller agents. */
		@Override
		public void action() {
			// create a CFP message
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			
			// add all the seller agents to the list of receivers
			for(AID seller : sellers) {
				cfp.addReceiver(seller);
			}
			
			// add the book we're looking for
			cfp.setContent(bookTitle);
			
			// finally, send the message
			myAgent.send(cfp);
		}
	}
}
