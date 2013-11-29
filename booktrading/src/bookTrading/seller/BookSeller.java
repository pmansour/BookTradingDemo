package bookTrading.seller;

import java.util.Date;

public interface BookSeller {

	public void sell(String bookTitle, double initPrice, double minPrice, Date deadline);
	
}
