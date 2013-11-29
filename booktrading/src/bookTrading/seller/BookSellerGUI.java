package bookTrading.seller;

/**
 * The GUI that will be used to show the status of a book-seller agent.
 * @author peter
 *
 */
public abstract class BookSellerGUI {

	protected BookSellerAgent agent;
	
	/**
	 * Create a new GUI for the given agent.
	 */
	public BookSellerGUI(BookSellerAgent agent) {
		this.agent = agent;
	}
	
	public abstract void show();
	public abstract void hide();
	
	public abstract void notifyUser(String message);
}
