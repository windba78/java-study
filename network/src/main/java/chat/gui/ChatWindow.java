package chat.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

public class ChatWindow {

	private Frame frame;
	private Panel pannel;
	private Button buttonSend;
	private TextField textField;
	private TextArea textArea;
	private TextArea list;
	private Socket socket;
	BufferedReader br = null;
	PrintWriter pw = null;
	private boolean kickCheck = false;
	private boolean changeCheck = false;

	public ChatWindow(String name, Socket socket) {
		frame = new Frame(name);
		pannel = new Panel();
		buttonSend = new Button("Send");
		textField = new TextField();
		textArea = new TextArea(30, 80);
		list = new TextArea(30, 10);
		this.socket = socket;
	}

	public void show() {
		/*
		 * 1. UI 초기화
		 */

		// Button
		buttonSend.setBackground(Color.GRAY);
		buttonSend.setForeground(Color.WHITE);
		buttonSend.addActionListener(actionEvent -> sendMessage());

		// Textfield
		textField.setColumns(80);
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				char keyCode = e.getKeyChar();
				if (keyCode == KeyEvent.VK_ENTER) {
					sendMessage();
				}
			}
		});

		// Pannel
		pannel.setBackground(Color.LIGHT_GRAY);
		pannel.add(textField);
		pannel.add(buttonSend);
		frame.add(BorderLayout.SOUTH, pannel);

		// TextArea
		textArea.setEditable(false);
		frame.add(BorderLayout.CENTER, textArea);

		// List
		list.setEditable(false);
		frame.add(BorderLayout.EAST, list);

		// Frame
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				finish();
			}
		});
		frame.setVisible(true);
		frame.pack();

		/*
		 * 2. IOStream (pipeline established)
		 */
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

			/*
			 * 3. Chat Client Thread 생성 및 실행
			 */
			new ChatClientThread().start();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void sendMessage() {
		String message = textField.getText();
		if ("quit".equals(message)) {
			finish();
		} else if ("change".equals(message)) {
			pw.println("change");
			changeCheck = true;
		} else if (changeCheck) {
			pw.println(message);
			changeCheck = false;
		} else if ("kick".equals(message)) {
			pw.println("kick");
			kickCheck = true;
		} else if (kickCheck) {
			pw.println(message);
			kickCheck = false;
		} else {
			pw.println("message:" + message);
		}
		textField.setText("");
		textField.requestFocus();
	}

	private void updateTextArea(String message) {
		textArea.append(message);
		textArea.append("\n");
	}

	private void finish() {
		pw.println("quit");
		System.exit(0);
	}

	private class ChatClientThread extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					while (true) {
						String data = br.readLine();
						if (data == null) {
							break;
						}
						updateTextArea(data);
					}
				} catch (IOException e) {
					// Stream closed
				} finally {
					try {
						if (br != null) {
							br.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}