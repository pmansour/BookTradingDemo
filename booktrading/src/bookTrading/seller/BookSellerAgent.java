package bookTrading.seller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import bookTrading.IO.gui.BookAgentGUI;
import bookTrading.IO.gui.BookSellerGUI;
import bookTrading.common.BookInfo;
import bookTrading.ontology.Book;
import bookTrading.ontology.BookTradingOntology;
import bookTrading.ontology.Costs;
import bookTrading.ontology.Sell;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BookSellerAgent extends Agent implements BookSeller {
	private static final long serialVersionUID = 408650196499616944L;
	
	public static final String BS_SERVICE_TYPE = "Book-Selling";

	// The catalogue of books currently on sale.
	private Map<String, PriceManager> catalogue;
	// The GUI controlling user interaction.
	private BookAgentGUI gui;
	
	// this agent's language/ontology
	private Codec codec;
	private Ontology ontology;
	
	@Override
	protected void setup() {
		// start and show a new GUI
		gui = new BookSellerGUI(this);
		gui.show();
		
		// register the acceptable content language and ontology
		codec = new SLCodec();
		ontology = BookTradingOntology.getInstance();
		
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		// initialize the catalogue
		catalogue = new HashMap<String, PriceManager>();

		// register this agent's service(s) with the DF
		registerServices();

		// start the external controller
		startExternalController();
		
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
			gui.hide();
			gui = null;
		}
		
		// this agent no longer provides any services
		deregisterServices();
		
		// print a goodbye message
		System.out.println("BSA " + getAID().getName() + " terminating.");
	}
	
	private void startExternalController() {
		// allow receiving objects from outside
		setEnabledO2ACommunication(true, 0);
		// handle receiving them
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 6696662791288144719L;

			@Override
			public void action() {
				// try to get a new book to sell from the O2A mailbox
				BookInfo info = (BookInfo) myAgent.getO2AObject();
				// if we have one, process it; otherwise, block
				if(info != null) {
					sell(info.getBook(), info.getInitPrice(), info.getMinPrice(), info.getDeadline());
				} else {
					block();
				}
			}
			
		});
	}
	
	/**
	 * Register this agent's service(s) in the DF.
	 */
	private void registerServices() {
		DFAgentDescription dfd;
		ServiceDescription service;
		
		// create the agent description for this agent
		dfd = new DFAgentDescription();
		dfd.setName(getAID());
		
		// create the actual service description
		service = new ServiceDescription();
		service.setType(BS_SERVICE_TYPE);
		service.setName(getLocalName() + '-' + BS_SERVICE_TYPE);
		
		// add the service to this agent
		dfd.addServices(service);
		
		// finally, register the service with the DF
		try {
			DFService.register(this, dfd);
			// inform the user
			gui.notifyUser("Registered in the DF for " + service.getName());
		} catch(FIPAException fe) {
			fe.printStackTrace(System.err);
			// inform the user
			gui.notifyUser("Could not register in the DF!");
		}
	}
	
	/**
	 * Deregister this agent's service(s) in the DF.
	 */
	private void deregisterServices() {
		try {
			DFService.deregister(this);
			// inform the user
			gui.notifyUser("Registered from the DF.");
		} catch(FIPAException fe) {
			fe.printStackTrace(System.err);
			// inform the user
			gui.notifyUser("Could not deregister from the DF.");
		}
	}
	
	private static final String CONFIRM_SALE =
			"Selling %s for initially $%.2f but minimum $%.2f before %s";
	/**
	 * Put a new book up for sale.
	 */
	public void sell(Book book, double initPrice, double minPrice, Date deadline) {
		addBehaviour(new PriceManager(this, book, initPrice, minPrice, deadline));
		// echo the new sale task
		gui.notifyUser(String.format(CONFIRM_SALE,
					book.toString(),
					initPrice,
					minPrice,
					deadline.toString()
				));
	}
	
	/**
	 * Incrementally (and linearly) accept a lower price as long as the book
	 * hasn't been sold, all the way from the initial price and up to the
	 * minimum price.
	 * 
	 * @author peter
	 *
	 */
	private class PriceManager extends TickerBehaviour {
		private static final long serialVersionUID = -5667551287935590044L;
		
		/** How often to wake up and decrease the price. */
		private static final long TICKER_INTERVAL = 3 * 1000;
		/** What to tell the user when we can't sell the book by the given deadline. */
		private static final String EXPR_MSG = "Could not sell the book %s before the deadline.";

		private Book book;
		private double initPrice, minPrice, currentPrice, deltaP;
		private long initTime, deadline, deltaT;
		
		public PriceManager(Agent agent, Book book, double initPrice, double minPrice, Date deadline) {
			super(agent, TICKER_INTERVAL);
			
			// save the given arguments
			this.book= book;
			this.initPrice = initPrice;
			this.minPrice = minPrice;
			this.deadline = deadline.getTime();
			
			// work out some stuff
			this.deltaP = initPrice - minPrice;
			this.initTime = System.currentTimeMillis();
			this.deltaT = this.deadline - this.initTime;
			
			// at first the current price is the initial price
			this.currentPrice = initPrice;
		}
		
		@Override
		public void onStart() {
			// add the book to the seller agent's catalogue
			catalogue.put(book.toString(), this);
			super.onStart();
		}

		@Override
		protected void onTick() {
			long currentTime = System.currentTimeMillis();
			// if the deadline expired
			if(currentTime > deadline) {
				// the book is no longer on sale
				catalogue.remove(book.toString());
				// notify the user
				gui.notifyUser(String.format(EXPR_MSG, book.toString()));
				// this behaviour is now useless
				stop();
			} else {
				// work out the current price
				long elapsedTime = currentTime - initTime;
				currentPrice = initPrice - deltaP * ((1.0 * elapsedTime) / deltaT);
				// make sure its within bounds
				if(currentPrice < minPrice) {
					currentPrice = minPrice;
				}
				// inform the user of the new price
				gui.notifyUser(String.format("Now accepting $%.2f for %s.", currentPrice, book.toString()));
			}
		}
		
		public double getCurrentPrice() {
			return currentPrice;
		}
		
	}
	
	/**
	 * Serve incoming Call For Proposal CA's from buyer agents.
	 * 
	 * Receives an incoming CFP.
	 * If the book is in the catalogue:
	 * 	-> Replies with a PROPOSE message containing the proposal price.
	 * Otherwise,
	 * 	-> Replies with a REFUSE message.
	 * 
	 * @author peter
	 *
	 */
	private class CallForOfferServer extends CyclicBehaviour {
		private static final long serialVersionUID = 5093118259000491987L;
		
		// we only care about CFP messages
		private MessageTemplate template =
				MessageTemplate.MatchPerformative(ACLMessage.CFP);

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(template);
			
			// block until we have a [CFP] message
			if(msg == null) {
				block();
				return;
			}
			
			// we're gonna reply no matter what
			ACLMessage reply = msg.createReply();
			
			// process the message
			try {
				// process the book using the BookTradingOntology
				ContentManager cm = myAgent.getContentManager();
				Action act = (Action) cm.extractContent(msg);
				Sell sellAction = (Sell) act.getAction();
				Book book = sellAction.getItem();
				
				// look for it it in our catalogue
				PriceManager pm = catalogue.get(book.toString());
				
				// if we have the book
				if(pm != null) {
					// create a new predicate with the book's cost
					Costs costs = new Costs();
					costs.setItem(book);
					costs.setPrice(pm.getCurrentPrice());
										
					// reply with the proposal
					reply.setPerformative(ACLMessage.PROPOSE);
					cm.fillContent(reply, costs);
				// if we don't
				} else {
					// reply with a REFUSE
					reply.setPerformative(ACLMessage.REFUSE);				
				}
			} catch(Exception e) {
				e.printStackTrace(System.err);
				reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
			}
			
			// send the reply in any case
			myAgent.send(reply);
		}
		
	}
	
	/**
	 * Serve incoming Accept Proposal CA's from buyer agents.
	 * 
	 * Receives an incoming ACCEPT PROPOSAL.
	 * If the proposal is corrupt,
	 * 	-> Reply with a NOT UNDERSTOOD.
	 * If the book is in the catalogue and the price >= what we're currently asking for:
	 * 	-> Reply with a CONFIRM.
	 * 	-> Stop the price manager.
	 * 	-> Take the book out of the catalogue.
	 * 	-> Notify the user that the book was sold for the proposed price.
	 * Otherwise,
	 * 	-> Reply with a DISCONFIRM.
	 * 
	 * @author peter
	 *
	 */
	private class PurchaseOrderServer extends CyclicBehaviour {
		private static final long serialVersionUID = 5093118259000491987L;

		/** What to tell the user when an item has been sold successfully. */
		private static final String SUCCESS_MSG = "Book %s has been sold for $%.2f.";
		
		// we only care about Accept Proposal messages
		private MessageTemplate template =
				MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
		
		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(template);
			
			// block until we have a [Accept Proposal] message
			if(msg == null) {
				block();
				return;
			}
			
			// in any case, we're going to reply to any accepted proposal
			ACLMessage reply = msg.createReply();
			
			try {
				// get the proposal which the buyer is accepting
				Costs proposal = (Costs) getContentManager().extractContent(msg);
				
				// get the price manager for the concerned book
				PriceManager pm = (PriceManager) catalogue.get(proposal.getItem().toString());

				// if we still have the book and the proposal price is valid
				if(pm != null && pm.getCurrentPrice() <= proposal.getPrice()) {
					// reply with a confirmation
					reply.setPerformative(ACLMessage.CONFIRM);
					// stop the price manager for this book
					pm.stop();
					// remove the book from the catalogue
					catalogue.remove(proposal.getItem().toString());
					// notify the user of success
					gui.notifyUser(String.format(SUCCESS_MSG, proposal.getItem().toString(), proposal.getPrice()));
				// otherwise
				} else {
					// reply with a disconfirmation
					reply.setPerformative(ACLMessage.DISCONFIRM);				
				}
			// if we weren't sent back a proper proposal
			} catch (Exception e) {
				// send a not understood reply we didn't get back a Proposal content
				reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
			}
			
			// finally, send the reply
			myAgent.send(reply);
		}
		
	}
}
