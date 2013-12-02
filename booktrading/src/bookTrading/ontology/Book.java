package bookTrading.ontology;

import jade.content.Concept;
import jade.util.leap.ArrayList;
import jade.util.leap.List;

public class Book implements Concept {
	private static final long serialVersionUID = -369440771173776399L;

	private String title;
	private List authors;
	private String editor;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List getAuthors() {
		return authors;
	}
	public void setAuthors(List authors) {
		this.authors = authors;
	}
	public void setAuthorsArray(String... authors) {
		this.authors = new ArrayList(authors.length);
		for(String author : authors) {
			this.authors.add(author);
		}
	}
	public String getEditor() {
		return editor;
	}
	public void setEditor(String editor) {
		this.editor = editor;
	}
	
	// returns a human-readable string for use in debugging
	@Override
	public String toString() {
		StringBuilder sb;
		
		sb = new StringBuilder();
		
		// start with the title
		sb.append('"');
		sb.append(title);
		sb.append('"');
		
		// then add the authors
		sb.append(" by ");
		Object[] authors = this.authors.toArray();
		for(Object author : authors) {
			sb.append(author);
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		
		// finally, add the editor (if they exist)
		if(editor != null) {
			sb.append(" and edited by ");
			sb.append(editor);
		}
	
		// and return this string
		return sb.toString();
	}
	
}
