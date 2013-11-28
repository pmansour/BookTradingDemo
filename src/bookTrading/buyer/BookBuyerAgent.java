package bookTrading.buyer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bookTrading.common.Proposal;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
		gui = new BookBuyerGUIText(this);
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
	
	/**
	 * Start looking for a new book to purchase.
	 */
	public void purchase(String bookTitle, int maxPrice, Date deadline) {
		addBehaviour(new PurchaseManager(this, bookTitle, maxPrice, deadline));
	}
	
	/**
	 * Incrementally (and linearly) accept a higher price (from 0 up to the
	 * max price) for a book until either the deadline expires or we purchase
	 * it.
	 * 
	 * @author peter
	 * 
	 */
	private class PurchaseManager extends TickerBehaviour {
		private static final long serialVersionUID = -8246238436156814059L;
		
		/** How often to wake up and increase the price. */
		private static final long TICK_INTERVAL = 60000;
		/** What to tell the user if the book can't be purchased. */
		private static final String FAIL_MSG = "Cannot buy book %s";
		
		private String bookTitle;
		private int maxPrice;
		private long deadline, initTime, deltaT;
		
		private boolean finished;
		
		public PurchaseManager(Agent agent, String bookTitle, int maxPrice, Date deadline) {
			super(agent, TICK_INTERVAL);
			
			// save the given arguments
			this.bookTitle = bookTitle;
			this.maxPrice = maxPrice;
			this.deadline = deadline.getTime();
			
			// come up with some times
			this.initTime = System.currentTimeMillis();
			this.deltaT = this.deadline - this.initTime;
			
			// initially, this isn't finished
			this.finished = false;
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
		
		public void setFinished() {
			finished = true;
		}
		public boolean isFinished() {
			return finished;
		}
		
	}
	
	private static enum NegotiatorState {
		START,						// Before #1 - Still need to send CFP's
		WAITING_FOR_PROPOSALS,		// Before #3 - Waiting for proposals
		WAITING_FOR_CONFIRMATION,	// Before #5 - Waiting for confirmation
		FINISHED					// Finished either in a success or failure.
	};
	/**
	 * At any time, if we're finished, then stop.
	 * 
	 * @author peter
	 *
	 */
	private class BookNegotiator extends Behaviour {
		private static final long serialVersionUID = -7290372126695207141L;
		
		/** The conversation ID to use in all the conversations we start. */
		private static final String CONV_ID = "book-trade";
		/** The "reply with" parameter pattern for the CFP messages. */
		private static final String RW_CFP = "cfp%ld";
		/** The "reply with" parameter pattern for the accept proposal messages. */
		private static final String RW_AP = "order%ld";
		/** The usual message deadline which sellers have to reply by. */
		private static final long MSG_DEADLINE = 30 * 1000;
		/** What to tell the user if there was a successful purchase. */
		private static final String SUCCESS_MSG = "Book %s was bought for %d.";
		
		// book stuff
		private String bookTitle;
		private int price;
		private PurchaseManager pm;
		
		// structure & logic stuff
		private NegotiatorState state;
		private MessageTemplate template;
		private long deadline;
		
		// proposal stuff
		private AID bestBidder;
		private int bestPrice;
		
		public BookNegotiator(String bookTitle, int acceptablePrice, PurchaseManager pm) {
			super();
			
			// save the given arguments
			this.bookTitle = bookTitle;
			this.price = acceptablePrice;
			this.pm = pm;
		}
		
		@Override
		public void onStart() {
			// initially, we're in the start state
			state = NegotiatorState.START;	
			
			// and we have no bids yet
			bestBidder = null;
			bestPrice = -1;
			
			// and no deadline yet either
			deadline = 0;
		}
		
		@Override
		public void action() {
			switch(state) {
				case START:
					// send the CFP messages to all the sellers.
					START();
					break;
				case WAITING_FOR_PROPOSALS:
					// get all the proposals, and send an AP to the best one
					// (if applicable).
					WFP();					
					break;
				case WAITING_FOR_CONFIRMATION:
					// wait for a confirmation of the AP we sent, and complete
					// the purchase if it gets confirmed.
					WFC();
					break;
			}
		}
		@Override
		public boolean done() {
			// we're done if the book has been purchased or if this negotiator
			// gave up.
			return pm.isFinished() || state == NegotiatorState.FINISHED;
		}
		
		/**
		 * -> Send out a CFP to each known seller.
		 * -> Record the time it was sent and effectively "count down".
		 * -> Update the template to receive, so we only care about PROPOSE.
		 */
		private void START() {
			// create a CFP message
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			
			// add all the seller agents to the list of receivers
			for(AID seller : sellers) {
				cfp.addReceiver(seller);
			}
			
			// add the book we're looking for
			cfp.setContent(bookTitle);
			
			// add some identifiers to the message
			cfp.setConversationId(CONV_ID);
			cfp.setReplyWith(String.format(RW_CFP, System.currentTimeMillis()));
			
			// and a deadline
			deadline = System.currentTimeMillis() + MSG_DEADLINE;
			cfp.setReplyByDate(new Date(deadline));
			
			// send the message
			myAgent.send(cfp);
			
			// update the template we're expecting
			template = MessageTemplate.and(
						MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
						MessageTemplate.and(
							MessageTemplate.MatchConversationId(CONV_ID),
							MessageTemplate.MatchReplyWith(cfp.getReplyWith())
						)								
					);
		}
		
		/**
		 * 	If the deadline hasn't expired yet:
		 * 		-> Receive Proposals from sellers.
		 * 		-> Keep track of the best proposal and its respective bidder.
		 * 	Once it expires:
		 * 		-> If we have a best proposal:
		 * 			-> Send out an AP with that proposal to the bidder.
		 * 			-> Record the time it was sent and "count down".
		 * 			-> Update the template to receive messages from that bidder.
		 * 		-> If we don't:
		 * 			-> Go to the FINISHED state.
		 */
		private void WFP() {
			// while the deadline hasn't expired yet
			if(System.currentTimeMillis() <= deadline) {
				// receive a proposal
				ACLMessage proposalMsg = myAgent.receive(template);
				
				// if there isn't one, block until we get one
				if(proposalMsg == null) {
					block();
					return;
				}
				
				try {
					// get the actual proposal price
					int proposalPrice = Integer.parseInt(proposalMsg.getContent());
					
					// if its the first proposal, or it beats the best proposal
					if(bestBidder == null || proposalPrice <= bestPrice) {
						// now it's the best proposal
						bestBidder = proposalMsg.getSender();
						bestPrice = proposalPrice;
					}
					
				} catch(NumberFormatException e) {
					// if we get an invalid price, ignore that proposal
					return;
				}
			// once the deadline has expired
			} else {
				// if we don't have a valid best proposal
				if(bestBidder == null || bestPrice > price) {
					// then we're done
					state = NegotiatorState.FINISHED;
					return;
				// otherwise (if we do have a good proposal)
				} else {
					// send an ACCEPT PROPOSAL message
					ACLMessage ap = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					
					// to the best bidder
					ap.addReceiver(bestBidder);
					
					// with the proposal they made
					try {
						ap.setContentObject(new Proposal(bookTitle, bestPrice));
					} catch (IOException e) {
						// if we can't send them an AP, then we might as well give up
						state = NegotiatorState.FINISHED;
						return;
					}
					
					// and some identifiers for the message
					ap.setConversationId(CONV_ID);
					ap.setReplyWith(String.format(RW_AP, System.currentTimeMillis()));
					
					// and a deadline
					deadline = System.currentTimeMillis() + MSG_DEADLINE;
					ap.setReplyByDate(new Date(deadline));
					
					// send the message
					myAgent.send(ap);
					
					// update the template we're expecting
					template = MessageTemplate.and(
								MessageTemplate.MatchSender(bestBidder),
								MessageTemplate.and(
									MessageTemplate.MatchConversationId(CONV_ID),
									MessageTemplate.MatchReplyWith(ap.getReplyWith())
								)								
							);
					
				}
			}
		}
		
		/**
		 * 	If the deadline hasn't expired yet:
		 * 		-> Receive a message from the bidder.
		 *		-> If it's a CONFIRM:
		 * 			-> Tell the price manager that it's finished.
		 * 			-> Notify the user.
		 * 			-> Go to the FINISHED state.
		 * 		-> Otherwise:
		 * 			-> Go to the START state.
		 * 	Once it expires,
		 * 		-> Go to the START state.
		 */
		private void WFC() {
			// while the deadline hasn't expired yet
			if(System.currentTimeMillis() <= deadline) {
				// receive a message from the bidder
				ACLMessage msg = myAgent.receive(template);
				
				// if there isn't one, block until there is
				if(msg == null) {
					block();
					return;
				}
				
				// if the the seller is confirming the transaction
				if(msg.getPerformative() == ACLMessage.CONFIRM) {
					// mark this book as finished in the price manager
					pm.setFinished();
					// notify the user of this
					gui.notifyUser(String.format(SUCCESS_MSG, bookTitle, bestPrice));
					// our work here is done
					state = NegotiatorState.FINISHED;
				// otherwise (if he's saying anything else)
				} else {
					// assume he's cancelling - so start again with this price!
					state = NegotiatorState.START;
				}
			// if the deadline expired,
			} else {
				// start over again!
				state = NegotiatorState.START;
			}
		}
	}
	
}
