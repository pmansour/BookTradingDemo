package bookTrading.IO.interpreter;

import java.util.Date;

import bookTrading.buyer.BookBuyer;

public class BookBuyerInterpreter implements BookAgentInterpreter {
	
	static String EXIT_CODE = "-1";
	
	private BookBuyer buyer;

	public BookBuyerInterpreter(BookBuyer buyer) {
		this.buyer = buyer;
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
		if(command.equalsIgnoreCase("buy")) {
			try {
				// get the components
				String title = tokens[1];
				double maxPrice = Double.parseDouble(tokens[2]);
				long deadline = Long.parseLong(tokens[3]);
				// start a purchase
				buyer.buy(
							title,
							maxPrice,
							new Date(System.currentTimeMillis() + deadline * 1000)
						);
			} catch(Exception e) {
				//e.printStackTrace(System.err);
			}
		}

		// continue interpreting
		return true;
	}

}
