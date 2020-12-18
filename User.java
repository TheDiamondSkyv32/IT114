package client;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JEditorPane;

public class User extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 4007176387123505845L;
	private String name;
    private JEditorPane nameField;

    public User(String name, String wrapper) {
    	this.name = name;
    	nameField = new JEditorPane();
    	nameField.setContentType("text/html");
    	nameField.setText(String.format(wrapper,  name));
    	nameField.setEditable(false);
    	this.setLayout(new BorderLayout());
    	this.add(nameField);
    }


	public void setName(String name, String wrapper) {
    	nameField.setText(String.format(wrapper,  name));
    }

    public String getName() {
    	return name;
    }
}
