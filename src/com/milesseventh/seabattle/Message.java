package com.milesseventh.seabattle;

public class Message {
	static enum Command {
		HANDSHAKE, HIT
	}
	public Command command;
	public Field f;
	public int[] config;
	public int x, y;
	public boolean miss;
}
