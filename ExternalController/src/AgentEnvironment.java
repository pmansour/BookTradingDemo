import jade.wrapper.ContainerController;

import java.util.Scanner;

import bookTrading.IO.interpreter.BookAgentInterpreter;
import bookTrading.IO.interpreter.BookBuyerInterpreter;
import bookTrading.IO.interpreter.BookSellerInterpreter;


public class AgentEnvironment {

	public static void main(String[] args) {
		ContainerController main;
		ExternalAgent buyer, seller;
		final BookAgentInterpreter buyerInterpreter, sellerInterpreter;
		
		// create a new main container
		main = ExternalAgent.startContainer("localhost", "1099", true);
		
		// create some new external agents
		buyer = new ExternalAgent(main, "Peter", ExternalAgent.BUYER_CLASS_NAME);
		seller = new ExternalAgent(main, "Amazon", ExternalAgent.SELLER_CLASS_NAME);
		
		// create some interpreters
		buyerInterpreter = new BookBuyerInterpreter(buyer);
		sellerInterpreter = new BookSellerInterpreter(seller);
		
		// start interpreting user input
		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean buyerContinue = true, sellerContinue = true;
				
				// get input from stdin
				Scanner s = new Scanner(System.in);
				
				// keep interpreting until there's no more input, or until
				// neither agents want to continue
				while(s.hasNextLine() && (buyerContinue || sellerContinue)) {
					// get the next line
					String line = s.nextLine();
					// get both agents to interpret it
					buyerContinue = buyerInterpreter.interpret(line);
					sellerContinue = sellerInterpreter.interpret(line);
				}
			}
			
		}).start();
	}
}
