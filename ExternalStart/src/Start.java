import jade.wrapper.ContainerController;

import java.util.Date;


public class Start {

	public static void main(String[] args) {
		ContainerController main;
		ExternalAgent buyer, seller;
		
		// create a new main container
		main = ExternalAgent.startContainer("localhost", "1099", true);
		
		// create a new external buyer
		buyer = new ExternalAgent(main, "Peter", ExternalAgent.BUYER_CLASS_NAME);
		seller = new ExternalAgent(main, "Amazon", ExternalAgent.SELLER_CLASS_NAME);
		// sell some books
		seller.sellBook("The Wonderful Wizard of Oz", new Date(System.currentTimeMillis() + 10 * 60 * 1000), 100, 25);
		seller.sellBook("The Merchant of Venice", new Date(System.currentTimeMillis() + 10 * 60 * 1000), 120, 50);
		seller.sellBook("Hamlet", new Date(System.currentTimeMillis() + 10 * 60 * 1000), 120, 35);
		// buy a book
		buyer.buyBook("The Wonderful Wizard of Oz", new Date(System.currentTimeMillis() + 60 * 1000), 40);
	}

}
