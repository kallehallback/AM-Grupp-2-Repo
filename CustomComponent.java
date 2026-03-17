import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class CustomComponent extends JPanel{
	
	int circleX = 0;
	int circleY = 240;
	
	public CustomComponent(){
		setBackground(new Color(0, 255, 255));
        
		new Timer(16, new ActionListener(){
			public void actionPerformed(ActionEvent e){
				step();
				repaint();
			}
		}).start();
	}
	
	private void step(){
		if(circleY < 240){
			circleY++;
		}
	}
    public void jump(){
        circleY = circleY - 10;
    }

	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		g.setColor(Color.RED);
		g.fillOval(150 - 10, circleY, 20, 20);
	}
}