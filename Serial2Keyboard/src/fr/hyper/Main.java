package fr.hyper;

public class Main {

	public static void main(String[] args) {
		Serial2Keyboard elem = new Serial2Keyboard();
		while(!elem.shouldClose()) elem.update();
	}

}
