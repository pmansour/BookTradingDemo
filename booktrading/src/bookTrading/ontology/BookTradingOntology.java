package bookTrading.ontology;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.AgentActionSchema;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PredicateSchema;
import jade.content.schema.PrimitiveSchema;

public class BookTradingOntology extends Ontology implements BookTradingVocabulary {
	private static final long serialVersionUID = -1912376119497641146L;

	// the name identifying this ontology
	public static final String ONTOLOGY_NAME = "BookTradingOntology";
	
	
	// singleton
	private static Ontology _instance = new BookTradingOntology();
	public static final Ontology getInstance() {
		return _instance;
	}
	
	// private constructor so no one can create this from anywhere else
	private BookTradingOntology() {
		// this ontology extends the basic one
		super(ONTOLOGY_NAME, BasicOntology.getInstance());
		
		try {
			// add the schemas
			add(new ConceptSchema(BOOK), Book.class);
			add(new PredicateSchema(COSTS), Costs.class);
			add(new AgentActionSchema(SELL), Sell.class);
			
			// define their structure
			
			// the book concept
			ConceptSchema cs = (ConceptSchema) getSchema(BOOK);
			cs.add(BOOK_TITLE, (PrimitiveSchema) getSchema(BasicOntology.STRING));
			cs.add(BOOK_AUTHORS, (PrimitiveSchema) getSchema(BasicOntology.STRING), 0, ObjectSchema.UNLIMITED);
			cs.add(BOOK_EDITOR, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			
			// the costs predicate
			PredicateSchema ps = (PredicateSchema) getSchema(COSTS);
			ps.add(COSTS_ITEM, (ConceptSchema) getSchema(BOOK));
			ps.add(COSTS_PRICE, (PrimitiveSchema) getSchema(BasicOntology.FLOAT));
			
			// the sell agent action
			AgentActionSchema as = (AgentActionSchema) getSchema(SELL);
			as.add(SELL_ITEM, (ConceptSchema) getSchema(BOOK));
		} catch(OntologyException oe) {
			oe.printStackTrace(System.err);
		}
	}
}
