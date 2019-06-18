/*Clase de comunicación entre cliente y servidor*/

import java.io.*;
import java.awt.geom.Point2D;
import java.awt.Point;

class RegionYMsg implements Serializable{
	int x; int y; 			//Region de la imagen entera
	private static int PpixelsX; 
	int pixelsX; 
	private static int PpixelsY; //Pixels de la imagen
	int pixelsY; //Pixels de la imagen
	private static double PxDer; 
	double xDer; 
	private static double PxIzq; //Coordenadas x izq y x der de la imagen
	double xIzq; //Coordenadas x izq y x der de la imagen
	private static double PyAbj; 
	double yAbj; 
	private static double PyArr;	//Coordenadas y arr e y abaj de la imagen
	double yArr;	//Coordenadas y arr e y abaj de la imagen
	int[][] region;			//Region
	String msg;				//Un msg
	int iteraciones;		//Numero de iteraciones de esta region (para pasarselo al cliente de alguna manera)
	private static char Pmodo;
	char modo; //True: 8bits, false: 16bits
	
	public RegionYMsg(int pixelsX, int pixelsY, Point2D AbjIzq, Point2D ArrDer, char modo){
		this.PpixelsX = pixelsX;
		this.PpixelsY = pixelsY;
		this.PxIzq = AbjIzq.getX();
		this.PxDer = ArrDer.getX();
		this.PyAbj = AbjIzq.getY();
		this.PyArr = ArrDer.getY();
		this.Pmodo = modo;
	}
	
	public RegionYMsg(int x, int y, int[][] region, int iteraciones, String msg){
		this.x = x;
		this.y = y;
		if(iteraciones <= 0){
			this.iteraciones = 100;
		}else{
			this.iteraciones = iteraciones;
		}
		this.region = region;
		this.msg = msg;
		asignarPrivados();
	}
	
	public RegionYMsg(Point coordenadas, int[][] region, int iteraciones, String msg){
		this.x = coordenadas.x;
		this.y = coordenadas.y;
		if(iteraciones <= 0){
			this.iteraciones = 100;
		}else{
			this.iteraciones = iteraciones;
		}
		this.region = region;
		this.msg = msg;
		asignarPrivados();
	}
	
	public RegionYMsg(String msg){
		this.msg = msg;
	}
	
	private void asignarPrivados(){
		this.pixelsX = PpixelsX;
		this.pixelsY = PpixelsY;
		this.xIzq = PxIzq;
		this.yArr = PyArr;
		this.xDer = PxDer;
		this.yAbj = PyAbj;
		this.modo = Pmodo;
	}
	
	public int getX(){
		return this.x;
	}
	
	public int getY(){
		return this.y;
	}
	
	public double getXIzq(){
		return this.xIzq;
	}
	
	public double getXDer(){
		return this.xDer;
	}
	
	public double getYArr(){
		return this.yArr;
	}
	
	public double getYAbaj(){
		return this.yAbj;
	}
	
	public int[][] getRegion(){
		return this.region;
	}
	
	public void setRegion(int[][] nuevaRegion){
		this.region = nuevaRegion;
	}
	
	public String getMsg() {
		return this.msg;
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public int getIteraciones(){
		return this.iteraciones;
	}
	
	public int getPX(){
		return this.pixelsX;
	}
	
	public int getPY(){
		return this.pixelsY;
	}
	
	public double getCoordenadaX(int pixel){
		return (((xDer - this.getXIzq())*pixel) / (this.getPX())) + this.getXIzq();
	}
	
	public double getCoordenadaY(int pixel){
		return (((yArr - this.getYAbaj())*pixel) / (this.getPY())) + this.getYAbaj();
	}
	
	public char getModo(){
		return this.modo;
	}
}
