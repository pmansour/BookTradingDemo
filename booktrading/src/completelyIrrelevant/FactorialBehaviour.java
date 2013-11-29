package completelyIrrelevant;

import java.math.BigInteger;



import jade.core.behaviours.Behaviour;

public class FactorialBehaviour extends Behaviour {
	private static final long serialVersionUID = 2362249319916571314L;

	private int number;
	private Callback<BigInteger> callback;
	
	private BigInteger factorial;
	
	public FactorialBehaviour(int number, Callback<BigInteger> callback) {
		this.number = number;
		this.callback = callback;
	}
	
	@Override
	public void onStart() {
		// initialize the factorial
		if(number < 1) {
			factorial = BigInteger.ZERO;
		} else if(number == 1) {
			factorial = BigInteger.ONE;
		} else {
			factorial = BigInteger.valueOf(number--);
		}
	}
	
	@Override
	public void action() {
		factorial = factorial.multiply(BigInteger.valueOf(number--));
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
