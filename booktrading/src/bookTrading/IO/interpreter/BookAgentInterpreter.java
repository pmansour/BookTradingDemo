package bookTrading.IO.interpreter;

public interface BookAgentInterpreter {
	
	static final String EXIT_CODE = "-1";

	/**
	 * Interpret the next line of input.
	 * @param line The next line.
	 * @return true to continue, false to exit.
	 */
	public abstract boolean interpret(String line);
}
