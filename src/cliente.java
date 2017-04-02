
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class cliente {

	/*los parametros de ejecución son 
	/* modo de conexion -(udp|tcp)
	 * ip_servidor
	 * puerto
	 */
	public static void main(String[] args) {

		if(args.length < 3)
		{
			System.out.println("Faltan parámetros para ejecutar el cliente");
			return;
		}	

		String modoConexion = args[0];
		String ipServidor = args[1];
		String puerto = args[2];

		if(modoConexion.contains("-tcp"))
		{
			TcpMode tcp = new TcpMode();
			tcp.iniciar(ipServidor, puerto);
		}else
		{
			UdpMode udp = new UdpMode();
			udp.iniciar(ipServidor, puerto);
		}





	}
}
//Clase para conectarse de forma persistnete
class TcpMode
{
	// declaramos un objeto socket para realizar la comunicación
	Socket socket = null;

	//nos dice si el cliente esta conectado
	private boolean clientConnected = true;			

	public void iniciar(String ipServidor, String puertoServidor)
	{
		System.out.println("cliente tcp iniciado");

		//cuando se apaga el cliente se manda un mensaje al servidor para que no envie mas
		//mensajes a este cliente
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() { 

				try {
					System.out.println("Cerrando socket del cliente");
					clientConnected = false;
					//buffer donde escribimos el texto para el servidor
					PrintStream out = new PrintStream(socket.getOutputStream()) ;

					out.write(("Disconnect\r\n").getBytes());

					socket.close();
				} catch (IOException e) {
					System.out.println("Error de entrada y salida");	
				}	
			}

		});

		int portInt = Integer.parseInt(puertoServidor); 

		try {
			connect(ipServidor,portInt);
		} catch (UnknownHostException e) {
			System.out.println("No se encontro el servidor");
		} catch (IOException e) {
			System.out.println("Error de entrada y salida");
		}
	}				


	private void connect(String ipServidor,int portInt) throws UnknownHostException, IOException
	{
		socket = new Socket(ipServidor,portInt);

		// Canal de entrada por donde se recibiran los datos
		BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String s;

		while(clientConnected){
			if((s = input.readLine()) != null)
			{
				System.out.println(s);
			}
		}

	}

}

class UdpMode
{
	// declaramos un objeto socket para enviar un paquete
	DatagramSocket socket = null;
	boolean clientConnected;	

	public void iniciar(String ipServidor, String puertoServidor)
	{

		System.out.println("cliente udp iniciado");

		int portInt = Integer.parseInt(puertoServidor); 
		InetAddress address;

		try {
			//se construye un datagram socket en la maquina local
			//en un puerto disponible
			//para enviar y recibir los mensajes
			socket = new DatagramSocket();
			byte[] bufferServer = new byte[256];

			//do {
			String mensaje = "getProverbio";
			byte[] mensaje_bytes = mensaje.getBytes();
			address=InetAddress.getByName(ipServidor);
			DatagramPacket pktToSend = new DatagramPacket(mensaje_bytes,mensaje.length(),address,portInt);
			socket.send(pktToSend);

			bufferServer = new byte[256];

			//Esperamos a recibir un paquete
			DatagramPacket pktReceived = new DatagramPacket(bufferServer,256);
			socket.receive(pktReceived);

			//Convertimos el mensaje recibido en un string
			String serverMessage = new String(bufferServer).trim();

			//Imprimimos el paquete recibido
			System.out.println(serverMessage);
			//	 } while (clientConnected);

			//cerramos la conexion una vez q se imprime el mensaje
			socket.close();


		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}
}

