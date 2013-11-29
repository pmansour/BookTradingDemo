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
	private double price;
	
	public Proposal(String bookTitle, double price) {
		this.bookTitle = bookTitle;
		this.price = price;
	}
	
	public String getBookTitle() {
		return bookTitle;
	}
	public double getPrice() {
		return price;
	}
}
