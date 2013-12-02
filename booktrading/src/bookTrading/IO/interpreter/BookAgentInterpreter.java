package bookTrading.IO.interpreter;

import bookTrading.ontology.Book;

public abstract class BookAgentInterpreter {
	
	static final String EXIT_CODE = "-1";

	/**
	 * Interpret the next line of input.
	 * @param line The next line.
	 * @return true to continue, false to exit.
	 */
	public abstract boolean interpret(String line);
	
	/**
	 * Interprets a book string.
	 * @param bookStr A string of the format [title;author1&author2&author3&...;editor]
	 * @return A Book object representing it.
	 */
	public Book interpretBook(String bookStr) {
		String[] bookParts;
		Book book;
		
		// split the given string into the 3 parts
		bookParts = bookStr.split(";");

		// start on the book object
		book = new Book();

		// title is always there first
		book.setTitle(bookParts[0]);

		// followed by authors (separated by &)
		book.setAuthorsArray(bookParts[1].split("&"));

		// finally, it may or may not include an editor
		if(bookParts.length > 2) {
			book.setEditor(bookParts[2]);
		}

		// now return this book
		return book;
	}
}
