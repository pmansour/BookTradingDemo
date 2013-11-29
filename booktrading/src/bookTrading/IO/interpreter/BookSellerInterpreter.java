package bookTrading.IO.interpreter;

import java.util.Date;

import bookTrading.seller.BookSeller;

public class BookSellerInterpreter implements BookAgentInterpreter {
	
	static String EXIT_CODE = "-1";

	private BookSeller seller;
	
	public BookSellerInterpreter(BookSeller seller) {
		this.seller = seller;
	}

	@Override
	public boolean interpret(String line) {
		// if it's the exit code then exit
		if(line.equalsIgnoreCase(EXIT_CODE)) {
			return false;
		}

		// split it into tokens
		String[] tokens = line.split(",");

		// what we do next depends on the command
		String command = tokens[0];
		if(command.equalsIgnoreCase("sell")) {
			try {
				// get the components
				String title = tokens[1];
				int initPrice = Integer.parseInt(tokens[2]);
				int minPrice = Integer.parseInt(tokens[3]);
				long deadline = Long.parseLong(tokens[4]);
				// start a sale
				seller.sell(
							title,
							initPrice,
							minPrice,
							new Date(System.currentTimeMillis() + deadline * 1000)
						);
			} catch(Exception e) {
				e.printStackTrace(System.err);
			}
		}

		// continue interpreting
		return true;
	}

}
