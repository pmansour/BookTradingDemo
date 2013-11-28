package bookTrading.test;

import bookTrading.behaviours.FactorialBehaviour;
import bookTrading.common.Callback;
import jade.core.Agent;

/**
 * Used to try out a generic behaviour.
 * @author peter
 *
 */
public class FactorialAgent extends Agent {
	private static final long serialVersionUID = 5117183288177602198L;

	@Override
	protected void setup() {
		setupFactorialBehaviour();
	}
	
	private void setupFactorialBehaviour() {
		final int number;
		
		// find the factorial number
		try {
			// get the agent arguments
			Object[] arguments = getArguments();
			// we need to actually have arguments
			if(arguments == null || arguments.length < 1) {
				throw new IllegalArgumentException("This agent needs at least 1 argument.");
			}
			// specifically, we need the first argument
			Object arg1 = arguments[0];
			// which needs to either be an Integer
			if(arg1 instanceof Integer) {
				number = (Integer) arg1;
			// or a string representation of an Integer
			} else if(arg1 instanceof String) {
				try {
					number = Integer.parseInt((String) arg1);
				} catch(NumberFormatException e) {
					throw new IllegalArgumentException("The first argument needs to be an Integer.");
				}
			// anything else is a heresy!
			} else {
				throw new IllegalArgumentException("The first argument needs to be an Integer.");
			}
			
		} catch(IllegalArgumentException e) {
			// print an error message
			System.err.println(e.getMessage());
			// suicide
			this.doDelete();
			// [just in case]
			return;
		}
		
		// add the factorial behaviour
		addBehaviour(new FactorialBehaviour(number, new Callback<Long>() {

			@Override
			public void done(Long factorial) {
				System.out.println(number + "! = " + factorial);
			}
			
		}));
	}
}
