package bookTrading.buyer;

public class BookBuyerGUITextReadOnly extends BookBuyerGUI {
	private String name;

	public BookBuyerGUITextReadOnly(BookBuyerAgent agent) {
		super(agent);
		this.name = agent.getAID().getLocalName();
	}
	
	@Override
	public void show() {
		// print some welcome text
		notifyUser("Welcome to the Book Buyer! My name is " + name);
		printUsage();
	}
	
	public void printUsage() {
		notifyUser("To purchase a book, please enter a line in the following form:");
		notifyUser("buy,[book title],[maximum price],[deadline (in seconds)]");
	}

	@Override
	public void hide() {
		// Don't need to do anything.
	}

	@Override
	public void notifyUser(String message) {
		System.out.println(name + ": " + message);
	}

}
