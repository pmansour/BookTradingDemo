package bookTrading.ontology;

public interface BookTradingVocabulary {
	// concept: Book { Title, Authors, Editor }
	public static final String BOOK = "Book";
	public static final String BOOK_TITLE = "title";
	public static final String BOOK_AUTHORS = "authors";
	public static final String BOOK_EDITOR = "editor";
	
	// predicate: Costs(Item, Price)
	public static final String COSTS = "Costs";
	public static final String COSTS_ITEM = "item";
	public static final String COSTS_PRICE = "price";
	
	// action: Sell (Item)
	public static final String SELL = "Sell";
	public static final String SELL_ITEM = "item";
}
