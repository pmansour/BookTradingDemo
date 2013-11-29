package bookTrading.common;

import java.io.Serializable;

/**
 * The data structure sent through an ACCEPT_PROPOSAL CA.
 * @author peter
 *
 */
public class Proposal implements Serializable {
	private static final long serialVersionUID = 3427391073513708991L;

	private String bookTitle;
	private int price;
	
	public Proposal(String bookTitle, int price) {
		this.bookTitle = bookTitle;
		this.price = price;
	}
	
	public String getBookTitle() {
		return bookTitle;
	}
	public int getPrice() {
		return price;
	}
}
