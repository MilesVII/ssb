package com.milesseventh.seabattle;

import java.net.InetAddress;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.milesseventh.seabattle.Game.State;

public abstract class Communicator {
	public static final int PORT = 7711;
	
	public Connection opponent;
	private Game host;
	private Server server;
	private Client client;
	public boolean isClient;
	
	public Communicator(Game nhost) {
		host = nhost;
	}

	private void initClient(){
		if (client != null)
			return;
		
		client = new Client();
		registerClasses(client.getKryo());
		client.addListener(new Listener(){
			@Override
			public void connected(Connection c){
				Game.shout("Established connection with " + c.getRemoteAddressTCP().getAddress().getHostAddress());
				isClient = true;
				opponent = c;
				
				Message m = new Message();
				m.command = Message.Command.HANDSHAKE;
				m.f = host.myF;
				m.config = host.configuration;
				client.sendTCP(m);
				
				disableServer();
				
				if (host.resumed)
					if (host.loadedGameStateIsActive)
						host.gameState = Game.State.PLAYING;
					else
						host.gameState = Game.State.WAITING;
				else
					host.gameState = Game.State.WAITING;
			}
			
			@Override
			public void received(Connection c, Object data){
				if (!(data instanceof Message))
					return;
				try {
					messageReceived((Message)data);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void disconnected(Connection c){
				connectionLost();
			}
		});
	}
	
	private void initServer(){
		if (server != null)
			return;
		
		server = new Server();
		registerClasses(server.getKryo());
		try{
			server.bind(PORT);
		} catch (Exception e){}
		server.addListener(new Listener(){
			@Override
			public void connected(Connection c){
				Game.shout("Established connection with " + c.getRemoteAddressTCP().getAddress().getHostAddress());
				isClient = false;
				opponent = c;
				
				Message m = new Message();
				m.command = Message.Command.HANDSHAKE;
				m.f = host.myF;
				m.config = host.configuration;
				server.sendToTCP(c.getID(), m);
				disableClient();
				
				if (host.resumed)//TODO: Code dublication
					if (host.loadedGameStateIsActive)
						host.gameState = Game.State.PLAYING;
					else
						host.gameState = Game.State.WAITING;
				else
					host.gameState = Game.State.PLAYING;
			}
			
			@Override
			public void received(Connection c, Object data){
				if (!(data instanceof Message))
					return;
				try {
					messageReceived((Message)data);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void disconnected(Connection c){
				connectionLost();
			}
		});
	}
	
	private void messageReceived(Message m){
		switch (m.command){
		case HANDSHAKE:
			handshakeReceived(m.f, m.config);
			break;
		case HIT:
			hitReceived(m.x, m.y);
			break;
		}
	}
	
	private void connectionLost(){
		Game.shout("Connection lost");
		host.resumed = true;
		host.loadedGameStateIsActive = host.gameState.equals(Game.State.PLAYING);
		host.gameState = State.CONNECTING;
		
		if (isClient){
			startListening();
			disableClient();
		}
	}
	
	public void connect(byte[] ip){
		initClient();
		try {
			client.start();
			client.connect(1000, InetAddress.getByAddress(ip), PORT);
		} catch (Exception e) {
			Game.shout("Failed");
			e.printStackTrace();
		}
	}
	
	public void startListening(){
		killEveryServer();
		initServer();
		server.start();
	}
	
	private void killEveryServer(){
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Thread[] threads = threadSet.toArray(new Thread[threadSet.size()]);
		for (Thread t : threads)
			if (t.getName().contains("Server") && t.isAlive())
				t.interrupt();
	}
	
	public void disableServer(){
		if (server != null){
			for (Connection c : server.getConnections())
				c.close();
			server.getUpdateThread().interrupt();
			server.stop();
			try{
				server.close();
			} catch (NullPointerException npe){}
			server = null;
		}
	}
	
	public void disableClient(){
		if (client != null){
			client.getUpdateThread().interrupt();
			client.stop();
			try{
				client.close();
			} catch (NullPointerException npe){}
			client = null;
		}
	}
	
	private void registerClasses(Kryo k){
		k.register(Message.class);
		k.register(Message.Command.class);
		k.register(Field.class);
		k.register(Field.CellState.class);
		k.register(Field.CellState[].class);
		k.register(Vector.class);
		k.register(java.util.Locale.class);
		k.register(int[].class);
	}
	
	public void send(int x, int y){
		Message m = new Message();
		m.command = Message.Command.HIT;
		m.x = x;
		m.y = y;
		if (isClient)
			client.sendTCP(m);
		else
			server.sendToTCP(opponent.getID(), m);
	}

	public abstract void hitReceived(int x, int y);
	public abstract void handshakeReceived(Field f, int[] config);
	/*
					switch (m.command){
					case HANDSHAKE:
						
						break;
					case HIT:
						break;
					}*/
}
