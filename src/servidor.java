

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class servidor {


	//para manejar los datos del fichero
	public static List<String> lines = null;
	public static Random random = null;

	/*los parametros de ejecución son 
	 * fichero-de-mensajes
	 * puerto
	 */
	public static void main(String[] args) {

		if(args.length < 2)
		{
			System.out.println("Faltan parámetros para ejecutar el servidor");
			return;
		}

		String puerto = args[0];
		String ficheroMensajesAddress = args[1];

		try {
			//creo un stream de string con las lineas del fichero
			lines = Files.lines(Paths.get(ficheroMensajesAddress)).collect(Collectors.toList());
			random = new Random();


			//inicio el servidor udo en un hilo aparte
			UdpServer udpServer =  new UdpServer();
			udpServer.setTcpPort(puerto);
			udpServer.start();


			//inicio el servidor tcp en el hilo principal
			//en su funcionamiento interno el crea un hilo por cada
			//peticion tcp
			TcpServer tcpServer= new TcpServer();
			tcpServer.iniciar(puerto);



		} catch (IOException e) {
			System.out.println("No se encontró el fichero de datos");
		}

	}

	//devuelve una linea aleatoria del stream del fichero
	public static String getLine()
	{
		//hay q ponerle el retorno de linea para q el cliente sepa cuando es que
		//acaba una linea y pudea imprimirla en la consola
		return lines.get(random.nextInt(lines.size())) + "\r\n";

	}


}
//UDP Server
class UdpServer extends Thread
{
	DatagramSocket socket = null;
	String tcpPort;

	public void setTcpPort(String tcpPort)
	{
		this.tcpPort = tcpPort;
	}

	public void run()
	{
		int portInt = Integer.parseInt(tcpPort);

		try{
			//Abrimos el ServerSocket y le asignamos el puerto por donde va a escuchar
			socket = new DatagramSocket(portInt);
			System.out.println("Servidor UDP iniciado por el puerto " + tcpPort);

			//Iniciamos el bucle
			do {
				// Recibimos el paquete
				byte[] mensaje_bytes = new byte[256];
				DatagramPacket paquete = new DatagramPacket(mensaje_bytes,256); 
				socket.receive(paquete);
				// Lo convertimos a String
				//String mensaje = new String(mensaje_bytes).trim();

				//Obtenemos IP Y PUERTO
				int puerto = paquete.getPort();
				InetAddress  address = paquete.getAddress();
				String proverbio =  servidor.getLine();
				byte[] proverbioBytes = proverbio.getBytes();
				DatagramPacket envpaquete = new DatagramPacket(proverbioBytes,proverbio.length(),address,puerto);
				// realizamos el envio
				socket.send(envpaquete);

			} while (true);


		}catch(IOException e)
		{
			e.printStackTrace();
		}

	}

}
class TcpServer
{
	ServerSocket socket = null;

	public void iniciar(String tcpPort) throws IOException
	{
		int portInt = Integer.parseInt(tcpPort); 

		try{
			//Abrimos el ServerSocket y le asignamos el puerto por donde va a escuchar
			socket = new ServerSocket(portInt);
			System.out.println("Servidor Tcp iniciado por el puerto " + tcpPort);
		}catch(IOException e)
		{
			e.printStackTrace();
		}

		while(true)//Siempre se estan esperando conexiones
		{

			if(socket !=null){	
				//se queda a la espera de llamadas
				Socket entrante = socket.accept();

				//Por cada cliente se crea una hilo para responderle 
				ClientResponse rc = new ClientResponse(entrante);
				rc.start();
			}else
				break;
		}

	}


}

class ClientResponse extends Thread
{

	private Socket scliente;

	//buffer donde escribimos el texto para el cliente
	private PrintStream   out;

	// nos dice si el cliente sigue o no conectado
	private boolean notDisconnected = true;

	//constructor de la clase
	ClientResponse(Socket ps)
	{
		scliente = ps;
		setPriority(NORM_PRIORITY - 1); // hacemos que la prioridad del hilo sea baja para no afectar el rendimiento
	}


	public void run() // Se implementa el metodo run de Thread
	{

		//en un subproceso se lee lo que envia el cliente y si es la sennal de desconexion se cambia el valor de la variable
		//notDisconnected
		Thread t= new Thread()
		{
			public void run() {

				// Se consigue el canal de entrada para detectar cuando el cliente manda el mensaje de desconexion
				BufferedReader input = null;
				try {
					input = new BufferedReader(new InputStreamReader(scliente.getInputStream()));

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					String s = input.readLine();
					if(s.equalsIgnoreCase("Disconnect")) // si el texto que envio el cliente
					{									  //es Disconnect entonces es que se desconecto 							
						notDisconnected = false;
					}
				} catch (IOException e) {
					//si no pudo leer lo que envia el cliente entonces se desconecta igual
					notDisconnected = false;
				} 



			}
		};
		t.start(); //se inicia el thread que lee los mensajes del cliente

		try {
			//buffer donde escribimos el texto para el cliente
			out = new PrintStream(scliente.getOutputStream()) ;

			do{
				//se escribe un proverbio
				byte[] proverbio =  servidor.getLine().getBytes();
				out.write(proverbio);
				out.flush();


				try {
					//se espera un segundo entre mensaje y mensaje
					sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}while(notDisconnected );

			out.close();


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
}