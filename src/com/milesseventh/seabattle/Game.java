package com.milesseventh.seabattle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
	//public static Object hachiko = new Object();
	
	public Paint pain      = new Paint(), 
	             titlePain = new Paint();
	public float touchX = -1, touchY;
	public boolean justTouched = false,
	               justReleased = false,
	               backPressed = false;
	public static final float MARGIN = 7;
	public boolean restart = false;
	
	public State gameState = State.ARRANGING;
	public Vector carriage, rotationPlacer;
	public Field enemyF, myF;
	public Button[] pad;
	public int[] configuration = {5, 4, 3, 3, 2};//;{4, 3, 3, 2, 2, 2, 1, 1, 1, 1}
	public int placerOffset = 0;
	
	public Communicator communism;
	
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
		
		communism = new Communicator(this){
			@Override
			public void hitReceived(int x, int y) {
				Field.HitResponce hr = myF.hitMe(x, y);
				if (hr.equals(Field.HitResponce.MISS))
					gameState = State.PLAYING;
				save();
				if (!myF.anyShipsLeft()){
					gameState = State.CELEBRATING;
					resetSave();
					shout("You lost!");
				}
			}

			@Override
			public void handshakeReceived(Field f, int[] config) {
				enemyF.cells = f.cells;
				enemyF.shipID = f.shipID;
				shout("Handshake received");
				if (!compareConfigs(config, configuration))
					shout("Warning: Using different configurations");
			}
		};
		
		load();
		//if (load())
			//communism.connect(communism.opponent.getRemoteAddressTCP().getAddress().getAddress());//TODO: Load opponent address from save
	}
	
	public void update(Canvas canvas){
		canvas.drawText(gameState.name(), 10, 10, pain);
		canvas.translate(0, canvas.getHeight());
		canvas.scale(1, -1);
		canvas.translate(0, (canvas.getHeight() - myF.rectSize.y) / 2f);
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
					communism.startListening();
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
			Field.HitResponce hr = enemyF.hitMe(Math.round(carriage.x), Math.round(carriage.y));
			switch(hr){
			case MISS:
				gameState = State.WAITING;
			case HIT:
				communism.send(Math.round(carriage.x), Math.round(carriage.y));
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
		case CELEBRATING:
			restart = true;
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
	
	private static final boolean KEEP_STATE = true;
	
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
			sb.f = myF;
			break;
		case PLAYING:
		case WAITING:
			sb.f = myF;
			//sb.opponent = opponent;
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

	public boolean resumed = false;
	public boolean loadedGameStateIsActive = false;
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
		
		switch (sb.gameState){
		case ARRANGING:
			configuration = sb.config;
			placerOffset = sb.placerOffset;
			gameState = sb.gameState;
		case CONNECTING:
			myF = sb.f;
			gameState = sb.gameState;
			communism.startListening();
			break;
		case PLAYING:
			myF = sb.f;
			gameState = State.CONNECTING;
			loadedGameStateIsActive = true;
			communism.startListening();
			break;
		case WAITING:
			myF = sb.f;
			gameState = State.CONNECTING;
			loadedGameStateIsActive = false;
			communism.startListening();
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
