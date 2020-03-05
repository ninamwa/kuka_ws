package API_ROS2_Sunrise;
import java.net.InetSocketAddress;


// Implemented classes

// RoboticsAPI
import API_ROS2_Sunrise.DataController;
import API_ROS2_Sunrise.ISocket;
import API_ROS2_Sunrise.TCPSocket;
import API_ROS2_Sunrise.UDPSocket;

import com.kuka.nav.fdi.FDIConnection;




public class KMP_sensor_reader {
	
	// Runtime variables
	volatile Boolean KMP_laser_requested;
	volatile Boolean KMP_odometry_requested;
	public volatile boolean closed = false;
	
    // Data retrieval socket via FDI 
    public FDIConnection fdi;
	public DataController listener;
	String FDI_IP = "172.31.1.102";
	int FDI_port = 34001; 
	
	// Laser ports on controller
	private static int laser_B1 = 1801;
	private static int laser_B4 = 1802;
	
	// Socket
	public ISocket laser_socket;
	public ISocket odometry_socket;
	int KMP_laser_port = 30005;
	int KMP_odometry_port = 30006;
	String LaserConnectionType;
	String OdometryConnectionType;
	
	
	public KMP_sensor_reader(int laserport, int odomport, String LaserConnectionType, String OdometryConnectionType) {
		this.KMP_laser_port = laserport;
		this.KMP_odometry_port = odomport;
		this.LaserConnectionType = LaserConnectionType;
		this.OdometryConnectionType = OdometryConnectionType;
		createLaserSocket();
		createOdometrySocket();

		KMP_laser_requested = true;
		KMP_odometry_requested = true;
		
		if (!(isLaserSocketConnected())) {
			KMP_laser_requested=false;
			System.out.println("Starting thread to connect laser node....");
			Thread monitorLaserConnections = new MonitorLaserConnectionThread();
			monitorLaserConnections.start();
			}
				
		if (!(isOdometrySocketConnected())) {
			KMP_odometry_requested=false;
			System.out.println("Starting thread to connect odometry node....");
			Thread monitorOdometryConnections = new MonitorOdometryConnectionThread();
			monitorOdometryConnections.start();
			}
		
		if (KMP_laser_requested || KMP_odometry_requested) { 
			this.fdiConnection();
		}
	}
	
	public void fdiConnection(){
		listener = new DataController(laser_socket, odometry_socket);
		InetSocketAddress fdi_adress = new InetSocketAddress(FDI_IP,FDI_port);
		this.fdi = new FDIConnection(fdi_adress);
		this.fdi.addConnectionListener(listener);
		this.fdi.addDataListener(listener);
		this.fdi.connect();
	}
	
	public void createLaserSocket(){
		if(LaserConnectionType == "TCP") {
			this.laser_socket = new TCPSocket(KMP_laser_port);

		}else {
			this.laser_socket = new UDPSocket(KMP_laser_port);
		}
	}
	
	public void createOdometrySocket(){
		if(OdometryConnectionType == "TCP") {
			this.odometry_socket = new TCPSocket(KMP_odometry_port);

		}else {
			this.odometry_socket = new UDPSocket(KMP_odometry_port);
		}
	}
	
	public class MonitorLaserConnectionThread extends Thread {
		public void run(){
			while(!(isLaserSocketConnected()) && (!(closed))) {

				createLaserSocket();
				if(isLaserSocketConnected()){
					break;
				}	
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					System.out.println("");
				}
			}
			if(!closed){
				KMP_laser_requested = true;
				if (fdi==null) {
					fdiConnection();
				}
				listener.setLaserSocket(laser_socket);
				subscribe_kmp_laser_data();	
				System.out.println("Connection with KMP Laser Node OK!");

				}	
		}
	}
	
	public class MonitorOdometryConnectionThread extends Thread {
		public void run(){
			while(!(isOdometrySocketConnected()) && (!(closed))) {
				
				createOdometrySocket();
				if (isOdometrySocketConnected()){
					break;
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					System.out.println("");
				}
			
		}	
			if(!closed){
				KMP_odometry_requested = true;
				if (fdi==null) {
					fdiConnection();
				}
				listener.setOdometrySocket(odometry_socket);
				subscribe_kmp_odometry_data();
				System.out.println("Connection with KMP Odometry Node OK!");
				}
		}
	}
	
	public void subscribe_kmp_odometry_data() {
		if (KMP_odometry_requested){
    		fdi.getNewOdometry();
		}
	}
	public void subscribe_kmp_laser_data(){
    	if (KMP_laser_requested){
    		fdi.getNewLaserScan(laser_B1);
    		fdi.getNewLaserScan(laser_B4);
    	}
	}
	
	public void start() {
		if(KMP_laser_requested) {
			subscribe_kmp_laser_data();
		}
		if(KMP_odometry_requested) {
			subscribe_kmp_odometry_data();
		}
	}
	
	public void close() {
		closed = true;
		try{
			this.fdi.disconnect();
			System.out.println("closing fdi");
		}catch(Exception e){
			System.out.println("Can not close FDI connection! : " + e);
		}
		try{
			laser_socket.close();
		}catch(Exception b){
			System.out.println("Can not close laser socket connection! : " + b);
		}
		try{
			odometry_socket.close();
		}catch(Exception c){
			System.out.println("Can not close odometry socket connection! : " + c);
		}
		System.out.println("KMP sensor closed!");

		
	}
	
	// Dette gir feilmelding hvis det ikke eksisterer et objekt
	public boolean isLaserSocketConnected() {
		return this.laser_socket.isConnected();
	}
	
	public boolean isOdometrySocketConnected() {
		return this.odometry_socket.isConnected();
	}
	
	public boolean isFDIConnected() {
		return this.listener.fdi_isConnected;
	}
	
	
	public boolean isRequested() {
		return (this.KMP_laser_requested || this.KMP_odometry_requested); 
	}
	
}