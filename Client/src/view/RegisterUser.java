package view;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class RegisterUser extends JFrame {
	private JTextField txtUsername, txtPhoneNumber;
	private JPasswordField txtPassword;
	private JComboBox<String> cboGender;
	private JPanel contentPane;

	public RegisterUser() {
		setTitle("Đăng ký tài khoản");
		setSize(682, 501);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		contentPane = new JPanel() {
			private static final long serialVersionUID = 1L;
			private Image backgroundImage = new ImageIcon("src/view/anhlogin2.jpg").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
			}
		};
		setContentPane(contentPane);
		contentPane.setLayout(null);

		// --- Panel nhập liệu ---
		JPanel pnlForm = new JPanel();
		pnlForm.setBounds(25, 32, 615, 405);
		pnlForm.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		pnlForm.setLayout(null);

		// cho panel trong suốt để thấy ảnh nền của contentPane
		pnlForm.setOpaque(false);

		contentPane.add(pnlForm);

		JLabel label_1 = new JLabel("Tên đăng nhập:");
		label_1.setForeground(new Color(255, 255, 255));
		label_1.setFont(new Font("Tahoma", Font.PLAIN, 18));
		label_1.setBounds(20, 74, 202, 46);
		pnlForm.add(label_1);
		txtUsername = new JTextField();
		txtUsername.setFont(new Font("Tahoma", Font.PLAIN, 18));
		txtUsername.setBounds(281, 74, 334, 46);
		pnlForm.add(txtUsername);

		JLabel label_2 = new JLabel("Mật khẩu:");
		label_2.setForeground(new Color(255, 255, 255));
		label_2.setFont(new Font("Tahoma", Font.PLAIN, 18));
		label_2.setBounds(20, 147, 202, 46);
		pnlForm.add(label_2);
		txtPassword = new JPasswordField();
		txtPassword.setFont(new Font("Tahoma", Font.PLAIN, 18));
		txtPassword.setBounds(281, 147, 334, 46);
		pnlForm.add(txtPassword);

		JLabel label_3 = new JLabel("Số điện thoại:");
		label_3.setForeground(new Color(255, 255, 255));
		label_3.setFont(new Font("Tahoma", Font.PLAIN, 18));
		label_3.setBounds(20, 213, 202, 46);
		pnlForm.add(label_3);
		txtPhoneNumber = new JTextField();
		txtPhoneNumber.setFont(new Font("Tahoma", Font.PLAIN, 16));
		txtPhoneNumber.setBounds(281, 214, 334, 46);
		pnlForm.add(txtPhoneNumber);

		JLabel label_4 = new JLabel("Giới tính:");
		label_4.setForeground(new Color(255, 255, 255));
		label_4.setFont(new Font("Tahoma", Font.PLAIN, 18));
		label_4.setBounds(20, 282, 202, 46);
		pnlForm.add(label_4);
		cboGender = new JComboBox<>(new String[] { "Nam", "Nữ", "Khác" });
		cboGender.setFont(new Font("Tahoma", Font.PLAIN, 16));
		cboGender.setBounds(281, 283, 334, 46);
		pnlForm.add(cboGender);

		contentPane.add(pnlForm);

		// --- Panel tiêu đề ---
		JLabel lblTitle = new JLabel("FORM ĐĂNG KÝ NGƯỜI DÙNG", SwingConstants.CENTER);
		lblTitle.setBounds(0, 10, 615, 32);
		pnlForm.add(lblTitle);
		lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
		lblTitle.setForeground(Color.BLUE);

		JButton btnNewButton = new JButton("Đăng kí");
		btnNewButton.setForeground(new Color(0, 0, 255));
		btnNewButton.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					String username = txtUsername.getText().trim();
					String password = new String(txtPassword.getPassword());
					String phone = txtPhoneNumber.getText().trim();
					String gender = (String) cboGender.getSelectedItem();

					if (username.isEmpty() || password.isEmpty() || phone.isEmpty()) {
						JOptionPane.showMessageDialog(RegisterUser.this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					boolean success = controller.RegisterController.registerUser(username, password, phone, gender);

					if (success) {
						JOptionPane.showMessageDialog(RegisterUser.this,
								"✅ Đăng ký tài khoản người dùng mới thành công!");
						clearForm();
					} else {
						JOptionPane.showMessageDialog(RegisterUser.this, "❌ Đăng ký thất bại! Vui lòng thử lại.", "Lỗi",
								JOptionPane.ERROR_MESSAGE);
					}

				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(RegisterUser.this, "ID phải là số!", "Lỗi nhập liệu",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		btnNewButton.setBounds(68, 358, 211, 47);
		pnlForm.add(btnNewButton);

		JButton btndangnhap = new JButton("Đăng Nhập");
		btndangnhap.setForeground(new Color(0, 128, 0));
		btndangnhap.setFont(new Font("Tahoma", Font.BOLD, 20));
		btndangnhap.setBounds(362, 358, 211, 47);
		btndangnhap.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LoginView loginview = new LoginView();
				loginview.setVisible(true);
				dispose();
			}
		});
		pnlForm.add(btndangnhap);
	}

	private void registerUser() {
		try {
			String username = txtUsername.getText().trim();
			String password = new String(txtPassword.getPassword());
			String phone = txtPhoneNumber.getText().trim();
			String gender = (String) cboGender.getSelectedItem();
			LocalDateTime createAt = LocalDateTime.now();

			if (username.isEmpty() || password.isEmpty() || phone.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập đúng", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void clearForm() {
		txtUsername.setText("");
		txtPassword.setText("");
		txtPhoneNumber.setText("");
		cboGender.setSelectedIndex(0);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new RegisterUser().setVisible(true);
		});
	}
}
