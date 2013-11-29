import jade.wrapper.ContainerController;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import bookTrading.IO.interpreter.BookAgentInterpreter;
import bookTrading.IO.interpreter.BookBuyerInterpreter;
import bookTrading.IO.interpreter.BookSellerInterpreter;


public class AgentEnvironment {

	public static void main(String[] args) {
		ContainerController main;
		ExternalAgent buyer, seller;
		final Map<String, BookAgentInterpreter> interpreters;
		
		// create a new main container
		main = ExternalAgent.startContainer("localhost", "1099", true);
		
		// create some new external agents
		buyer = new ExternalAgent(main, "Peter", ExternalAgent.BUYER_CLASS_NAME);
		seller = new ExternalAgent(main, "Amazon", ExternalAgent.SELLER_CLASS_NAME);
		
		// start the RMA
		new ExternalAgent(main, "rma", "jade.tools.rma.rma");
		
		// create some interpreters
		interpreters = new HashMap<String, BookAgentInterpreter>(2);
		interpreters.put("peter", new BookBuyerInterpreter(buyer));
		interpreters.put("amazon", new BookSellerInterpreter(seller));
		
		// start interpreting user input
		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean continuePolling = true;
				
				// get input from stdin
				Scanner s = new Scanner(System.in);
				
				// keep interpreting until there's no more input, or until
				// neither agents want to continue
				while(s.hasNextLine() && continuePolling) {
					// get the next line
					String line = s.nextLine();
					
					// if the line includes a colon (:)
					if(line.contains(":")) {
						// split it at the colon
						String[] tokens = line.split(":");
						
						// the first token should say which agent is meant by this message
						String agent = tokens[0].toLowerCase();
						
						// let the agent meant by it interpret it
						continuePolling = interpreters.get(agent).interpret(tokens[1]);
					// otherwise
					} else {
						// let all the agents interpret it
						for(BookAgentInterpreter interpreter : interpreters.values()) {
							if(!interpreter.interpret(line)) {
								continuePolling = false;
							}
						}
					}
				}
			}
			
		}).start();
	}
}
