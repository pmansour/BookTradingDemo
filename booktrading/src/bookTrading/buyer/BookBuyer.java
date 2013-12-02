package bookTrading.buyer;

import java.util.Date;

import bookTrading.ontology.Book;

public interface BookBuyer {

	public void buy(Book book, double maxPrice, Date deadline);
	
}
