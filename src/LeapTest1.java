import java.io.IOException;
import java.io.OutputStream;

import com.fazecast.jSerialComm.*;
import java.util.Random;
import com.leapmotion.leap.*;
import com.leapmotion.leap.Gesture.State;

class LeapListener extends Listener
{
	private static SerialPort p = SerialPort.getCommPort("COM3");
	private Vector targetPoint;
	private double alpha = 0.33;
	private double filtered_proximity = 0.0;
	
	public void onInit(Controller controller) {
		//System.out.println("Initialised");
		
		//Set up connection to Arduino
		p.setBaudRate(115200);
		p.openPort();
		
		try {
			Thread.sleep(5000); //wait for 5 seconds
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		//Random object for creating random target point for given trial of experiment
		Random r = new Random();
		
		/*Values for max and min target point x, y and z coordinates 
		  (based on InteractionBox values - possibly unnecessary)*/
		int maxY = 200;
		int minY = 70;
		
		int maxX = 100;
		int minX = -100;
		
		int maxZ = 75;
		int minZ = -75;
		
		//Values for target point: x-coordinate, y-coordinate, and z-coordinate
		float x = minX + r.nextFloat() * (maxX - minX);
		float y = minY + r.nextFloat() * (maxY - minY);
		float z = minZ + r.nextFloat() * (maxZ - minZ);
		
		targetPoint = new Vector(x, y, z); //the point the user has to find
		
	}
	
	public void onConnect(Controller controller) {
		//System.out.println("Connected to sensor");
	}
	
	public void onDisconnect(Controller controller) {
		//System.out.println("Sensor disconnected");
	}
	
	public void onExit(Controller controller) {
		//System.out.println("Exited");
	}
	
	public void onFrame(Controller controller) {
		//frames are processed here - data for each frame
		Frame frame = controller.frame(); //frame from the controller
		
		//Interaction box measurements - not needed in final program
		//InteractionBox ib = frame.interactionBox();
		//System.out.println("Box height: " + ib.height());
		//System.out.println("Box width: " + ib.width());
		//System.out.println("Box depth: " + ib.depth());
		
		//Isolate end of middle finger, store position as vector
		FingerList fingers = frame.fingers();
		Finger middle = fingers.get(2);
		Bone bone = middle.bone(Bone.Type.TYPE_DISTAL);
		Vector tracking = bone.nextJoint();
		
		//proximity of finger tip to target
		float proximity = tracking.distanceTo(targetPoint);
		
		//Exponential Moving Average
		filtered_proximity = (alpha * proximity) + (1.0 - alpha) * filtered_proximity;
		
		//define a radius around the target point and calculate closeness score within boundary of this area
		float sphereRadius = 150;
		float closenessPercentage = (float) (filtered_proximity / sphereRadius) * 100;
		
		if (closenessPercentage > 100) {
			closenessPercentage = 0; //score is 0 if user's finger tip is outside sphere's radius
		}
		
		//Information for testing purposes: positions of target and finger, proximity, and sphere-based score
		//System.out.println("TARGET: " + targetPoint.getX() + "," + targetPoint.getY() + "," + targetPoint.getZ());
		//System.out.printf("TRACKING: %.2f, %.2f, %.2f\n", tracking.getX(), tracking.getY(), tracking.getZ());
		//System.out.println("Proximity = " + proximity);
		//System.out.println("Filtered Proximity = " + filtered_proximity);
		System.out.println("Closeness score = " + closenessPercentage);
		//System.out.println("-------------------------------------");
		//System.out.printf("%05.1f,%05.1f\n", proximity, filtered_proximity);
		
		
		//Output brightness value to Arduino, reflecting closeness percentage
		String brightCmd = "2";
		OutputStream output = p.getOutputStream();
		
		int lightsOff = 0; //when lights off, rgb values all = 0
		int fullBrightness = 255; //at full brightness, all rgb values = 255
		int brightness = (int) simple_interpolate(lightsOff, fullBrightness, closenessPercentage);
		brightCmd += " " + Integer.toString(brightness) + " " + Integer.toString(brightness) + " " + Integer.toString(brightness) + "\n";
		
		try 
		{
			output.write(brightCmd.getBytes());
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		
	}
	
	public float simple_interpolate(int a, int b, float pct) {
	    return a + (b - a) * pct;
	}
}

public class LeapTest1 {

	public static void main(String[] args) {
		
		LeapListener listener = new LeapListener();
		Controller controller = new Controller();
		
		controller.addListener(listener);
		
		//System.out.println("Press enter to quit");
		
		try {
			System.in.read();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		controller.removeListener(listener);

	}

}
