import java.io.File;

/*Hace un zoom de 10^args[0] hacia un punto (args[1],args[2]), en el modo args[3]
	El numero de iteraciones no es decente, o a partir de 10^-15 el double no tiene precisión*/
	/*Un buen punto: -1.74999841099374081749002483162428393452822172335808534616943930976364725846655540417646727085571962736578151132907961927190726789896685696750162524460775546580822744596887978637416593715319388030232414667046419863755743802804780843375 -0.00000000000000165712469295418692325810961981279189026504290127375760405334498110850956047368308707050735960323397389547038231194872482690340369921750514146922400928554011996123112902000856666847088788158433995358406779259404221904755*/

public class zoom{
	public static void main(String[] args){
		//java Serverfilter %i 175 3840 2160 %i -1.0 1.0 1.0
		try{
			String comando = "java Serverfilter";

			// 2 x 2 x 2 = 8
			try{Thread.sleep(350);}catch(Exception e){}
			int rX = 3840;
			int rY = 2160;
			double haciaX = 0.0; //-0.75 medio
			double haciaY = 0.0; //0 medio
			double x1 = -2.5;
			double y1 = -1.0;
			double x2 = 1.0;
			double y2 = 1.0;
			char modo = args[3].charAt(0);
			double zoom = java.lang.Math.pow(10, Integer.parseInt(args[0]));
			int iteraciones = 300;
				iteraciones = (1675721*Integer.parseInt(args[0])) / (220) + 2000;
			if(Integer.parseInt(args[0]) == 0){
				haciaX = -0.75;
				haciaY = 0;
			}else{
				haciaX = Double.parseDouble(args[1]);
				haciaY = Double.parseDouble(args[2]);
				double aux = (java.lang.Math.abs(x1) + java.lang.Math.abs(x2)) / (zoom);
				x1 = haciaX - aux;
				x2 = haciaX + aux;
				
				
				aux = (java.lang.Math.abs(y1) + java.lang.Math.abs(y2)) / (zoom);
				y1 = haciaY - aux;
				y2 = haciaY + aux;
			}
			
			String[] command = {Character.toString(modo), Integer.toString(iteraciones), "10^"+args[0], Integer.toString(rX), Integer.toString(rY), Double.toString(x1), Double.toString(y1), Double.toString(x2), Double.toString(y2)};
			Runnable r = new Runnable() {
				public void run() {
					Serverfilter.main(command);
				}
			};
			Thread t = new Thread(r);
			t.start();
			t.join();
			t.interrupt();
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
}