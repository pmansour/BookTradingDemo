package bookTrading.common;

import java.io.Serializable;
import java.util.Date;

import bookTrading.ontology.Book;

public class BookInfo implements Serializable {
	private static final long serialVersionUID = -3242193306082354418L;
	
	// general stuff
	private Book book;
	private Date deadline;
	
	// for buyer agents
	private double maxPrice;
	
	// for seller agents
	private double initPrice;
	private double minPrice;
	
	/**
	 * A BookInfo for a buyer agent.
	 */
	public BookInfo(Book book, double maxPrice, Date deadline) {
		this.book = book;
		this.deadline = deadline;

		this.maxPrice = maxPrice;
	}
	/**
	 * A BookInfo for a seller agent.
	 */
	public BookInfo(Book book, double initPrice, double minPrice, Date deadline) {
		this.book = book;
		this.deadline = deadline;
		
		this.initPrice = initPrice;
		this.minPrice = minPrice;
	}
	
	public Book getBook() {
		return book;
	}
	public Date getDeadline() {
		return deadline;
	}

	public double getMaxPrice() {
		return maxPrice;
	}
	
	public double getInitPrice() {
		return initPrice;
	}
	public double getMinPrice() {
		return minPrice;
	}
}
