package bookTrading.buyer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bookTrading.IO.gui.BookAgentGUI;
import bookTrading.IO.gui.BookBuyerGUI;
import bookTrading.common.BookInfo;
import bookTrading.ontology.Book;
import bookTrading.ontology.BookTradingOntology;
import bookTrading.ontology.Costs;
import bookTrading.ontology.Sell;
import bookTrading.seller.BookSellerAgent;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BookBuyerAgent extends Agent implements BookBuyer{
	private static final long serialVersionUID = -2179361359046163266L;
	
	private static final long REFRESH_SELLERS_INTERVAL = 30 * 1000;

	// the agents that are selling books
	private List<AID> sellers;
	// the GUI that shows the state of this buyer
	private BookAgentGUI gui;
	
	// this agent's language/ontology
	private Codec codec;
	private Ontology ontology;
	
	@Override
	protected void setup() {
		// start the GUI
		gui = new BookBuyerGUI(this);
		gui.show();
		
		// register the acceptable content language and ontology
		codec = new SLCodec();
		ontology = BookTradingOntology.getInstance();
		
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		
		// start refreshing the list of sellers every now and then
		startRefreshingSellers();
		
		// start the external controller
		startExternalController();
	}
	@Override
	protected void takeDown() {
		// get rid of the GUI if needed
		if(gui != null) {
			gui.hide();
			gui = null;
		}
		
		// print a goodbye message
		System.out.println("BBA " + getAID().getName() + " terminated.");
	}
	
	private void startExternalController() {
		// allow receiving objects from outside
		setEnabledO2ACommunication(true, 0);
		// handle receiving them
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 6696662791288144719L;

			@Override
			public void action() {
				// try to get a new book to buy from the O2A mailbox
				BookInfo info = (BookInfo) myAgent.getO2AObject();
				// if we have one, process it; otherwise, block
				if(info != null) {
					buy(info.getBook(), info.getMaxPrice(), info.getDeadline());
				} else {
					block();
				}
			}
			
		});
	}
	
	/**
	 * Add a behaviour that continuously refreshes the list of sellers through
	 * the DF.
	 */
	private void startRefreshingSellers() {
		// initialize the sellers array
		sellers = new ArrayList<AID>();
		
		// consistently refresh it
		addBehaviour(new TickerBehaviour(this, REFRESH_SELLERS_INTERVAL) {
			private static final long serialVersionUID = 2727449035142212115L;

			@Override
			public void onStart() {
				super.onStart();
				
				// initially do a refresh
				onTick();
			}
			
			@Override
			protected void onTick() {
				DFAgentDescription template;
				ServiceDescription service;
				
				// initialize the template (and its service)
				template = new DFAgentDescription();
				service = new ServiceDescription();
				
				// we need a book selling service
				service.setType(BookSellerAgent.BS_SERVICE_TYPE);
				template.addServices(service);
				
				// look for it
				try {
					DFAgentDescription[] results = DFService.search(myAgent, template);
					// get rid of the old list of sellers
					sellers.clear();
					// add the ones from the search results
					for(DFAgentDescription result : results) {
						sellers.add(result.getName());
					}
				} catch(FIPAException fe) {
					fe.printStackTrace(System.err);
				}
			}
			
		});
	}
	
	private static final String CONFIRM_PURCHASE =
			"Buying %s for $%.2f or less before %s";
	/**
	 * Start looking for a new book to purchase.
	 */
	public void buy(Book book, double maxPrice, Date deadline) {
		addBehaviour(new PurchaseManager(this, book, maxPrice, deadline));
		// echo the new purchase task
		gui.notifyUser(String.format(CONFIRM_PURCHASE,
					book,
					maxPrice,
					deadline.toString()
				));
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
		private static final long TICK_INTERVAL = 5 * 1000;
		/** What to tell the user if the book can't be purchased. */
		private static final String FAIL_MSG = "Could not buy book %s before the deadline.";
		
		private Book book;
		private double maxPrice;
		private long deadline, initTime, deltaT;
		
		private boolean finished;
		
		public PurchaseManager(Agent agent, Book book, double maxPrice, Date deadline) {
			super(agent, TICK_INTERVAL);
			
			// save the given arguments
			this.book = book;
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
				gui.notifyUser(String.format(FAIL_MSG, book.toString()));
				stop();
			} else {
				// work out the acceptable price (max price * desperateness ratio)
				long elapsedTime = currentTime - initTime;
				double acceptablePrice = maxPrice * ((1.0 * elapsedTime) / deltaT);
				
				// make sure the new price is within bounds
				if(acceptablePrice > maxPrice) {
					acceptablePrice = maxPrice;
				}
				
				// start a new behaviour to get the book at this price
				myAgent.addBehaviour(new BookNegotiator(book, acceptablePrice, this));
			}
		}
		
		public void setFinished() {
			finished = true;
			// stop this behaviour
			stop();
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
	 * Handle negotiating on the given price for the given book with all the sellers.
	 * 
	 * @author peter
	 *
	 */
	private class BookNegotiator extends Behaviour {
		private static final long serialVersionUID = -7290372126695207141L;
		
		/** The conversation ID to use in all the conversations we start. */
		private static final String CONV_ID = "book-trade";
		/** The "reply with" parameter pattern for the CFP messages. */
		private static final String RW_CFP = "cfp%d";
		/** The "reply with" parameter pattern for the accept proposal messages. */
		private static final String RW_AP = "order%d";
		/** The usual message deadline which sellers have to reply by. */
		private static final long MSG_DEADLINE = 2 * 1000;
		/** What to tell the user if there was a successful purchase. */
		private static final String SUCCESS_MSG = "Book %s was bought for $%.2f.";
		
		// book stuff
		private Book book;
		private double price;
		private PurchaseManager pm;
		
		// structure & logic stuff
		private NegotiatorState state;
		private MessageTemplate template;
		private long deadline;
		
		// proposal stuff
		private AID bestBidder;
		private Costs bestProposal;
		
		public BookNegotiator(Book book, double acceptablePrice, PurchaseManager pm) {
			super();
			
			// save the given arguments
			this.book = book;
			this.price = acceptablePrice;
			this.pm = pm;
		}
		
		@Override
		public void onStart() {
			// inform the user of what we're doing
			gui.notifyUser(String.format("Trying to buy %s at $%.2f.", book.toString(), price));
			
			// initially, we're in the start state
			state = NegotiatorState.START;	
			
			// and we have no bids yet
			bestBidder = null;
			bestProposal = null;
			
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
				case FINISHED:
					// do nothing
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
			// what the sellers should reply with
			String replyWith = String.format(RW_CFP, System.currentTimeMillis());
			
			// make a custom message for each seller
			for(AID seller : sellers) {
				// create a CFP message
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			
				// add the seller
				cfp.addReceiver(seller);
			
				// interpretation stuff
				cfp.setOntology(BookTradingOntology.getInstance().getName());
				cfp.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			
				try {
					// we want a sell action for the book we're looking for				
					Sell sellAction = new Sell();
					sellAction.setItem(book);
	
					// which is part of an actual action
					Action act = new Action();
					act.setAction(sellAction);
					act.setActor(seller);
					
					// put that into the message
					getContentManager().fillContent(cfp, act);
				} catch (Exception e) {
					// print the stack trace
					e.printStackTrace(System.err);
					// stop trying to buy this book
					state = NegotiatorState.FINISHED;
					return;
				}
				
				// add some identifiers to the message
				cfp.setConversationId(CONV_ID);
				cfp.setReplyWith(replyWith);
				
				// and a deadline
				deadline = System.currentTimeMillis() + MSG_DEADLINE;
				cfp.setReplyByDate(new Date(deadline));
				
				// send the message
				myAgent.send(cfp);
			}
			
			// update the template we're expecting
			template = MessageTemplate.and(
						MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
						MessageTemplate.MatchInReplyTo(replyWith)
					);
			
			// change to the next state
			state = NegotiatorState.WAITING_FOR_PROPOSALS;
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
					// get the actual proposal
					Costs proposal = (Costs) getContentManager().extractContent(proposalMsg);
					
					// if its the first proposal, or it beats the best proposal
					if(bestBidder == null || bestProposal == null || proposal.getPrice() <= bestProposal.getPrice()) {
						// now it's the best proposal
						bestBidder = proposalMsg.getSender();
						bestProposal = proposal;
					}
					
				} catch(Exception e) {
					// if we get an invalid proposal, ignore it
					return;
				}
			// once the deadline has expired
			} else {				
				// if we don't have a valid best proposal
				if(bestBidder == null || bestProposal == null || bestProposal.getPrice() > price) {					
					// then we're done
					state = NegotiatorState.FINISHED;
					return;
				// otherwise (if we do have a good proposal)
				} else {
					// send an ACCEPT PROPOSAL message
					ACLMessage ap = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					
					// to the best bidder
					ap.addReceiver(bestBidder);
					
					// using the known technologies
					ap.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
					ap.setOntology(BookTradingOntology.getInstance().getName());
					
					// with the proposal they made
					try {
						getContentManager().fillContent(ap, bestProposal);
					} catch (Exception e) {
						// print the stack trace
						e.printStackTrace(System.err);
						
						// if we can't send them an AP, then we might as well stop
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
								MessageTemplate.MatchInReplyTo(ap.getReplyWith())
							);
					
					// move to the next state
					state = NegotiatorState.WAITING_FOR_CONFIRMATION;
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
					gui.notifyUser(String.format(SUCCESS_MSG, book.toString(), bestProposal.getPrice()));
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
