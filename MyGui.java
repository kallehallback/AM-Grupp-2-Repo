import javax.swing.JFrame;
import javax.swing.JButton;

public class MyGui{
    private JFrame frame = new JFrame("Happy coding");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.setSize(300, 300);
	
	public void show(){
		frame.setVisible(true);	
	}
    public void button(){
        JButton button = new JButton("Click me!");
        frame.add(button);

    }

}