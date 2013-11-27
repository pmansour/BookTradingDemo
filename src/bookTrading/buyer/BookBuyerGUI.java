package bookTrading.buyer;

/**
 * The GUI that will be used to show the status of a book-buyer agent.
 * @author peter
 *
 */
public abstract class BookBuyerGUI {

	protected BookBuyerAgent agent;
	
	/**
	 * Create a new GUI for the given agent.
	 */
	public BookBuyerGUI(BookBuyerAgent agent) {
		this.agent = agent;
	}
	
	public abstract void show();
	public abstract void hide();
	
	public abstract void notifyUser(String message);
}
