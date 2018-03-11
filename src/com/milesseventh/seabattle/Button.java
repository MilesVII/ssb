package com.milesseventh.seabattle;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public abstract class Button {
	public Vector position, size;
	public Paint pain;
	
	public Button(Vector nposition, Vector nsize){
		position = nposition.cpy();
		size = nsize.cpy();
		pain = new Paint();
	}
	
	public void render(Vector touch, boolean released, Canvas canvas){
		pain.setColor(Color.BLACK);
		pain.setStyle(Style.STROKE);
		canvas.drawRect(position.x, position.y + size.y, position.x + size.x, position.y, pain);
		if (position.x < touch.x && position.y < touch.y &&
			position.x + size.x > touch.x && position.y + size.y > touch.y){
			pain.setColor(Color.rgb(218, 64, 0));
			pain.setStyle(Style.FILL);
			canvas.drawRect(position.x, position.y + size.y, position.x + size.x, position.y, pain);
			if (released)
				action();
		}
	}
	
	public abstract void action();
}
