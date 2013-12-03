import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Scanner;

import junit.framework.TestCase;


public class SimpleTest extends TestCase {

	private PrintStream oldStdOut;
	private InputStream oldStdIn;
	
	private AgentEnvironment env;
	
	private OutputStream toStdIn;
	private InputStream fromStdOut;
	private PrintStream printToStdIn;
	private Scanner readFromStdOut;
	
	@Override
	protected void setUp() {
		AgentEnvironment.Settings settings;
		PipedOutputStream src;
		PipedInputStream snk;
		
		// create the settings for this test agent environment
		settings = new AgentEnvironment.Settings();
		
		settings.setAgentContainer(false);
		
		settings.setMainHost("localhost");
		settings.setMainPort("1099");
		
		settings.setBuyers(Arrays.asList(new String[] {"Peter", "Michael", "Adrian"}));
		settings.setSellers(Arrays.asList(new String[] {"Amazon", "GoogleBooks", "DSTOLibrary"}));
		
		// start the agent environment
		env = new AgentEnvironment(settings);
		
		
		// save the old stdin/out
		oldStdIn = System.in;
		oldStdOut = System.out;
		
		// do some forwarding stuff
		try {
			// create a pipe
			src = new PipedOutputStream();
			snk = new PipedInputStream();
			src.connect(snk);
			// forward our custom output stream to the pipe
			toStdIn = src;
			printToStdIn = new PrintStream(toStdIn);
			// forward the pipe to stdin
			System.setIn(snk);
			
			// create another pipe
			src = new PipedOutputStream();
			snk = new PipedInputStream();
			src.connect(snk);
			// forward stdout to the pipe
			System.setOut(new PrintStream(src));
			// forward the pipe to our custom input stream
			fromStdOut = snk;
			readFromStdOut = new Scanner(fromStdOut);
		} catch(IOException e) {
			// we can't go on
			e.printStackTrace(System.err);
			return;
		}
	}
	@Override
	protected void tearDown() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				// wait a few seconds for agents to finish what they're doing first
				try {
					Thread.sleep(5000);
				} catch(InterruptedException ie) {
					return;
				}
				
				// we no longer need the agent environment
				env.kill();
				env = null;
				
				// but bring back the old streams
				System.setIn(oldStdIn);
				System.setOut(oldStdOut);
			}
			
		}).start();
	}
	
	/**
	 * Make Amazon sell a book and Peter buy it, then make sure it's been sold.
	 * Requires either:
	 * 	-> Monitoring and writing to stdin and stdout, or
	 * 	-> Some other method of observability.
	 */
	public void testSuccessfulLocalSale() {
		boolean bought = false, sold = false;
		
		// send the messages in a few seconds
		new Thread(new Runnable() {
			@Override
			public void run() {
				// wait some time to let them start up
				try {
					Thread.sleep(2500);
				} catch (InterruptedException e) {
					e.printStackTrace(System.err);
					assertTrue(false);
				}
				
				// tell Amazon to sell Hamlet
				printToStdIn.println("Amazon:sell,Hamlet;Shakespeare,50,20,60");
				System.err.println("Amazon:sell,Hamlet;Shakespeare,50,20,60");
				// tell Peter to buy it
				printToStdIn.println("Peter:buy,Hamlet;Shakespeare,40,60");
				System.err.println("Peter:buy,Hamlet;Shakespeare,40,60");
			}
		}).start();
		
		
		// monitor standard out
		while(readFromStdOut.hasNextLine() && (!bought || !sold)) {
			String line = readFromStdOut.nextLine();
			
			System.err.println(line);
			
			// if the book was bought successfully
			if(line.contains("Book \"Hamlet\" by Shakespeare was bought for")) {
				bought = true;
				System.err.println("Hamlet was bought successfully!");
			}
			// if the book was sold successfully
			else if(line.contains("Book \"Hamlet\" by Shakespeare has been sold for")) {
				sold = true;
				System.err.println("Hamlet was sold successfully!");
			}
			
			// but we don't want any failures
			assertFalse(line.contains("Could not buy book \"Hamlet\" by Shakespeare before the deadline."));
			assertFalse(line.contains("Could not sell the book \"Hamlet\" by Shakespeare before the deadline."));
		}
	}
}
