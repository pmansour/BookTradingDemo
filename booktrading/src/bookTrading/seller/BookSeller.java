package bookTrading.seller;

import java.util.Date;

import bookTrading.ontology.Book;

public interface BookSeller {

	public void sell(Book book, double initPrice, double minPrice, Date deadline);
	
}
