package com.milesseventh.seabattle;

import java.io.Serializable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class Field implements Serializable{
	/****/
	private static final long serialVersionUID = 3254850344077927579L;

	enum CellState {
		UNKNOWN, SHOT_MISSED, SHOT_HIT, SHIP, SHIP_DESTROYED, EMPTY
	}
	
	public Vector fieldSize, rectSize, position;
	public static Paint pain = new Paint();
	public CellState[] cells;
	public int[] shipID;
	public boolean isMine;
	public Field(){}
	public Field(Vector nposition, Vector field, Vector rect, boolean nisMine) {
		position= nposition.cpy();
		fieldSize = field.cpy();
		rectSize = rect.cpy();
		cells = new CellState[Math.round(fieldSize.x * fieldSize.y)];
		shipID = new int[Math.round(fieldSize.x * fieldSize.y)];
		isMine = nisMine;
		
		for (int i = 0; i < cells.length; ++i){
			cells[i] = CellState.UNKNOWN;
			shipID[i] = -1;
		}
	}

	public void render(Canvas canvas, Game.State gameState){
		pain.setColor(Color.BLACK);
		pain.setStyle(Style.STROKE);
		canvas.drawRect(position.x, position.y + rectSize.y, position.x + rectSize.x, position.y, pain);
		/*for (int x = 1; x < fieldSize.x - 1; ++x)
			for (int y = 1; y < fieldSize.y - 1; ++y){
				canvas.drawLine(position.x + x * rectSize.x / fieldSize.x, position.y, 
				                position.x + x * rectSize.x / fieldSize.x, position.y + rectSize.y, pain);
				canvas.drawLine(position.x, y * rectSize.y / fieldSize.y, 
				   position.x + rectSize.x, y * rectSize.y / fieldSize.y, pain);
			}*/
		pain.setStyle(Style.FILL);
		for (int x = 0; x < fieldSize.x; ++x)
			for (int y = 0; y < fieldSize.y; ++y){
				switch(get(x, y)){
				case SHOT_MISSED:
					drawCell(x, y, Color.GRAY,               canvas);
					drawDot (x, y, Color.rgb(218, 64, 0),    canvas);
					break;
				case EMPTY:
					drawCell(x, y, Color.GRAY,               canvas);
					break;
				case SHIP:
					if (isMine || gameState.equals(Game.State.CELEBRATING))
						drawCell(x, y, Color.rgb(64, 64, 255), canvas);
					break;
				case SHOT_HIT:
					drawCell(x, y, Color.rgb(240, 100, 100), canvas);
					break;
				case SHIP_DESTROYED:
					drawCell(x, y, Color.rgb(170, 32, 32),   canvas);
					break;
				case UNKNOWN:
					break;
				}
			}
	}

	public void drawCell(int x, int y, int color, Canvas canvas){
		pain.setColor(color);
		pain.setStyle(Style.FILL);
		canvas.drawRect(position.x + x * rectSize.x / fieldSize.x, 
		                position.y + y * rectSize.y / fieldSize.y, 
		                position.x + (x + 1) * rectSize.x / fieldSize.x, 
		                position.y + (y + 1) * rectSize.y / fieldSize.y, pain);
	}
	
	public void drawDot(int x, int y, int color, Canvas canvas){
		pain.setColor(color);
		pain.setStyle(Style.FILL);
		canvas.drawCircle(position.x + ((float) x + .5f) * rectSize.x / fieldSize.x, 
		                  position.y + ((float) y + .5f) * rectSize.y / fieldSize.y, 
		                  rectSize.x / fieldSize.x * .12f, pain);
	}
	public void drawCarriage(int x, int y, int color, Canvas canvas){
		pain.setColor(color);
		pain.setStyle(Style.STROKE);
		canvas.drawRect(position.x + x * rectSize.x / fieldSize.x + 1, 
		                position.y + y * rectSize.y / fieldSize.y + 1, 
		                position.x + (x + 1) * rectSize.x / fieldSize.x - 2, 
		                position.y + (y + 1) * rectSize.y / fieldSize.y - 2, pain);
	}
	
	public void drawTempShip(Vector sPosition, Vector sDirection, int sSize, Canvas canvas){
		for (int i = 0; i < sSize; ++i)
			drawCell(Math.round(sPosition.x + sDirection.x * i), 
			         Math.round(sPosition.y + sDirection.y * i), 
			         isShipPlacementValid(sPosition, sDirection, sSize) ? Color.GREEN : Color.LTGRAY, canvas);
	}

	public boolean isShipPlacementValid(Vector sPosition, Vector sDirection, int sSize){
		for (int i = 0; i < sSize; ++i)
			if (!isCellPlacementValid(Math.round(sPosition.x + sDirection.x * i), 
			                          Math.round(sPosition.y + sDirection.y * i)))
				return false;
		return true;
	}

	public boolean isCellPlacementValid(int x, int y){//=C
		if (x < 0 || x >= fieldSize.x ||
		    y < 0 || y >= fieldSize.y)
			return false;
		
		return !checkCell(x, y, CellState.SHIP);
	}
	
	public boolean checkCell(int x, int y, CellState target){
		if (x < 0 || x >= fieldSize.x ||
		    y < 0 || y >= fieldSize.y)
			return false;
		
		if (y < fieldSize.y - 1 &&
		    get(x, y + 1).equals(target))
			return true;
		if (y < fieldSize.y - 1 && x < fieldSize.x - 1 &&
		    get(x + 1, y + 1).equals(target))
			return true;
		if (x < fieldSize.x - 1 &&
		    get(x + 1, y).equals(target))
			return true;
		if (x < fieldSize.x - 1 && y > 0 &&
		    get(x + 1, y - 1).equals(target))
			return true;
		if (y > 0 &&
		    get(x, y - 1).equals(target))
			return true;
		if (x > 0 && y > 0 &&
		    get(x - 1, y - 1).equals(target))
			return true;
		if (x > 0 &&
		    get(x - 1, y).equals(target))
			return true;
		if (x > 0 && y < fieldSize.y - 1 &&
		    get(x - 1, y + 1).equals(target))
			return true;
		if (get(x, y).equals(target))
			return true;
		return false;
	}
	
	public boolean place(Vector sPosition, Vector sDirection, int sSize, int id){
		if (isShipPlacementValid(sPosition, sDirection, sSize)){
			for (int i = 0; i < sSize; ++i){
				set(Math.round(sPosition.x + sDirection.x * i), Math.round(sPosition.y + sDirection.y * i), CellState.SHIP);
				setID(Math.round(sPosition.x + sDirection.x * i), Math.round(sPosition.y + sDirection.y * i), id);
			}
			return true;
		} else 
			return false;
	}
	
	public CellState get(int x, int y){
		return cells[Math.round(x + y * fieldSize.x)];
	}
	
	public int getID(int x, int y){
		return shipID[Math.round(x + y * fieldSize.x)];
	}
	
	public void set(int x, int y, CellState state){
		cells[Math.round(x + y * fieldSize.x)] = state;
	}
	
	public void setID(int x, int y, int id){
		shipID[Math.round(x + y * fieldSize.x)] = id;
	}
	
	public enum HitResponce {
		HIT, MISS, IGNORED
	}
	public HitResponce hitMe(int x, int y){
		if (get(x, y).equals(CellState.SHIP)){
			set(x, y, CellState.SHOT_HIT);
			shipWreck(getID(x, y));
			return HitResponce.HIT;
		} else if (get(x, y).equals(CellState.UNKNOWN)){
			set(x, y, CellState.SHOT_MISSED);
			return HitResponce.MISS;
		}
		return HitResponce.IGNORED;
	}
	
	public void shipWreck(int id){
		for (int i = 0; i < shipID.length; ++i){
			if (shipID[i] == id && cells[i].equals(CellState.SHIP))
				return;
		}
		for (int x = 0; x < fieldSize.x; ++x)
			for (int y = 0; y < fieldSize.y; ++y)
				if (getID(x, y) == id){
					set(x, y, CellState.SHIP_DESTROYED);
					for (int xx = 0; xx < fieldSize.x; ++xx)
						for (int yy = 0; yy < fieldSize.y; ++yy){
							if (xx - x <= 1 && xx - x >= -1 &&
							    yy - y <= 1 && yy - y >= -1 &&
							    get(xx, yy).equals(CellState.UNKNOWN))
								set(xx, yy, CellState.EMPTY);
						}
				}
	}
	
	public boolean anyShipsLeft(){
		for (CellState cs : cells)
			if (cs.equals(CellState.SHIP))
				return true;
		return false;
	}
}
