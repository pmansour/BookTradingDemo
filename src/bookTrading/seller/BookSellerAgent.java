package bookTrading.seller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

public class BookSellerAgent extends Agent {
	private static final long serialVersionUID = 408650196499616944L;

	private Map<String, PriceManager> catalogue;
	
	private BookSellerGUI gui;
	
	@Override
	protected void setup() {
		// initialize the catalogue
		catalogue = new HashMap<String, PriceManager>();
		
		// start and show a new GUI
		//TODO: gui = new BookSellerGUI(this);
		gui.show();
		
		// start the two behaviour servers:
		// serve calls for price from buyer agents
		addBehaviour(new CallForOfferServer());
		// serve purchase requests from buyer agents
		addBehaviour(new PurchaseOrderServer());
	}
	@Override
	protected void takeDown() {
		// get rid of the GUI if it's there
		if(gui != null) {
			gui = null;
		}
		
		// print a goodbye message
		System.out.println("BSA " + getAID().getName() + " terminating.");
	}
	
	/**
	 * Put a new book for sale.
	 */
	public void putForSale(String bookTitle, int initPrice, int minPrice, Date deadline) {
		addBehaviour(new PriceManager(this, bookTitle, initPrice, minPrice, deadline));
	}
	
	private class PriceManager extends TickerBehaviour {
		private static final long serialVersionUID = -5667551287935590044L;
		
		/** How often to wake up and look for prices for a book. */
		private static final long TICKER_INTERVAL = 60000;
		/** What to tell the user when we can't sell the book by the given deadline. */
		private static final String EXPR_MSG = "Cannot sell the book %s.";

		private String bookTitle;
		private int minPrice, initPrice, currentPrice, deltaP;
		private long initTime, deadline, deltaT;
		
		public PriceManager(Agent agent, String bookTitle, int initPrice, int minPrice, Date deadline) {
			super(agent, TICKER_INTERVAL);
			
			// save the given arguments
			this.bookTitle = bookTitle;
			this.minPrice = minPrice;
			this.initPrice = initPrice;
			this.deadline = deadline.getTime();
			
			// work out some stuff
			this.deltaP = initPrice - minPrice;
			this.initTime = System.currentTimeMillis();
			this.deltaT = this.deadline - this.initTime;
		}
		
		@Override
		public void onStart() {
			// add the book to the seller agent's catalogue
			catalogue.put(bookTitle, this);
			super.onStart();
		}

		@Override
		protected void onTick() {
			long currentTime = System.currentTimeMillis();
			// if the deadline expired
			if(currentTime > deadline) {
				// notify the user and stop looking
				gui.notifyUser(String.format(EXPR_MSG, bookTitle));
				catalogue.remove(bookTitle);
				stop();
			} else {
				// work out the current price
				long elapsedTime = currentTime - initTime;
				currentPrice = initPrice - deltaP * (int) (elapsedTime / deltaT);
			}
		}
		
		public int getCurrentPrice() {
			return currentPrice;
		}
		
	}
}
