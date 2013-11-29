package bookTrading.common;

import java.io.Serializable;
import java.util.Date;

public class BookInfo implements Serializable {
	private static final long serialVersionUID = -3242193306082354418L;
	
	// general stuff
	private String title;
	private Date deadline;
	
	// for buyer agents
	private double maxPrice;
	
	// for seller agents
	private double initPrice;
	private double minPrice;
	
	/**
	 * A BookInfo for a buyer agent.
	 */
	public BookInfo(String title, double maxPrice, Date deadline) {
		this.title = title;
		this.deadline = deadline;

		this.maxPrice = maxPrice;
	}
	/**
	 * A BookInfo for a seller agent.
	 */
	public BookInfo(String title, double initPrice, double minPrice, Date deadline) {
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
