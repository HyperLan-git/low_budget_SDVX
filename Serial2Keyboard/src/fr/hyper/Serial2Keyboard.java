package fr.hyper;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class Serial2Keyboard {
	private Robot robot;
	private SerialPort port = null;

	private JFrame window;
	private boolean shouldClose = false, started = false;

	private JComboBox<String> portSelector;
	private JButton startButton = new JButton("Start");
	private JTextArea logging = new JTextArea("Messages Recieved", 20, 40);

	private JRadioButtonMenuItem loggingOption = new JRadioButtonMenuItem("Enable logging");

	public Serial2Keyboard() {
		try {
			robot = new Robot();
		} catch (AWTException e) {
			JOptionPane.showConfirmDialog(null,
					"This program cannot run without elevated privilege !",
					"Error", JOptionPane.ERROR_MESSAGE);
			throw new IllegalStateException("Could not create robot");
		}
		portSelector = new JComboBox<String>(SerialPortList.getPortNames());

		logging.setEditable(false);
		logging.setAutoscrolls(true);

		startButton.addActionListener(e -> {
			started = !started;
			startButton.setText(started ? "Stop" : "Start");
		});

		window = new JFrame("Serial2Keyboard");

		JMenuBar bar = new JMenuBar();
		JMenu menu = new JMenu("Options");
		JMenuItem reload = new JMenuItem("Reload Ports");
		reload.addActionListener(e -> {
			portSelector.removeAllItems();
			for(String str : SerialPortList.getPortNames())
				portSelector.addItem(str);
		});
		menu.add(loggingOption);
		menu.add(reload);
		bar.add(menu);
		window.setJMenuBar(bar);

		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setMinimumSize(new Dimension(400, 250));
		window.setLayout(new BorderLayout());
		window.add(portSelector, BorderLayout.NORTH);
		window.add(new JScrollPane(logging), BorderLayout.CENTER);
		window.add(startButton, BorderLayout.SOUTH);
		window.setVisible(true);
	}

	public boolean shouldClose() {
		return shouldClose;
	}

	public void update() {
		try {
			if (!started) {
				if (port != null) port.closePort();
				port = null;
				return;
			}
			if (port == null) {
				if (portSelector.getSelectedItem() == null) return;
				port = new SerialPort(portSelector.getSelectedItem().toString());
				if (!port.openPort()) {
					logging.setText("Failed to open port !");
					port = null;
					return;
				}
				port.setParams(SerialPort.BAUDRATE_9600,
						SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				logging.setText("Messages Recieved");
			}
			if (port.getInputBufferBytesCount() > 0) {
				StringBuilder log = null;
				if (loggingOption.isSelected()) log = new StringBuilder(logging.getText());

				byte[] data = port.readBytes();

				for (byte b : data) {
					char c = Character.toChars(b)[0];

					if (loggingOption.isSelected() && log != null) {
						String stamp = new Timestamp(System.currentTimeMillis()).toString();
						while (stamp.length() < 23) stamp += '0';
						log.insert(0, "[" + stamp + "] " + c + "\n");
					}

					int keycode = KeyEvent.getExtendedKeyCodeForChar(c);
					if (keycode == KeyEvent.VK_UNDEFINED) continue;

					robot.keyPress(keycode);
					robot.keyRelease(keycode);
				}
				if (loggingOption.isSelected() && log != null) logging.setText(log.toString());
			} else 
				if(logging.getText().length() > 5000) logging.setText("Messages Recieved");
		} catch (SerialPortException e) {
			e.printStackTrace();
			if (port != null)
				try {
					port.closePort();
				} catch (SerialPortException e1) {
					// Ignore
				}
			logging.setText("Communication error !");
			port = null;
		}
	}
}
