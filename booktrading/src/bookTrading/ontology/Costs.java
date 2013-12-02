package bookTrading.ontology;

import jade.content.Predicate;

public class Costs implements Predicate {
	private static final long serialVersionUID = 3029124398241862427L;

	private Book item;
	private int price;
	
	public Book getItem() {
		return item;
	}
	public void setItem(Book item) {
		this.item = item;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	
	
}
