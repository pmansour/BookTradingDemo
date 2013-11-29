package bookTrading.seller;

import java.util.Date;

public interface BookSeller {

	public void sell(String bookTitle, int initPrice, int minPrice, Date deadline);
	
}
