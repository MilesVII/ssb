package com.milesseventh.seabattle;

import java.io.Serializable;

public class StateBox implements Serializable{
	/****/
	private static final long serialVersionUID = 219016705302807900L;
	public Game.State gameState;
	public Field f;
	public int[] config;
	public int placerOffset;
	public byte[] opponent;
	
	public StateBox() {}
}
