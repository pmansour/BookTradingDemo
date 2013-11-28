package bookTrading.behaviours;

import bookTrading.common.Callback;

import jade.core.behaviours.Behaviour;

public class FactorialBehaviour extends Behaviour {
	private static final long serialVersionUID = 2362249319916571314L;

	private int number;
	private Callback<Long> callback;
	
	private long factorial;
	
	public FactorialBehaviour(int number, Callback<Long> callback) {
		this.number = number;
		this.callback = callback;
	}
	
	@Override
	public void onStart() {
		// initialize the factorial
		if(number < 1) {
			factorial = 0;
		} else if(number == 1) {
			factorial = 1;
		} else {
			factorial = number--;
		}
	}
	
	@Override
	public void action() {
		this.factorial *= number--;
	}

	@Override
	public boolean done() {
		return number < 1;
	}
	
	@Override
	public int onEnd() {
		// call the callback method with the result
		callback.done(this.factorial);
		
		return 0;
	}

}
