package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import client.Client;
import model.Conversation;
import model.Friend;

public class ChatWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField textField;

	private JLabel lbUserName;
	private JButton btnSend;
	private Client client;

	private int selectedFriendId = -1;
	private int selectedGroupId = -1; // id nhóm
	private String selectedFriendName = null;
	// Danh sách lịch sử tin nhắn theo từng bạn
	private Map<Integer, java.util.List<String>> chatHistory = new HashMap<>();

	private JPanel messagePanel;
	private JScrollPane scrollPane;

	private DefaultListModel<Conversation> conversationModel; // thay friend bằng conversation
	private JList<Conversation> conversationList; // thay friend bằng conversation

	private Friend friend;

	// username hiện tại đã có qua setUser(String)
	private String currentUsername;

	public void setClient(Client client) {
		this.client = client;
	}

	public void setUser(String user) {
		this.currentUsername = user;
		lbUserName.setText(user);
	}

	// Server trả về lịch sử cho otherUserId
	public void renderHistory(int otherUserId, java.util.List<String[]> rows) {
		// Chỉ render nếu panel hiện đang chọn đúng người
		if (selectedFriendId != otherUserId)
			return;

		messagePanel.removeAll();
		for (String[] r : rows) {
			String senderName = r[0];
			String content = r[1];
			String ts = r[2];

			boolean isMe = senderName.equals(currentUsername);
			String who = isMe ? "Bạn" : senderName;
			String text = "[" + who + " • " + ts + "] " + content;

			addMessageBubble(text, isMe);
		}
		messagePanel.revalidate();
		messagePanel.repaint();

	}
	
	// bẻ dòng 
	
	private String wrapText(String text, int maxLen) {
	    if (text == null || text.length() <= maxLen) return text;

	    StringBuilder sb = new StringBuilder();
	    int i = 0;
	    while (i < text.length()) {
	        int end = Math.min(i + maxLen, text.length());
	        sb.append(text, i, end);
	        if (end < text.length()) {
	            sb.append("<br>");   // xuống dòng HTML
	        }
	        i = end;
	    }
	    return sb.toString();
	}


	public void addMessage(String msg) {
		JLabel label = new JLabel(msg);
		label.setFont(new Font("Tahoma", Font.PLAIN, 16));
		messagePanel.add(label);
		messagePanel.revalidate();
		messagePanel.repaint();
	}

	private void addMessageBubble(String text, boolean isMe) {
	    // Panel ngoài để canh trái/phải - cũng phải gói gọn
	    JPanel line = new JPanel(new BorderLayout());
	    line.setOpaque(false);

	    String wrapped = wrapText(text, 50);
	    String html =
	        "<html><body style='word-wrap:break-word; overflow-wrap:break-word;'>"
	        + wrapped +
	        "</body></html>";

	    JLabel bubble = new JLabel(html);
	    bubble.setFont(new Font("Tahoma", Font.PLAIN, 16));
	    bubble.setOpaque(false);
	    
	    // Panel nhỏ chứa bubble với FlowLayout
	    JPanel bubblePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	    bubblePanel.setOpaque(true);
	    bubblePanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
	    
	    Color myColor    = new Color(0xDCF8C6);
	    Color otherColor = new Color(0xD4E7F5);
	    bubblePanel.setBackground(isMe ? myColor : otherColor);
	    
	    bubblePanel.add(bubble);
	    
	    // Giới hạn kích thước bubblePanel
	    bubblePanel.setPreferredSize(bubblePanel.getPreferredSize());
	    bubblePanel.setMaximumSize(bubblePanel.getPreferredSize());

	    if (isMe) {
	        line.add(bubblePanel, BorderLayout.EAST);
	    } else {
	        line.add(bubblePanel, BorderLayout.WEST);
	    }

	    // *** Giới hạn chiều cao của line panel cũng ***
	    line.setMaximumSize(new Dimension(Integer.MAX_VALUE, line.getPreferredSize().height));

	    messagePanel.add(line);
	    messagePanel.add(Box.createVerticalStrut(5));
	    messagePanel.revalidate();
	    SwingUtilities.invokeLater(() -> {
	        scrollPane.getVerticalScrollBar().setValue(
	                scrollPane.getVerticalScrollBar().getMaximum());
	    });
	}



	public void addMyMessage(String msg) {
		addMessageBubble(msg, true);
	}

	public void addOtherMessage(String msg) {
		addMessageBubble(msg, false);
	}

	public void addImage(File file) {
		ImageIcon icon = new ImageIcon(file.getAbsolutePath());
		Image img = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
		icon = new ImageIcon(img);

		JLabel imgLabel = new JLabel(icon);
		messagePanel.add(imgLabel);
		messagePanel.revalidate();
		messagePanel.repaint();
	}

	public void addFileMessage(File file) {
		javax.swing.filechooser.FileSystemView fileSystemView = javax.swing.filechooser.FileSystemView
				.getFileSystemView();
		javax.swing.Icon fileIcon = fileSystemView.getSystemIcon(file);

		JButton fileButton = new JButton(file.getName(), fileIcon);
		fileButton.setFont(new Font("Tahoma", Font.PLAIN, 16));
		fileButton.setHorizontalAlignment(JButton.LEFT);

		fileButton.addActionListener(e -> {
			try {
				if (file.exists()) {
					java.awt.Desktop.getDesktop().open(file);
					new FileReader(file); // chỉ mở file, chưa dùng đọc nội dung
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		messagePanel.add(fileButton);
		messagePanel.revalidate();
		messagePanel.repaint();
	}

	// nguyen thay updatefriend thành conversation
	public void updateFriendList(List<Friend> users) {
		conversationModel.clear();
		for (Friend user : users) {
			conversationModel.addElement(new Conversation(user.getId(), user.getName(), Conversation.Type.USER));
		}
	}

	public void addGroupConversation(int groupId, String groupName) {
		conversationModel.addElement(new Conversation(groupId, groupName, Conversation.Type.GROUP));
	}

	// NGUYEN THEMM

	// Lưu tin nhắn nhận được và thay friend thành conversation
	public void receivePrivateMessage(String from, String msg) {
	    // Nếu là tin do chính mình gửi thì bỏ qua (đã hiển thị lúc bấm Gửi)
	    if (from.equals(currentUsername)) {
	        return;
	    }

	    for (int i = 0; i < conversationModel.size(); i++) {
	        Conversation conv = conversationModel.get(i);
	        if (conv.getType() == Conversation.Type.USER && conv.getName().equals(from)) {

	        	java.time.LocalDateTime now = java.time.LocalDateTime.now();
	        	java.time.format.DateTimeFormatter fmt =
	        	        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	        	String ts = now.format(fmt);   // ví dụ: 2025-11-25 23:08:11

	            String text = "[" + from + " • " + ts + "] " + msg;

	            chatHistory
	                .computeIfAbsent(conv.getId(), k -> new ArrayList<>())
	                .add(text);

	            if (selectedFriendId == conv.getId()) {
	                addOtherMessage(text);   // isMe = false → nền xanh biển, bên trái
	            }
	            return;
	        }
	    }
	}


	private void openCreateGroupDialog() {
		// Danh sách bạn hiện có

		// Tạo danh sách bạn bè dựa trên Conversation
		DefaultListModel<Friend> friendModel = new DefaultListModel<>();
		for (int i = 0; i < conversationModel.size(); i++) {
			Conversation c = conversationModel.get(i);
			if (c.getType() == Conversation.Type.USER) {
				friendModel.addElement(new Friend(c.getId(), c.getName()));
			}
		}
		JList<Friend> list = new JList<>(friendModel);

		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setVisibleRowCount(10);
		JTextField tfName = new JTextField("Nhóm mới");

		JPanel panel = new JPanel(new java.awt.BorderLayout(5, 5));
		panel.add(new JLabel("Tên nhóm:"), java.awt.BorderLayout.NORTH);
		panel.add(tfName, java.awt.BorderLayout.CENTER);
		panel.add(new JScrollPane(list), java.awt.BorderLayout.SOUTH);

		int ok = JOptionPane.showConfirmDialog(this, panel, "Tạo nhóm chat", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (ok == JOptionPane.OK_OPTION) {
			String groupName = tfName.getText().trim();
			java.util.List<Friend> selected = list.getSelectedValuesList();
			if (groupName.isEmpty() || selected.isEmpty())
				return;

			try {
				// Gửi CREATE_GROUP
				client.sendCreateGroup(groupName, selected);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void renderGroupHistory(int groupId, List<String[]> rows) {
		messagePanel.removeAll();
		for (String[] r : rows) {
			String sender = r[0];
			String content = r[1];
			String ts = r[2];

			boolean isMe = sender.equals(currentUsername);
			String who = isMe ? "Bạn" : sender;
			String text = "[" + who + " • " + ts + "] " + content;

			addMessageBubble(text, isMe);
		}
		messagePanel.revalidate();
		messagePanel.repaint();

	}

	public void receiveGroupMessage(int groupId, String groupName, String sender, String content) {
	    Conversation current = conversationList.getSelectedValue();
	    if (current != null && current.getType() == Conversation.Type.GROUP && current.getId() == groupId) {
	        boolean isMe = sender.equals(currentUsername);
	        String ts = java.time.LocalDateTime.now().toString();
	        String who = isMe ? "Bạn" : sender;
	        String text = "[" + who + " • " + ts + "] " + content;

	        addMessageBubble(text, isMe);
	    }
	}


	// Xoá nhóm khỏi danh sách sau khi rời
	public void removeGroupFromList(int groupId) {
		for (int i = 0; i < conversationModel.size(); i++) {
			Conversation conv = conversationModel.get(i);
			if (conv.getType() == Conversation.Type.GROUP && conv.getId() == groupId) {
				conversationModel.remove(i);
				break;
			}
		}

		// Nếu đang mở đúng nhóm đó thì clear khung chat
		if (selectedGroupId == groupId) {
			selectedGroupId = -1;
			messagePanel.removeAll();
			messagePanel.revalidate();
			messagePanel.repaint();
		}
	}

	// nguyên thêm để gọi videooo
	private void startVideoCall(int toUserId) {
		try {
			client.requestVideoCall(toUserId);
			addMessage("Đang gọi video tới " + selectedFriendName + "...");
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Không gọi được video!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	public ChatWindow() {
		// bổ sung thêm ảnh bìa của appp
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 983, 591);

		// Giao diện chính có ảnh nền
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

		// them dau + tao cuoc tro chuyen
		JButton btnCreateGroup = new JButton("+");
		btnCreateGroup.setBounds(32, 16, 61, 38);
		btnCreateGroup.addActionListener(e -> openCreateGroupDialog());
		contentPane.add(btnCreateGroup);
		// Nút rời nhóm
		JButton btnLeaveGroup = new JButton("Rời nhóm");
		btnLeaveGroup.setFont(new Font("Tahoma", Font.BOLD, 14));
		btnLeaveGroup.setBounds(54, 494, 118, 51); // bạn có thể chỉnh lại toạ độ cho đẹp hơn

		btnLeaveGroup.addActionListener(e -> {
			Conversation conv = conversationList.getSelectedValue();
			if (conv == null || conv.getType() != Conversation.Type.GROUP) {
				JOptionPane.showMessageDialog(this, "Hãy chọn một nhóm trong danh sách để rời!", "Thông báo",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn rời nhóm \"" + conv.getName() + "\"?",
					"Xác nhận", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				try {
					if (client != null) {
						client.leaveGroup(conv.getId()); // Gửi yêu cầu lên server
					}
				} catch (IOException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(this, "Lỗi khi gửi yêu cầu rời nhóm!", "Lỗi",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		contentPane.add(btnLeaveGroup);

		// ====== Danh sách bạn bè bên trái ======
		conversationModel = new DefaultListModel<>();

		contentPane.setLayout(null);
		conversationList = new JList<>(conversationModel);
		conversationList.setFont(new Font("Tahoma", Font.BOLD, 16));
		conversationList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Thêm margin 10px phía trên và dưới

		// nguyen chỉnh để lấy lịch sử chat và thay thành nhóm chat

		conversationList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				Conversation conv = conversationList.getSelectedValue();
				if (conv != null) {
					selectedFriendId = -1;
					selectedGroupId = -1;
					selectedFriendName = null;

					messagePanel.removeAll();
					messagePanel.revalidate();
					messagePanel.repaint();

					try {
						if (conv.getType() == Conversation.Type.USER) {
							selectedFriendId = conv.getId();
							selectedFriendName = conv.getName();
							client.requestHistory(selectedFriendId);
						} else if (conv.getType() == Conversation.Type.GROUP) {
							selectedGroupId = conv.getId();
							client.requestGroupHistory(selectedGroupId);
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		JScrollPane friendScrollPane = new JScrollPane(conversationList);
		friendScrollPane.setBounds(10, 68, 205, 406);
		contentPane.add(friendScrollPane);

		JLabel lblNewLabel_1 = new JLabel("Cuộc trò chuyện :");
		lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblNewLabel_1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0)); // Thêm margin 10px phía trên và dưới
		friendScrollPane.setColumnHeaderView(lblNewLabel_1);

		// ====== Panel chứa tin nhắn ======
		messagePanel = new JPanel();
		messagePanel.setBackground(Color.WHITE);
		messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));

		scrollPane = new JScrollPane(messagePanel);
		scrollPane.setBounds(248, 68, 692, 406);
		contentPane.add(scrollPane);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		// ====== Label tên user ======
		lbUserName = new JLabel("Chưa đăng nhập");
		lbUserName.setForeground(new Color(255, 255, 255));
		lbUserName.setBounds(584, 10, 200, 44);
		lbUserName.setFont(new Font("Tahoma", Font.BOLD, 19));
		contentPane.add(lbUserName);

		// ====== Ô nhập tin nhắn ======
		textField = new JTextField();
		textField.setBounds(252, 496, 501, 48);
		contentPane.add(textField);
		textField.setColumns(10);

		// ====== Nút gửi ======
		btnSend = new JButton("Gửi");
		btnSend.setFont(new Font("Tahoma", Font.BOLD, 15));
		btnSend.setBounds(855, 492, 85, 51);

// nguyen chinh

		btnSend.addActionListener(e -> {
			if (client != null) {
				String msg = textField.getText().trim();
				if (!msg.isEmpty()) {
				    try {
				        // Tùy loại gửi mà tạo prefix giống HISTORY
				        String displayText;
				        java.time.format.DateTimeFormatter fmt =
				                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				        java.time.LocalDateTime now = java.time.LocalDateTime.now();

				        if (selectedFriendId > -1) {
				            client.sendDirectMessage(selectedFriendId, msg);
				            String ts = now.format(fmt);
				            displayText = "[Bạn • " + ts + "] " + msg;

				        } else if (selectedGroupId > -1) {
				            client.sendGroupMessage(selectedGroupId, msg);
				            String ts = now.format(fmt);
				            displayText = "[Bạn • " + ts + "] " + msg;

				        } else {
				            client.sendBroadcastMessage(msg);
				            String ts = now.format(fmt);
				            displayText = "[Bạn • " + ts + "] " + msg;
				        }


				        addMyMessage(displayText);   // luôn dùng cùng 1 dạng
				    } catch (IOException ex) {
				        ex.printStackTrace();
				    }
				    textField.setText("");
				}

			}
		});

		contentPane.add(btnSend);

		// ====== Nút chọn file ======
		JButton btnFile = new JButton("File");
		btnFile.setBounds(779, 494, 66, 48);
		btnFile.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			int response = fileChooser.showOpenDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				try {
					if (client != null) {
						if (selectedFriendId > -1) {
							// ✅ Gửi file riêng tư cho 1 người
							client.sendDirectFile(selectedFriendId, file);
						} else if (selectedGroupId > -1) {
							// ✅ Gửi file vào nhóm
							client.sendGroupFile(selectedGroupId, file);
						} else {
							JOptionPane.showMessageDialog(this, "Vui lòng chọn người hoặc nhóm để gửi file!",
									"Chưa chọn người nhận", JOptionPane.WARNING_MESSAGE);
							return;
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				// ✅ Hiển thị file trong cửa sổ chat hiện tại
				String name = file.getName().toLowerCase();
				if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif")) {
					addImage(file);
				} else {
					addFileMessage(file);
				}
			}
		});
		JButton btnCall = new JButton("Gọi");
		btnCall.setFont(new Font("Tahoma", Font.BOLD, 15));
		btnCall.setBounds(140, 9, 66, 49);
		btnCall.addActionListener(e -> {
			if (selectedFriendId > -1) {
				startVideoCall(selectedFriendId);
			} else {
				JOptionPane.showMessageDialog(this, "Hãy chọn 1 người để gọi video!");
			}
		});
		contentPane.add(btnCall);

		contentPane.add(btnFile);

		JLabel lblNewLabel = new JLabel("Đăng nhập với tên: ");
		lblNewLabel.setForeground(new Color(255, 255, 255));
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 19));
		lblNewLabel.setBounds(258, 9, 268, 45);
		contentPane.add(lblNewLabel);

	}
}
