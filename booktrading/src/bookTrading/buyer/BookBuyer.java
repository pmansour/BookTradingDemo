package bookTrading.buyer;

import java.util.Date;

public interface BookBuyer {

	public void buy(String bookTitle, int maxPrice, Date deadline);
	
}
