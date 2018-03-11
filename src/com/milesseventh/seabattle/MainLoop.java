package com.milesseventh.seabattle;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;

public class MainLoop extends Thread {
	public SurfaceHolder sh;
	public boolean running = true;
	public Game game;
	
	@Override
	public void run(){
		Rect r = sh.getSurfaceFrame();
		game = new Game(r.width(), r.height());
		while (running){
			Canvas c = sh.lockCanvas();
			if (c == null){
				running = false;
				break;
			}
			c.drawColor(Color.WHITE);
			game.update(c);
			if (game.restart)
				game = new Game(r.width(), r.height());
			sh.unlockCanvasAndPost(c);
		}
	}
}
