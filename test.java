import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class test{

	public static void main(String[] args){
		JFrame frame = new JFrame("Happy Coding");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		CustomComponent custom = new CustomComponent();
                frame.add(custom);
                
                frame.addKeyListener(new KeyListener(){

	        public void keyPressed(KeyEvent ke){
                int kc = ke.getKeyCode();
                if (kc == KeyEvent.VK_SPACE){
                    custom.jump();
                }
			}
				
		public void keyReleased(KeyEvent ke){}
	
		public void keyTyped(KeyEvent ke){}
            });
		
		frame.setSize(300, 300);	
		frame.setVisible(true);
	}
}