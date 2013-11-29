import jade.wrapper.ContainerController;

import java.util.Date;
import java.util.Scanner;


public class AgentEnvironment {
	
	private static final String EXIT_CODE = "-1";
	
	/*// a minute for a sale, 30 seconds for a purchase
	private static final long SALE_WINDOW = 5 * 60 * 1000;
	private static final long BUY_WINDOW = 2 * 60 * 1000;*/
	
	private static ContainerController main;
	private static ExternalAgent buyer, seller;

	public static void main(String[] args) {
		// create a new main container
		main = ExternalAgent.startContainer("localhost", "1099", true);
		
		// create a new external buyer
		seller = new ExternalAgent(main, "Amazon", ExternalAgent.SELLER_CLASS_NAME);
		buyer = new ExternalAgent(main, "Peter", ExternalAgent.BUYER_CLASS_NAME);
		
		// start interpreting user input
		new Thread(new Runnable() {

			@Override
			public void run() {
				// get input from stdin
				Scanner s = new Scanner(System.in);
				// keep interpreting until the user breaks
				while(interpret(s));
			}
			
		}).start();
		
		/*// sell some books
		seller.sellBook("The Wonderful Wizard of Oz", new Date(System.currentTimeMillis() + SALE_WINDOW), 100, 25);
		seller.sellBook("The Merchant of Venice", new Date(System.currentTimeMillis() + SALE_WINDOW), 120, 50);
		seller.sellBook("Hamlet", new Date(System.currentTimeMillis() + SALE_WINDOW), 120, 35);
		// buy a book
		buyer.buyBook("The Wonderful Wizard of Oz", new Date(System.currentTimeMillis() + BUY_WINDOW), 40);*/
	}

	/**
	 * This encapsulates one step of interpreting user input.
	 * @param s The scanner to use to get the input.
	 * @return true to continue, false to exit.
	 */
	public static boolean interpret(Scanner s) {
		// read the next line.
		String line = s.nextLine();
		
		// if it's the exit code then exit
		if(line.equalsIgnoreCase(EXIT_CODE)) {
			return false;
		}
		
		// split it into tokens
		String[] tokens = line.split(",");
		
		// what we do next depends on the command
		String command = tokens[0];
		if(command.equalsIgnoreCase("buy")) {
			try {
				// get the components
				String title = tokens[1];
				int maxPrice = Integer.parseInt(tokens[2]);
				long deadline = Long.parseLong(tokens[3]);
				// start a purchase
				buyer.buyBook(title, new Date(System.currentTimeMillis() + deadline * 1000), maxPrice);
			} catch(Exception e) {
				e.printStackTrace(System.err);
			}
		} else if(command.equalsIgnoreCase("sell")) {
			try {
				// get the components
				String title = tokens[1];
				int initPrice = Integer.parseInt(tokens[2]);
				int minPrice = Integer.parseInt(tokens[3]);
				long deadline = Long.parseLong(tokens[4]);
				// start a sale
				seller.sellBook(title, new Date(System.currentTimeMillis() + deadline * 1000), initPrice, minPrice);
			} catch(Exception e) {
				e.printStackTrace(System.err);
			}
		}
		
		// continue interpreting
		return true;
	}
}
