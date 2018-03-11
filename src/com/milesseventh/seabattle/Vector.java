package com.milesseventh.seabattle;

import java.io.Serializable;

public class Vector implements Serializable{
	/****/
	private static final long serialVersionUID = 6839553152968857216L;
	//Temporary implementation of 2D vector
	//Should not be used for real projects
	public float x = 0, y = 0;

	public Vector(){}

	public Vector(float xx, float yy){
		x = xx; y = yy;
	}
	
	public void mimic(Vector v){
		x = v.x; y = v.y;
	}

	public Vector cpy(){
		return new Vector(x, y);
	}
	public float distance(Vector v){
		return distance(this, v);
	}

	public static float distance(Vector a, Vector b){
		return Vector.getVector(a).sub(b).length();
		
	}

	public float distance2(Vector v){
		return distance(this, v);
	}

	public static float distance2(Vector a, Vector b){
		return Vector.getVector(a).sub(b).length2();
		
	}

	public float length(){
		return length(x, y);
	}

	public static float length(float x, float y){
		return (float)Math.sqrt(x * x + y * y);
	}

	public float length2(){
		return length2(x, y);
	}
	
	public static float length2(float x, float y){
		return x * x + y * y;
	}

	public Vector normalized(){
		return scale(1 / length());
	}

	public Vector normalize(){
		mimic(scale(1 / length()));
		return this;
	}

	public Vector add(Vector v){
		x += v.x; y += v.y;
		return this;
	}

	public Vector added(float nx, float ny){
		return Vector.getVector(x + nx, y + ny);
	}
	public Vector sub(Vector v){
		x -= v.x; y -= v.y;
		return this;
	}

	//Processed vectors are loaded from pool and should not be used as global values
	//Should be used carefully
	public Vector scale(float s){
		mimic(scaled(s));
		return this;
	}
	
	public Vector scaled(float s){
		Vector v = getVector(this);
		v.x *= s; v.y *= s;
		return v;
	}

	public Vector multiply(Vector nv){
		mimic(multiplied(nv));
		return this;
	}
	
	public Vector multiplied(Vector nv){
		Vector v = getVector(this);
		v.x *= nv.x; v.y *= nv.y;
		return v;
	}

	public Vector rotateNormal90CW(){
		if (x == 0 && y == 1){
			x = 1; y = 0;
		} else if (x == 1 && y == 0){
			x = 0; y = -1;
		} else if (x == 0 && y == -1){
			x = -1; y = 0;
		} else if (x == -1 && y == 0){
			x = 0; y = 1;
		}
		return this;
	}
	
	static float dot(Vector a, Vector b){
		return a.x * b.x + a.y * b.y;
	}

	static float project(Vector v0, Vector v1){
		return dot(v0, v1) / dot(v1, v1);
	}

	//Vector pool
	private static final int VECTORS_IN_POOL = 512;
	private static Vector[] vpool = new Vector[VECTORS_IN_POOL];
	private static int vectorsCounter = 0, holder;
	public static Vector getVector(){//new Vector2() alternative
		return getVector(0, 0);
	}
	public static Vector getVector(Vector in){//cpy() alternative
		return getVector(in.x, in.y);
	}
	public static Vector getVector(float x, float y){//new Vector2(x, y) alternative
		//if (true)
			//return new Vector(x, y);
		
		if (vpool[vectorsCounter] == null)
			vpool[vectorsCounter] = new Vector();
		vpool[vectorsCounter].x= x;
		vpool[vectorsCounter].y= y;
		holder = vectorsCounter;
		++vectorsCounter;
		vectorsCounter %= VECTORS_IN_POOL;
		return vpool[holder];
	}
}
