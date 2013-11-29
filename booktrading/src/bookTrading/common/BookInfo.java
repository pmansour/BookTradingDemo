package bookTrading.common;

import java.io.Serializable;
import java.util.Date;

public class BookInfo implements Serializable {
	private static final long serialVersionUID = -3242193306082354418L;
	
	// general stuff
	private String title;
	private Date deadline;
	
	// for buyer agents
	private int maxPrice;
	
	// for seller agents
	private int initPrice;
	private int minPrice;
	
	/**
	 * A BookInfo for a buyer agent.
	 */
	public BookInfo(String title, Date deadline, int maxPrice) {
		this.title = title;
		this.deadline = deadline;

		this.maxPrice = maxPrice;
	}
	/**
	 * A BookInfo for a seller agent.
	 */
	public BookInfo(String title, Date deadline, int initPrice, int minPrice) {
		this.title = title;
		this.deadline = deadline;
		
		this.initPrice = initPrice;
		this.minPrice = minPrice;
	}
	
	public String getTitle() {
		return title;
	}
	public Date getDeadline() {
		return deadline;
	}

	public int getMaxPrice() {
		return maxPrice;
	}
	
	public int getInitPrice() {
		return initPrice;
	}
	public int getMinPrice() {
		return minPrice;
	}
}
