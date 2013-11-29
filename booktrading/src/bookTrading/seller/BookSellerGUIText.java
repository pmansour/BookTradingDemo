package bookTrading.seller;

import java.util.Date;
import java.util.Scanner;

public class BookSellerGUIText extends BookSellerGUI {
	private static final String EXIT_CODE = "-s";
	
	private Thread interpreter;
	private String name;

	public BookSellerGUIText(BookSellerAgent agent) {
		super(agent);
		this.name = agent.getAID().getLocalName();
	}
	
	@Override
	public void show() {
		// print some welcome text
		System.out.println(name + ": Welcome to the Book Seller! My name is " + name);
		printUsage();
		// start the interpreter (in another thread)
		interpreter = new Thread(new Runnable() {
			@Override
			public void run() {
				// start listening at stdin
				Scanner s = new Scanner(System.in);
				while(true) {
					// read the next line.
					String line = s.nextLine();
					
					// if it's the exit code then exit
					if(line.equalsIgnoreCase(EXIT_CODE)) {
						break;
					}
					
					// split it into tokens
					String[] tokens = line.split(",");
					
					// we're only concerned if the first token is 'sell'
					if(tokens[0].equalsIgnoreCase("sell") == false) {
						continue;
					}
					
					// parse the input string
					try {
						// get the components
						String title = tokens[1];
						int initPrice = Integer.parseInt(tokens[2]);
						int minPrice = Integer.parseInt(tokens[3]);
						long deadline = Long.parseLong(tokens[4]);
						// start a new task for the agent
						agent.putForSale(title, initPrice, minPrice, new Date(System.currentTimeMillis() + (deadline * 1000)));						
					} catch(Exception e) {
						// if it's invalid, print the usage again
						printUsage();
					}
				}
			}
		});
		interpreter.start();
	}
	
	public void printUsage() {
		System.out.println(name + ": To sell a book, please enter a line in the following form:");
		System.out.println(name + ": sell,[book title],[initial price],[minimum price],[deadline (in seconds)]");
		System.out.println(name + ": To exit, please type in '" + EXIT_CODE + "'");		
	}

	@Override
	public void hide() {
		// stop the interpreter
		interpreter.interrupt();
	}

	@Override
	public void notifyUser(String message) {
		System.out.println(name + ": " + message);
	}

}
