package bookTrading.ontology;

import jade.content.AgentAction;

public class Sell implements AgentAction {
	private static final long serialVersionUID = -4680534607994599298L;

	private Book item;

	public Book getItem() {
		return item;
	}

	public void setItem(Book item) {
		this.item = item;
	}
	
	
}
