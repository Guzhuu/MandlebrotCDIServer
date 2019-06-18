import java.net.*;
import java.util.Arrays;
import java.util.Vector;
import java.io.*;
import java.util.*;
import java.awt.image.*;
import java.io.File;
import javax.imageio.*;

import java.awt.geom.Point2D;
import java.awt.Point;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/*
	java Serverfilter modo nºiteraciones nombreImagen resolucionX resolucionY (izquierda, abajo) (derecha, arriba) trozosX trozosY
*/

public class Serverfilter{
	public static int puerto = 4444;
	public static int iteraciones = 32;
	public static char modo = 'g'; ////g 8bitgray, h 16bitgray, c 24bit color
	public static Object esperando = new Object();
    //public static final CountDownLatch senal = new CountDownLatch(1); No funciona
	 
	public static void main (String args[]) {
		int resolucionX = 1920;
		int resolucionY = 1080;
		int trozosX = 9;
		int trozosY = 7;
		Point2D regionAbjIzq = new Point2D.Double();
		Point2D regionArrDer = new Point2D.Double();
		String nombreImagen = "out";
		//Nombre, iteraciones, resolucionx, resoluciony, coordenadas AbjIzq, coordenadas ArrDer
		
		try {
			modo = args[0].charAt(0);
		}catch(Exception e) {
			System.out.println("No se ha modo (g-gris, h-mas gris, c-color, al menos, rojo), se pondrá modo 0");
			modo = 'g';
		}
		try {
			iteraciones = Integer.parseInt(args[1]);
		}catch(Exception e) {
			System.out.println("No se ha especificado numero de iteraciones, se pondrán 32");
			iteraciones = 32;
		}
		try {
			nombreImagen = args[2];
		}catch(Exception e) {
			System.out.println("No se ha especificado nombre de imagen, se llamará \"out.png\"");
			nombreImagen = "out";
		}
		try {
			resolucionX = Integer.parseInt(args[3]);
		}catch(Exception e) {
			System.out.println("No se ha especificado resolución eje X, se pondrá a 1024");
			resolucionX = 1920;
		}
		try {
			resolucionY = Integer.parseInt(args[4]);
		}catch(Exception e) {
			System.out.println("No se ha especificado resolución eje Y, se pondrá a 512");
			resolucionY = 1080;
		}
		try {
			regionAbjIzq.setLocation(Double.parseDouble(args[5]), Double.parseDouble(args[6]));
		}catch(Exception e) {
			System.out.println("No se ha especificado limite izquierdo inferior, será (-2.5, -1.0)");
			regionAbjIzq.setLocation(-2.5, -1.0);
		}
		try {
			regionArrDer.setLocation(Double.parseDouble(args[7]), Double.parseDouble(args[8]));
		}catch(Exception e) {
			System.out.println("No se ha especificado limite derecho superior, será (1.0, 1.0)");
			regionArrDer.setLocation(1.0, 1.0);
		}
		try {
			trozosX = Integer.parseInt(args[9]);
			trozosY = Integer.parseInt(args[10]);
		}catch(Exception e) {
			//System.out.println("No se ha especificado limite derecho superior, será (1.0, 1.0)");
			trozosX = 9;
			trozosY = 7;
		}
		//Poner como un número múltiplo de la resolución para que funcione bien
		while(resolucionX % trozosX != 0 && trozosX > 2){
			trozosX--;
		}
		while(resolucionY % trozosY != 0 && trozosY > 2){
			trozosY--;
		}
		if(trozosX <= 1){
			trozosX = 2;
		}
		if(trozosY <= 1){
			trozosY = 2;
		}
		System.out.println("Trozos eje X: " + trozosX);
		System.out.println("Trozos eje Y: " + trozosY);
		Imagen imagen = new Imagen(resolucionX, resolucionY, trozosX, trozosY, regionAbjIzq, regionArrDer, nombreImagen, modo);
		Thread servidor = new Thread(new Server(imagen, puerto));
		servidor.start();
		//Lo ideal sería el CountDownLatch, pero no consigo usarlo
		synchronized(esperando){
			try{esperando.wait();}catch(Exception e){System.out.println("Error: " + e.toString());}
		}
		servidor.interrupt();
		System.out.println("---------- Dibujando imagen final -------------");
		imagen.set();		
		System.out.println("---------- Dibujada imagen final -------------");
		System.exit(0);
	}
}

class Gestor extends Thread{
	ServerSocket server;
	Socket socket;
	ObjectInputStream ois = null;
	ObjectOutputStream oos = null;
	static Imagen imagen = null;
	
	public Gestor(ServerSocket server, Socket socket, ObjectInputStream ois, ObjectOutputStream oos, Imagen imagen){
		this.server = server;
		this.socket = socket;
		this.ois = ois;
		this.oos = oos;
		if(this.imagen == null){
			this.imagen = imagen;
		}
	}
	
	public void run(){
		try{
			//socket = server.accept();
			Point coordenadasRegion = this.imagen.getRegionUnused();
			RegionYMsg regionAEnviar = this.imagen.getRegion(coordenadasRegion.x, coordenadasRegion.y);
			
			regionAEnviar.setMsg("Region a filtrar");
			
			if(coordenadasRegion.x == -1 || regionAEnviar.getMsg().equals("Error")) {
				//System.out.println("\n!!!!!!!!!!!\n[GESTOR]: Error al coger la región " + coordenadasRegion.x + "," + coordenadasRegion.y + "\n!!!!!!!!!!!!");
				oos.writeObject(new RegionYMsg("Error"));
				if(imagen.todasRegionesUsadas()){
					System.out.println("[GESTOR]: Todas las regiones han sido usadas");
					Server.flagFin = true;
					oos.writeObject(new RegionYMsg("Error"));	
				}
			}else{
				System.out.println("[GESTOR]: Enviando al cliente la región ("+coordenadasRegion.x+","+coordenadasRegion.y+")");
				oos.writeObject(regionAEnviar);	
				System.out.println("[GESTOR]: Enviada al cliente la región ("+coordenadasRegion.x+","+coordenadasRegion.y+")");
				
				ois = new ObjectInputStream(socket.getInputStream());
				RegionYMsg message = (RegionYMsg) ois.readObject();
				if(message.getMsg().equals("Region filtrada")){
					System.out.println("[GESTOR]: He recibido una nueva región, encargo escribirla");
					
					this.imagen.saveRegion(coordenadasRegion.x, coordenadasRegion.y, message.getRegion());
				}else{
					System.out.println("[GESTOR]: Mensaje no recogido: " + message.getMsg());
					oos.writeObject(new RegionYMsg("Mensaje no recogido"));
				}
			}
			
		}catch(ArrayIndexOutOfBoundsException x) {
			System.out.println("En el gestor: " + x.toString());
		}catch(Exception e){
			//System.out.println("En el gestor: " + e.toString());
		}finally{
			try{
				ois.close();
				oos.close();
				socket.close();
			}catch(Exception es){
				//System.out.println("En el gestor, excepcion de finally: " + es.toString());
			}
		}
	}
}

class Server extends Thread {
	Socket socket = null ;
	ObjectInputStream ois = null;
	ObjectOutputStream oos = null;
	Imagen imagen;
	Integer puerto;
	static boolean flagFin = false;
	
	public Server(Imagen imagen, Integer puerto) {
		this.imagen = imagen;
		this.puerto = puerto;
		this.flagFin = false;
	}
	
	public void run() {
		try {
			ServerSocket server = new ServerSocket(this.puerto);
			while(!flagFin) {
				socket = server.accept();
				System.out.println("[SERVIDOR]: Esperando input");
				ois = new ObjectInputStream(socket.getInputStream());
				RegionYMsg message = (RegionYMsg) ois.readObject();
				System.out.println("\n[SERVIDOR RECIBE]: " + message.getMsg());
				oos = new ObjectOutputStream(socket.getOutputStream());
				if(message.getMsg().equals("Oiga usté, deme una nueva región")){
					new Gestor(server, socket, ois, oos, this.imagen).start();
				/*}else if(message.getMsg().equals("Region filtrada")) {
					System.out.println("[SERVIDOR]: He recibido una nueva región, encargo escribirla");
					
					new Pintor(message.getX(), message.getY(), message.getRegion(), this.imagen).start();
				*/}else{
					System.out.println("\n[SERVIDOR RECIBE]: Ignoro aquello para lo que no estoy preparado: " + message.getMsg());
					oos.writeObject("Ignoro aquello para lo que no estoy preparado");
				}
				System.gc();
			}
			System.out.println("[SERVIDOR]: Esperando a que finalicen todos los Gestores para dibujar la imagen final...");
			while(!this.imagen.todasRegionesUsadas()){ //Server y serverFilter
				try{sleep(1000);}catch(Exception e){}
			}
			server.close();
		}catch (Exception e) {
			System.out.println("En el servidor: " + e.toString());
		}finally{
			synchronized(Serverfilter.esperando){
				Serverfilter.esperando.notify();
			}
		}
	}
}


class Imagen {
	static BufferedImage img;
	static int tamanoX;
	static int tamanoY;
	static Point2D AbjIzq; //Doble negativo
	static Point2D ArrDer; //Doble positivo
	static boolean[][] regionesUsadas;
	static boolean[][] regionesGuardadas;
	static ReentrantLock regionesUsadasW = new ReentrantLock();
	static ReentrantLock regionesUsadasR = new ReentrantLock();
	static ReentrantLock regionesGuardadasL = new ReentrantLock();
	static Raster raster;
	static String nombreImg;
	static char modo;
	
	public Imagen(int rX, int rY, int tX, int tY, Point2D coordenadasAbjIzq, Point2D coordenadasArrDer, String nombreImagen, char modo) {
		try {
			if(modo == 'g'){
				img = new BufferedImage(rX, rY, BufferedImage.TYPE_BYTE_GRAY);
			}else if(modo == 'h'){
				img = new BufferedImage(rX, rY, BufferedImage.TYPE_USHORT_GRAY);
			}else{
				img = new BufferedImage(rX, rY, BufferedImage.TYPE_3BYTE_BGR);
			}
			this.raster = img.getData();
			nombreImg = nombreImagen;
			tamanoX = img.getWidth()/tX;
			tamanoY = img.getHeight()/tY;
			regionesUsadas = new boolean[tX][tY];
			regionesGuardadas = new boolean[tX][tY];
			AbjIzq = coordenadasAbjIzq;
			ArrDer = coordenadasArrDer;
			//inicializar variables regionymsg
			new RegionYMsg(rX, rY, AbjIzq, ArrDer, modo);
			this.modo = modo;
			Arrays.fill(regionesUsadas[0], false);
			Arrays.fill(regionesUsadas[1], false);
			Arrays.fill(regionesGuardadas[0], false);
			Arrays.fill(regionesGuardadas[1], false);
			Arrays.fill(regionesGuardadas, false);
			Arrays.fill(regionesUsadas, false);
		}catch(ArrayStoreException ase){
			//Error común, no error en realidad
		}catch(Exception e) {
			System.out.println(e.toString());
		}
	}
	
	public RegionYMsg getRegion(int x, int y) {
		try{
			regionesUsadasW.lock();
			if(x >= 0 && y >= 0 && !regionesUsadas[x][y]) {
				regionesUsadas[x][y] = true;
				regionesUsadasW.unlock();
				int[][] retorno = new int[tamanoX][tamanoY];
				
				for(int i = 0; i < tamanoX; i++) {
					for(int j = 0; j < tamanoY; j++) {
						try{
							retorno[i][j] = raster.getSample((x*tamanoX)+i, (y*tamanoY)+j, 0);
						}catch(Exception e){
							//System.out.println("Error al coger region: " + e.toString());
						}
					}
				}				
				return new RegionYMsg(x, y, retorno, Serverfilter.iteraciones, "Region lista");
			}
			regionesUsadasW.unlock();
		}catch(Exception e){
			System.out.println("En el getRegion: " + e.toString());
			return new RegionYMsg("Error");
		}
		return new RegionYMsg("Error");
	}
	
	public Point getRegionUnused(){
		regionesUsadasR.lock();
		try{
			for(int i = 0; i < regionesUsadas.length; i++){
				for(int j = 0; j < regionesUsadas[0].length; j++){
					if(!regionesUsadas[i][j]){
						return new Point(i,j);
					}
				}
			}
			this.todasRegionesUsadas();
		}catch(Exception e){
			System.out.println("Error al coger region sin usar: " + e.toString());
			return new Point(-1,-1);
		}finally{
			regionesUsadasR.unlock();
		}
		return new Point(-1,-1);
	}
	
	public void saveRegion(int x, int y, int[][] valores){
		try{
			regionesGuardadasL.lock();
			if(!regionesGuardadas[x][y]){
				if(this.modo != 'c'){
					synchronized(img){
						WritableRaster rasterW = img.getRaster();
						for(int i=0; i<valores.length; i++) {
							for(int j=0; j<valores[0].length; j++) {
								rasterW.setSample((x*tamanoX)+i,(y*tamanoY)+j,0,valores[i][j]);
							}
						}
						img.setData(rasterW);
					}
				}else{
					synchronized(img){
						for(int i=0; i<valores.length; i++) {
							for(int j=0; j<valores[0].length; j++) {
								img.setRGB((x*tamanoX)+i, (y*tamanoY)+j,(~valores[i][j])<<8); /*ATENCIOOOOOOON, JUGAR CON LOS BITS DE ESTE VALOR DA COLORES DISTINTOS, AQUi ESTA LA PALETA, DIGAMOS*/
							}
						}
					}
				}
				regionesGuardadas[x][y] = true;
				regionesGuardadasL.unlock();
			}else{
				regionesGuardadasL.unlock();
			}
			regionesUsadasW.lock();
			if(!regionesUsadas[x][y]){ regionesUsadas[x][y] = true;}
			regionesUsadasW.unlock();
		}catch(Exception e){
			System.out.println("Error al guardar region: " + e.toString());
		}finally{
			System.out.println("saveRegion() concluido ("+x+","+y+")");
		}
	}
	
	public void set(){
		//Guarda imagen
		try{
			synchronized(img){
				File file_out=new File(this.nombreImg + ".png");
				ImageIO.write(img,"png",file_out);
			}
		}catch(Exception e){
			System.out.println("Error al escribir la imagen: " + e.toString());
		}finally{
			System.out.println("set() concluido");
		}
	}
	
	public boolean todasRegionesUsadas(){
		try{
			regionesGuardadasL.lock();
			boolean retorno = true;
			for(int x = 0; x < regionesGuardadas.length; x++){
				for(int y = 0; y < regionesGuardadas[0].length; y++){
					if(!regionesGuardadas[x][y]){
						retorno = false;
						regionesUsadasW.lock();
						regionesUsadas[x][y] = false;
						regionesUsadasW.unlock();
					}
				}
			}
			regionesGuardadasL.unlock();
			return retorno;
		}catch(Exception e){
			System.out.println("En el todasRegionesUsadas: " + e.toString());
			return false;
		}
	}
}


/*
	h) Tarda mucho, sobretodo cambiando el valor de las iteraciones, lo cual puede hacer también que tarde poco o nada.
	
	Recomendadas 300 iteraciones para tamaño normal y que quede bonito (modo true)
	Recomendadas 100 iteraciones para tamaño normal y que quede definido (modo false, no hay manera de que quede bonito desde lejos con 16bit de color)
*/

