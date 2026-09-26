package view;

import javax.swing.*;

import client.Client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

import model.Friend;

public class LoginView extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField textName;
	private JTextField textPass;
	private JLabel lblNewLabel_2;

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				LoginView frame = new LoginView();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public LoginView() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 628, 411);

		// Giao diện chính có ảnh nền
		contentPane = new JPanel() {
			private static final long serialVersionUID = 1L;
			private Image backgroundImage = new ImageIcon("src/view/anhlogin.jpg").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
			}
		};
		setContentPane(contentPane);
		contentPane.setLayout(null);

		textName = new JTextField();
		textName.setFont(new Font("Tahoma", Font.PLAIN, 18));
		textName.setBounds(179, 108, 411, 45);
		contentPane.add(textName);

		textPass = new JTextField();
		textPass.setFont(new Font("Tahoma", Font.PLAIN, 18));
		textPass.setBounds(179, 182, 411, 45);
		contentPane.add(textPass);

		JButton btnLogin = new JButton("Đăng nhập");
		btnLogin.setBackground(new Color(0, 128, 0));
		btnLogin.setForeground(new Color(255, 255, 255));
		btnLogin.setFont(new Font("Tahoma", Font.BOLD, 18));
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doLogin();
			}
		});
		btnLogin.setBounds(360, 267, 181, 59);
		contentPane.add(btnLogin);

		JLabel lblNewLabel = new JLabel("Tên");
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
		lblNewLabel.setBounds(38, 105, 78, 44);
		contentPane.add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("Mật Khẩu");
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 18));
		lblNewLabel_1.setBounds(38, 179, 112, 45);
		contentPane.add(lblNewLabel_1);

		lblNewLabel_2 = new JLabel("Chat Client-Server SBTC");
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 22));
		lblNewLabel_2.setBounds(147, 32, 310, 45);
		contentPane.add(lblNewLabel_2);

		JButton btndang_Ki = new JButton("Đăng Kí");
		btndang_Ki.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RegisterUser registerForm = new RegisterUser();
				registerForm.setVisible(true);
				dispose(); // đóng cửa sổ đăng nhập hiện tại (nếu muốn)
			}
		});

		btndang_Ki.setBackground(new Color(0, 0, 160));
		btndang_Ki.setForeground(Color.WHITE);
		btndang_Ki.setFont(new Font("Tahoma", Font.BOLD, 18));
		btndang_Ki.setBounds(97, 267, 181, 59);
		contentPane.add(btndang_Ki);
	}

	private void doLogin() {
		String username = textName.getText().trim();
		String password = textPass.getText().trim();

		try {
			Socket socket = new Socket("localhost", 2209);
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			DataInputStream dis = new DataInputStream(socket.getInputStream());

			dos.writeUTF(username);
			dos.writeUTF(password);
			dos.flush();

			String response = dis.readUTF();
			if ("LOGIN_SUCCESS".equals(response)) {
				JOptionPane.showMessageDialog(this, "Đăng nhập thành công!");

				// Mở ChatWindow
				ChatWindow chat = new ChatWindow();
				chat.setVisible(true);
				chat.setUser(username);

				// 🟩 Nhận danh sách bạn bè 1 lần duy nhất
				int size = dis.readInt();
				java.util.List<Friend> allFriends = new java.util.ArrayList<>();
				for (int i = 0; i < size; i++) {
					int id = dis.readInt();
					String friendName = dis.readUTF();
					allFriends.add(new Friend(id, friendName));
				}
				// ✅ Cập nhật list sau khi đọc xong toàn bộ
				chat.updateFriendList(allFriends);

				// Khởi tạo client chat (truyền ChatWindow để hiển thị tin nhắn)
				Client client = new Client(chat);
				chat.setClient(client);

				// Gắn socket đã login vào client
				client.setSocket(socket, dis, dos);

				// Chạy client
				new Thread(client).start();

				// Đóng login
				this.dispose();
			} else {
				JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!");
				socket.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không kết nối được server!");
		}
	}
}