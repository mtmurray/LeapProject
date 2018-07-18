import java.io.IOException;
import java.io.OutputStream;

import com.fazecast.jSerialComm.*;
import java.util.Random;
import java.util.Scanner;

import com.leapmotion.leap.*;
import com.leapmotion.leap.Gesture.State;

class LeapListener extends Listener
{
	private static SerialPort p = SerialPort.getCommPort("COM3");
	private Vector targetPoint;
	private double alpha = 0.60;
	private double filtered_proximity = 0.0;
	private String trial_type;
	private double previous_proximity = 0.0;
	
	public void onInit(Controller controller) {
		//Establish trial type: brightness ("b"), colour ("c"), or position ("p")
		System.out.println("Please specify feedback type for this trial (b, c, p):");
		Scanner in = new Scanner(System.in);
		trial_type = in.nextLine();
		
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
		
		OutputStream output = p.getOutputStream();
		
		try 
		{
			output.write("3".getBytes());
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void onFrame(Controller controller) {
		//frames are processed here - data for each frame
		Frame frame = controller.frame(); //frame from the controller
		
		//Interaction box measurements
		//InteractionBox ib = frame.interactionBox();
		//System.out.println("Box height: " + ib.height());
		//System.out.println("Box width: " + ib.width());
		//System.out.println("Box depth: " + ib.depth());
		
		//Isolate end of middle finger, store position as vector
		//FingerList fingers = frame.fingers();
		//Bone bone = fingers.get(2).bone(Bone.Type.TYPE_DISTAL);
		
		HandList hands = frame.hands();
		Hand hand = hands.get(0);
		Vector tracking = hand.palmPosition();
		
		//proximity of finger tip to target
		float proximity = tracking.distanceTo(targetPoint);
		
		//Exponential Moving Average
		filtered_proximity = (alpha * proximity) + (1.0 - alpha) * filtered_proximity;
		
		//define a radius around the target point and calculate closeness score within boundary of this area
		float sphereRadius = 150;
		float closenessPercentage = (float) (filtered_proximity / sphereRadius) * 100;
		
		if (filtered_proximity > sphereRadius) {
			closenessPercentage = 0; //score is 0 if user's finger tip is outside sphere's radius
		}
		
		//Information for testing purposes
		//System.out.println("TARGET: " + targetPoint.getX() + "," + targetPoint.getY() + "," + targetPoint.getZ());
		//System.out.printf("TRACKING: %.2f, %.2f, %.2f\n", tracking.getX(), tracking.getY(), tracking.getZ());
		//System.out.println("Proximity = " + proximity);
		//System.out.println("Filtered Proximity = " + filtered_proximity);
		//System.out.println("Closeness score = " + closenessPercentage);
		//System.out.println("-------------------------------------");
		//System.out.printf("%05.1f,%05.1f\n", proximity, filtered_proximity);
		
		if (trial_type.equals("b") && ((filtered_proximity >= previous_proximity + 5 || filtered_proximity <= previous_proximity - 5)))
		{
			updateBrightness(closenessPercentage);
		}
		else if (trial_type.equals("c") && ((filtered_proximity >= previous_proximity + 5 || filtered_proximity <= previous_proximity - 5))) 
		{
			updateColour(closenessPercentage);
		}
		else if (trial_type.equals("p")) 
		{
			//yet to implement position feedback
		}
		else 
		{
			return;
		}
		
		//set current trial's proximity to act as previous proximity during next trial
		previous_proximity = filtered_proximity;
	}
	
	public void updateBrightness(float pct)
	{
		//Output brightness value to Arduino, reflecting closeness percentage
		String brightCmd = "2";
		OutputStream output = p.getOutputStream();
		
		int lightsOff = 0; //when lights off, rgb values all = 0
		int fullBrightness = 255; //at full brightness, all rgb values = 255
		int brightness = (int) simple_interpolate(lightsOff, fullBrightness, pct);
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
	
	public void updateColour(float pct) 
	{
		String colourCmd = "2";
		OutputStream output = p.getOutputStream();
		
		int blue = 0; //level of blue does not need to change
		
		//red rg values
		int r1 = 255;
		int g1 = 0;
		
		//green rg values
		int r2 = 0;
		int g2 = 255;
		
		//values to update LEDs
		int newR = (int) simple_interpolate(r1, r2, pct);
		int newG = (int) simple_interpolate(g1, g2, pct);
		
		colourCmd += " " + Integer.toString(newR) + " " + Integer.toString(newG) + " " + Integer.toString(blue);
		
		try 
		{
			output.write(colourCmd.getBytes());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public float simple_interpolate(int a, int b, float pct) {
	    return a + (b - a) * (pct/100);
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
