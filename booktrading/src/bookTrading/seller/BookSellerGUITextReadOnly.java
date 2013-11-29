package bookTrading.seller;

public class BookSellerGUITextReadOnly extends BookSellerGUI {
	
	private String name;

	public BookSellerGUITextReadOnly(BookSellerAgent agent) {
		super(agent);
		this.name = agent.getAID().getLocalName();
	}
	
	@Override
	public void show() {
		// print some welcome text
		notifyUser("Welcome to the Book Seller! My name is " + name);
		printUsage();
	}
	
	public void printUsage() {
		notifyUser("To sell a book, please enter a line in the following form:");
		notifyUser("sell,[book title],[initial price],[minimum price],[deadline (in seconds)]");
	}

	@Override
	public void hide() {
		// Don't need to do anything
	}

	@Override
	public void notifyUser(String message) {
		System.out.println(name + ": " + message);
	}

}
