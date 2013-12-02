package bookTrading.IO.interpreter;

import java.util.Date;

import bookTrading.ontology.Book;
import bookTrading.seller.BookSeller;

public class BookSellerInterpreter extends BookAgentInterpreter {
	
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
				Book book = interpretBook(tokens[1]);
				double initPrice = Double.parseDouble(tokens[2]);
				double minPrice = Double.parseDouble(tokens[3]);
				long deadline = Long.parseLong(tokens[4]);
				
				// start a sale
				seller.sell(
							book,
							initPrice,
							minPrice,
							new Date(System.currentTimeMillis() + deadline * 1000)
						);
			} catch(Exception e) {
				// we don't care about malformed commands
			}
		}

		// continue interpreting
		return true;
	}
	
	public static String getUsageMessage() {
		return "sell,[bookTitle;author1&author2&author3;bookEditor],[initial price],[minimum price],[deadline (in seconds)]";
	}

}
