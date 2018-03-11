package com.milesseventh.seabattle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.widget.Toast;

public class Game {
	enum State {
		ARRANGING, CONNECTING, PLAYING, WAITING, CELEBRATING
	}
	public static final int SEVENTH_COLOR = Color.rgb(218, 64, 0);
	public static Object hachiko = new Object();
	
	public Paint pain      = new Paint(), 
	             titlePain = new Paint();
	public float touchX = -1, touchY;
	public boolean justTouched = false,
	               justReleased = false,
	               backPressed = false;
	public static final float MARGIN = 7;
	public boolean restart = false;
	
	public State gameState = State.ARRANGING;
	public boolean resumed = false;
	public Vector carriage, rotationPlacer;
	public Field enemyF, myF;
	public Button[] pad;
	public int[] configuration = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};//{5, 4, 3, 3, 2};
	public int placerOffset = 0;
	
	public static final int PORT = 7711;
	public static byte[] opponent = {0, 0, 0, 0};
	public static Client client;
	public static Server server;
	public static boolean initiator = false;
	
	public Game(int w, int h){
		pain.setColor(Color.BLACK);
		pain.setTextAlign(Align.LEFT);
		pain.setTextSize(20);

		carriage = new Vector(5, 5);
		rotationPlacer = new Vector(0, 1);
		enemyF = new Field(Vector.getVector(MARGIN, MARGIN), 
		                   Vector.getVector(10, 10), Vector.getVector(w / 3f - MARGIN * 2, w / 3f - MARGIN * 2), false);
		myF    = new Field(Vector.getVector(w / 3f, MARGIN), 
		                   Vector.getVector(10, 10), Vector.getVector(w / 3f - MARGIN * 2, w / 3f - MARGIN * 2), true);
		
		float s = (w / 3f - MARGIN * 2f) / 3f;
		Vector p = new Vector(w / 3f * 2f + MARGIN + s, MARGIN + s);
		Vector sv = new Vector(s, s);
		pad = new Button[] {
				new Button(p.added(0, s), sv){
					@Override
					public void action() {
						//up
						move(Vector.getVector(0, 1));
					}
				},
				new Button(p.added(0, -s), sv){
					@Override
					public void action() {
						//down
						move(Vector.getVector(0, -1));
					}
				},
				new Button(p.added(-s, 0), sv){
					@Override
					public void action() {
						//left
						move(Vector.getVector(-1, 0));
					}
				},
				new Button(p.added(s, 0), sv){
					@Override
					public void action() {
						//right
						move(Vector.getVector(1, 0));
					}
				},
				new Button(p, sv){
					@Override
					public void action() {
						//fire
						fire();
					}
				},
				new Button(p.added(s, s), sv){
					@Override
					public void action() {
						//cw
						if (gameState == State.ARRANGING)
							rotationPlacer.rotateNormal90CW();
					}
				}
		};
		
		if (load()){
			try {
				client.connect(15000, InetAddress.getByAddress(opponent), PORT);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void initConnection(){
		if (server != null){
			server.close();
			server.stop();
		}
		if (client != null){
			client.close();
			client.stop();
		}
		server = new Server();
		server.start(); 
		try{
			server.bind(PORT);
		} catch (Exception e){}
		server.getKryo().register(Message.class);
		server.getKryo().register(Message.Command.class);
		server.getKryo().register(Field.class);
		server.getKryo().register(Field.CellState.class);
		server.getKryo().register(Field.CellState[].class);
		server.getKryo().register(Vector.class);
		server.getKryo().register(java.util.Locale.class);
		server.getKryo().register(int[].class);
		
		client = new Client();
		client.start();
		client.getKryo().register(Message.class);
		client.getKryo().register(Message.Command.class);
		client.getKryo().register(Field.class);
		client.getKryo().register(Field.CellState.class);
		client.getKryo().register(Field.CellState[].class);
		client.getKryo().register(Vector.class);
		client.getKryo().register(java.util.Locale.class);
		client.getKryo().register(int[].class);
		
		server.addListener(new Listener(){
			@Override
			public void connected(Connection c){
				if (resumed)
					load();
				if (!initiator){
					opponent = c.getRemoteAddressTCP().getAddress().getAddress();
					try {
						client.connect(15000, InetAddress.getByAddress(opponent), PORT);
					} catch (Exception e) {
						c.close();
						
					}
				}
			}
			
			@Override
			public void received(Connection c, Object data){
				if (!(data instanceof Message))
					return;
				try {
					Message m = (Message) data;
					switch (m.command){
					case HANDSHAKE:
						enemyF.cells = m.f.cells;
						enemyF.shipID = m.f.shipID;
						if (!compareConfigs(m.config, configuration))
							shout("Warning: Using different configurations");
						if (!initiator)
							gameState = State.PLAYING;
						break;
					case HIT:
						Field.HitResponce hr = myF.hitMe(m.x, m.y);
						if (hr.equals(Field.HitResponce.MISS))
							gameState = State.PLAYING;
						save();
						if (!myF.anyShipsLeft()){
							gameState = State.CELEBRATING;
							resetSave();
							shout("You lost!");
						}
						break;
					}
				} catch(Exception e) {}
			}
			
			@Override
			public void disconnected(Connection c){
				client.close();
			}
		});
		client.addListener(new Listener(){
			@Override
			public void connected(Connection c){
				shout("Established connection with " + c.getRemoteAddressTCP().getAddress().getHostAddress());
				opponent = c.getRemoteAddressTCP().getAddress().getAddress();
				if (!resumed)
					gameState = State.WAITING;
				Message m = new Message();
				m.command = Message.Command.HANDSHAKE;
				m.f = myF;
				m.config = configuration;
				client.sendTCP(m);
			}
			@Override
			public void disconnected(Connection c){
				shout("Connection lost");
				save();
				gameState = State.CONNECTING;
			}
		});
	}
	
	public void update(Canvas canvas){
		canvas.translate(0, canvas.getHeight());
		canvas.scale(1, -1);
		canvas.translate(0, (canvas.getHeight() - myF.rectSize.y) / 2f);
		//canvas.drawText(isConnected.name(), canvas.getWidth() / 2f, canvas.getHeight() / 2f, pain);
		enemyF.render(canvas, gameState);
		myF.render(canvas, gameState);
		
		switch (gameState){
		case ARRANGING:
			myF.drawTempShip(carriage, rotationPlacer, configuration[placerOffset], canvas);
			break;
		case PLAYING:
			enemyF.drawCarriage(Math.round(carriage.x), Math.round(carriage.y), 
			                    SEVENTH_COLOR, canvas);
			break;
		default:
			break;
		}
		
		for (Button b : pad)
			b.render(Vector.getVector(touchX, touchY - (canvas.getHeight() - myF.rectSize.y) / 2f), justTouched, canvas);
		
		if (backPressed){
			MenuFragment mf = new MenuFragment(this);
			mf.show(GameActivity.me.getFragmentManager(), "...");
		}
		if (justReleased)
			touchX = -1;
		justReleased = false;
		justTouched = false;
		backPressed = false;
	}
	
	public void move(Vector displacement){
		carriage.add(displacement);
		carriage.x = clamp(carriage.x, 0, myF.fieldSize.x - 1);
		carriage.y = clamp(carriage.y, 0, myF.fieldSize.y - 1);
	}
	
	public void fire(){
		switch (gameState){
		case ARRANGING:
			if (myF.place(carriage, rotationPlacer, configuration[placerOffset], placerOffset)){
				++placerOffset;
				if (placerOffset >= configuration.length){
					initConnection();
					gameState = State.CONNECTING;
					shout("Press central button to connect");
				}
				save();
			}
			break;
		case CONNECTING:
			ConnectionSetupFragment csf = new ConnectionSetupFragment();
			csf.show(GameActivity.me.getFragmentManager(), "...");
			break;
		case PLAYING:
			//myF.hitMe(Math.round(carriage.x), Math.round(carriage.y));
			Field.HitResponce hr = enemyF.hitMe(Math.round(carriage.x), Math.round(carriage.y));
			switch(hr){
			case MISS:
				gameState = State.WAITING;
			case HIT:
				Message m = new Message();
				m.command = Message.Command.HIT;
				m.x = Math.round(carriage.x);
				m.y = Math.round(carriage.y);
				client.sendTCP(m);
				save();
				if (!enemyF.anyShipsLeft()){
					gameState = State.CELEBRATING;
					resetSave();
					shout("You won!");
				}
				break;
			case IGNORED:
				break;
			}
			break;
		default:
			break;
		}
	}
	public float clamp(float x, float min, float max){
		return Math.min(Math.max(min, x), max);
	}
	
	public boolean compareConfigs(int[] a, int[] b){
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i)
			if (a[i] != b[i])
				return false;
		return true;
	}
	
	public static void shout(final String mess){
		GameActivity.me.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(GameActivity.me, mess, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private static final boolean KEEP_STATE = false;
	
	public void save(){
		if (!KEEP_STATE)
			return;
		
		StateBox sb = new StateBox();
		sb.gameState = gameState;
		switch(gameState){
		case ARRANGING:
			sb.config = configuration;
			sb.placerOffset = placerOffset;
		case CONNECTING:
			initConnection();
			sb.f = myF;
			break;
		case PLAYING:
		case WAITING:
			initConnection();
			sb.f = myF;
			sb.opponent = opponent;
			break;
		default:
			break;
		}
		
		try {
			FileOutputStream fos = GameActivity.me.openFileOutput("SSB.state", Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(sb);
			oos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean load(){
		if (!KEEP_STATE)
			return false;
		
		StateBox sb;
		try{
			FileInputStream fis = GameActivity.me.openFileInput("SSB.state");
			ObjectInputStream ois = new ObjectInputStream(fis);
			sb = (StateBox)ois.readObject();
			ois.close();
			fis.close();
			if (sb == null)
				return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		gameState = sb.gameState;
		switch (gameState){
		case ARRANGING:
			configuration = sb.config;
			placerOffset = sb.placerOffset;
		case CONNECTING:
			myF = sb.f;
			break;
		case PLAYING:
		case WAITING:
			myF = sb.f;
			opponent = sb.opponent;
			break;
		default:
			break;
		}
		
		resumed = true;
		return true;
	}
	
	public void resetSave(){
		GameActivity.me.deleteFile("SSB.state");
	}
}
