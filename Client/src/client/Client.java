package client;

import java.awt.Image;
import java.io.*;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

//🔹 Thêm 4 dòng import bắt buộc này
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

//🔊 AUDIO
import javax.sound.sampled.*;

import common.Protocol;
import view.ChatWindow;

public class Client implements Runnable {


	static {
		System.loadLibrary("opencv_java4140");
	}

	private Socket client;
	private DataInputStream dis;
	private DataOutputStream dos;
	private ChatWindow ui;
	private JLabel videoFrameLabel;
	// ----- TRẠNG THÁI VIDEO CALL -----
	private javax.swing.JFrame videoCallFrame;
	private javax.swing.JLabel localVideoLabel; // video từ camera của mình
	private javax.swing.JLabel remoteVideoLabel; // video nhận từ người kia
	private volatile boolean videoCallRunning = false;
	// ----- TRẠNG THÁI AUDIO -----
	private volatile boolean audioRunning = false;
	private SourceDataLine speakerLine;  // loa phát tiếng bên kia

	private volatile boolean muteAudio = false;   // true = tắt tiếng micro
	private volatile boolean stopVideo = false;   // true = tắt gửi video

	private TargetDataLine micLine;
	private int currentVideoPartnerId = -1;
	private String currentVideoPartnerName;

	public Client(ChatWindow ui) {
		this.ui = ui;
	}

	// Hàm này để LoginView gán socket đã login thành công
	public void setSocket(Socket socket, DataInputStream dis, DataOutputStream dos) {
		this.client = socket;
		this.dis = dis;
		this.dos = dos;
	}

	@Override
	public void run() {
		try {
			// nguyen chinh lai ham while true de gui tin nhan rieng
			try {
				while (true) {
					if (ui == null)
						continue;
					String message = dis.readUTF();
					if (message.equals("FILE")) {
						receiveFile();

					} else if (message.equals("DM")) {
						String from = dis.readUTF();
						String content = dis.readUTF();

						// Debug ở client để chắc chắn nhận được
						System.out.println("Client nhận DM: from=" + from + ", msg=" + content);

						if (ui != null) {
							ui.receivePrivateMessage(from, content);
						}

					} else if (message.equals("HISTORY")) {
						int otherUserId = dis.readInt();
						int n = dis.readInt();
						java.util.List<String[]> rows = new java.util.ArrayList<>();
						for (int i = 0; i < n; i++) {
							String senderName = dis.readUTF();
							String content = dis.readUTF();
							String ts = dis.readUTF();
							rows.add(new String[] { senderName, content, ts });
						}
						ui.renderHistory(otherUserId, rows); // gọi UI cập nhật khung chat

					} else if (message.equals("GROUP_CREATED")) {
						int gid = dis.readInt();
						String gname = dis.readUTF();
						ui.addGroupConversation(gid, gname); // thêm vào list hội thoại

					} else if (message.equals("GROUP_HISTORY")) {
						int gid = dis.readInt();
						int n = dis.readInt();
						java.util.List<String[]> rows = new java.util.ArrayList<>();
						for (int i = 0; i < n; i++) {
							rows.add(new String[] { dis.readUTF(), dis.readUTF(), dis.readUTF() }); // sender, content,
							// ts
						}
						ui.renderGroupHistory(gid, rows);

					} else if (message.equals("GROUP_MSG")) {
						int gid = dis.readInt();
						String gname = dis.readUTF();
						String sender = dis.readUTF();
						String content = dis.readUTF();
						ui.receiveGroupMessage(gid, gname, sender, content);
					}

					// ===== VIDEO CALL PROTOCOL =====
					else if (message.equals(common.Protocol.RESP_VIDEO_CALL_INCOMING)) {
						int fromUserId = dis.readInt();
						String fromUser = dis.readUTF();
						System.out.println("Nhận cuộc gọi video từ " + fromUser);

						int choice = javax.swing.JOptionPane.showConfirmDialog(null,
								fromUser + " đang gọi video. Chấp nhận?", "Video call",
								javax.swing.JOptionPane.YES_NO_OPTION);

						if (choice == javax.swing.JOptionPane.YES_OPTION) {
							dos.writeUTF(Protocol.CMD_VIDEO_CALL_ACCEPT);
							dos.writeInt(fromUserId);
							dos.flush();

							currentVideoPartnerId = fromUserId;
							currentVideoPartnerName = fromUser;
							openVideoCallWindow(); // bên được gọi
						} else {
							dos.writeUTF(Protocol.CMD_VIDEO_CALL_REJECT);
							dos.writeInt(fromUserId);
							dos.flush();
						}

					} else if (message.equals(Protocol.RESP_VIDEO_CALL_ACCEPTED)) {
						// mình là người GỌI, được báo đã accept
						int partnerId = dis.readInt();
						String partnerName = dis.readUTF();

						currentVideoPartnerId = partnerId;
						currentVideoPartnerName = partnerName;
						openVideoCallWindow();

					} else if (message.equals(Protocol.RESP_VIDEO_CALL_REJECTED)) {
						String reason = dis.readUTF();
						ui.addMessage(reason);

					} else if (message.equals(Protocol.CMD_VIDEO_FRAME)) {
						// nhận frame từ phía kia
						int len = dis.readInt();
						byte[] bytes = new byte[len];
						dis.readFully(bytes);

						// ✅ SỬA LỖI 2: Kiểm tra kích thước Label để tránh lỗi chia cho 0
						if (remoteVideoLabel != null && remoteVideoLabel.getWidth() > 0 && remoteVideoLabel.getHeight() > 0) {
							ImageIcon icon = new ImageIcon(new ImageIcon(bytes).getImage().getScaledInstance(
									remoteVideoLabel.getWidth(), remoteVideoLabel.getHeight(), Image.SCALE_SMOOTH));
							SwingUtilities.invokeLater(() -> remoteVideoLabel.setIcon(icon));
						}

					} else if (message.equals(Protocol.RESP_VIDEO_CALL_ENDED)) {
						// phía kia tắt cuộc gọi
						endVideoCallLocalOnly();
					}
					// 👉 THÊM SAU RESP_VIDEO_CALL_ENDED
					else if (message.equals(Protocol.CMD_AUDIO_FRAME)) {
						int len = dis.readInt();
						byte[] pcm = new byte[len];
						dis.readFully(pcm);
						playAudio(pcm, len);
					}

					// rời nhóm
					else if (message.equals("LEAVE_GROUP_SUCCESS")) {
						int gid = dis.readInt();
						ui.removeGroupFromList(gid);
						ui.addMessage("Bạn đã rời khỏi 1 nhóm ");
					} else if (message.equals("LEAVE_GROUP_FAILED")) {
						ui.addMessage("Rời nhóm thất bại.");
					} else if (message.equals("GROUP_MEMBER_LEFT")) {
						int gid = dis.readInt();
						String name = dis.readUTF();
						ui.addMessage(name + " đã rời khỏi một nhóm (ID: " + gid + ")");
					}

					else {
						// Bỏ qua các lệnh kỹ thuật, chỉ hiển thị tin nhắn người dùng thực
						if (!message.equals("HISTORY") && !message.equals("DM") && !message.equals("FILE")
								&& !message.equals("GROUP_MSG") && !message.equals("GROUP_HISTORY")
								&& !message.equals("GROUP_CREATED")) {
							ui.addMessage(message);
						}
					}

				}

			} catch (IOException e) {
				System.out.println(" Mất kết nối server.");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 👉 THÊM 2 HÀM NÀY

	private void ensureSpeaker() throws LineUnavailableException {
		if (speakerLine == null) {
			AudioFormat fmt = audioFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
			speakerLine = (SourceDataLine) AudioSystem.getLine(info);
			speakerLine.open(fmt, 4096);
			speakerLine.start();
		}
	}

	private void playAudio(byte[] data, int len) {
		try {
			ensureSpeaker();
			speakerLine.write(data, 0, len);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// gọi videoo
	public void startVideoStream(int toUserId, JLabel videoLabel) throws IOException {
		System.out.println("Bắt đầu quay video và gửi...");

		VideoCapture camera = new VideoCapture(0, Videoio.CAP_DSHOW);
		if (!camera.isOpened()) {
			System.out.println("Không mở được camera!");
			return;
		}

		Mat frame = new Mat();
		MatOfByte mob = new MatOfByte();

		while (videoCallRunning && camera.read(frame)) {
			if (frame.empty())
				continue;

			// 👉 NẾU ĐANG TẮT VIDEO THÌ BỎ QUA GỬI FRAME (có thể vẫn show preview)
			if (stopVideo) {
				try { Thread.sleep(33); } catch (InterruptedException e) { break; }
				continue;
			}

			Imgcodecs.imencode(".jpg", frame, mob);
			byte[] bytes = mob.toArray();

			synchronized (dos) {
				dos.writeUTF(Protocol.CMD_VIDEO_FRAME);
				dos.writeInt(bytes.length);
				dos.write(bytes);
				dos.flush();
			}

			// ✅ SỬA LỖI 3: Preview local (Kiểm tra kích thước Label để tránh lỗi chia cho 0)
			if (videoLabel != null && videoLabel.getWidth() > 0 && videoLabel.getHeight() > 0) {
				ImageIcon icon = new ImageIcon(new ImageIcon(bytes).getImage()
						.getScaledInstance(videoLabel.getWidth(), videoLabel.getHeight(), Image.SCALE_SMOOTH));
				SwingUtilities.invokeLater(() -> videoLabel.setIcon(icon));
			}

			try { Thread.sleep(33); } catch (InterruptedException e) { break; }
		}

		camera.release();
	}

	// nguyên thêm hàm tiện ích
	// Gửi yêu cầu gọi video
	public void requestVideoCall(int toUserId) throws IOException {
		dos.writeUTF(Protocol.CMD_VIDEO_CALL_REQUEST);
		dos.writeInt(toUserId);
		dos.flush();
	}

	// Kết thúc cuộc gọi (nút hangup hoặc nhận ENDED)
	private void endVideoCallLocalOnly() {
		videoCallRunning = false;
		audioRunning = false;
		muteAudio = false;
		stopVideo = false;

		// Giải phóng micro
		if (micLine != null) {
			micLine.stop();
			micLine.close();
			micLine = null;
		}

		// Giải phóng loa
		if (speakerLine != null) {
			speakerLine.drain();
			speakerLine.stop();
			speakerLine.close();
			speakerLine = null;
		}

		// ✅ Kiểm tra null bên trong invokeLater
		SwingUtilities.invokeLater(() -> {
			if (videoCallFrame != null) {
				videoCallFrame.dispose();
				videoCallFrame = null;
				localVideoLabel = null;
				remoteVideoLabel = null;
			}
		});
	}

	// Hàm này được gọi khi mình chủ động bấm "Kết thúc"
	private void endVideoCall() {
		videoCallRunning = false;
		try {
			if (currentVideoPartnerId != -1) {
				dos.writeUTF(Protocol.CMD_VIDEO_CALL_END);
				dos.writeInt(currentVideoPartnerId);
				dos.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		endVideoCallLocalOnly();
	}

	private void openVideoCallWindow() {
		if (videoCallFrame != null)
			return; // đang mở rồi

		videoCallFrame = new JFrame("Đang gọi video với " + currentVideoPartnerName);
		videoCallFrame.setSize(800, 600);
		videoCallFrame.setLocationRelativeTo(null);
		videoCallFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		localVideoLabel = new JLabel("Đang bật camera của bạn", JLabel.CENTER);
		remoteVideoLabel = new JLabel("Đang chờ video từ " + currentVideoPartnerName, JLabel.CENTER);

		JPanel videoPanel = new JPanel(new java.awt.GridLayout(1, 2));
		videoPanel.add(localVideoLabel);
		videoPanel.add(remoteVideoLabel);

		// 👉 Tạo 3 nút điều khiển
		JButton btnMute = new JButton("Tắt tiếng");
		JButton btnToggleVideo = new JButton("Tắt video");
		JButton btnHangup = new JButton("Kết thúc");

		// 👉 Xử lý tắt/bật tiếng (muteAudio ảnh hưởng tới startAudioCapture)
		btnMute.addActionListener(e -> {
			muteAudio = !muteAudio;
			btnMute.setText(muteAudio ? "Bật tiếng" : "Tắt tiếng");
		});

		// 👉 Xử lý tắt/bật video (stopVideo ảnh hưởng tới startVideoStream)
		btnToggleVideo.addActionListener(e -> {
			stopVideo = !stopVideo;
			btnToggleVideo.setText(stopVideo ? "Bật video" : "Tắt video");
		});

		// 👉 Kết thúc cuộc gọi
		btnHangup.addActionListener(e -> endVideoCall());

		// 👉 Panel chứa 3 nút
		JPanel controlPanel = new JPanel(new java.awt.FlowLayout());
		controlPanel.add(btnMute);
		controlPanel.add(btnToggleVideo);
		controlPanel.add(btnHangup);

		// Thêm vào frame
		videoCallFrame.getContentPane().add(videoPanel, java.awt.BorderLayout.CENTER);
		videoCallFrame.getContentPane().add(controlPanel, java.awt.BorderLayout.SOUTH);

		videoCallFrame.setVisible(true);
		// reset trạng thái mỗi lần mở cửa sổ call
		muteAudio = false;
		stopVideo = false;

		// Bật flag và bắt đầu gửi video + audio của mình
		videoCallRunning = true;

		// 👉 GỌI BẮT MICRO GỬI AUDIO
		startAudioCapture();

		new Thread(() -> {
			try {
				startVideoStream(currentVideoPartnerId, localVideoLabel);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}).start();

	}

	public void requestHistory(int toUserId) throws IOException {
		dos.writeUTF("GET_HISTORY");
		dos.writeInt(toUserId);
		dos.flush();
	}

	private void receiveFile() {
		try {
			String fileName = dis.readUTF();
			long fileSize = dis.readLong();

			// 1. Chọn thư mục thật sự tồn tại, ví dụ:
			File dir = new File("D:/downloads/nguyen");   // sửa đúng user của bạn
			if (!dir.exists()) {
				dir.mkdirs();          // tự tạo thư mục nếu chưa có
			}

			File file = new File(dir, fileName);
			FileOutputStream fos = new FileOutputStream(file);

			byte[] buffer = new byte[4096];
			int bytesRead;
			long remaining = fileSize;

			while (remaining > 0 &&
					(bytesRead = dis.read(buffer, 0,
							(int) Math.min(buffer.length, remaining))) != -1) {
				fos.write(buffer, 0, bytesRead);
				remaining -= bytesRead;
			}
			fos.close();

			ui.addFileMessage(file);
			System.out.println("Đã nhận file: " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(String msg) throws IOException {
		if (msg != null && !msg.isEmpty()) {
			dos.writeUTF(msg);
			dos.flush();
		}
	}

	public void sendDirectMessage(int toUserId, String msg) throws IOException {
		if (msg != null && !msg.isEmpty()) {
			dos.writeUTF("DM"); // loại gói
			dos.writeInt(toUserId); // người nhận
			dos.writeUTF(msg); // nội dung
			dos.flush();
		}
	}

	public void sendDirectFile(int userId, File file) throws IOException {
		if (file != null && file.exists()) {
			// Gửi lệnh chỉ định người nhận
			dos.writeUTF("SEND_FILE");
			dos.writeInt(userId);
			dos.writeUTF(file.getName());
			dos.writeLong(file.length());

			// Gửi dữ liệu file
			try (FileInputStream fis = new FileInputStream(file)) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = fis.read(buffer)) != -1) {
					dos.write(buffer, 0, bytesRead);
				}
			}
			dos.flush();
			System.out.println("Đã gửi file riêng cho user ID = " + userId);
		}
	}

	public void leaveGroup(int groupId) throws IOException {
		dos.writeUTF("LEAVE_GROUP");
		dos.writeInt(groupId);
		dos.flush();
	}

	public void sendGroupFile(int groupId, File file) throws IOException {
		if (file != null && file.exists()) {
			// Gửi lệnh chỉ định nhóm nhận
			dos.writeUTF("SEND_GROUP_FILE");
			dos.writeInt(groupId);
			dos.writeUTF(file.getName());
			dos.writeLong(file.length());

			try (FileInputStream fis = new FileInputStream(file)) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = fis.read(buffer)) != -1) {
					dos.write(buffer, 0, bytesRead);
				}
			}
			dos.flush();
			System.out.println("Đã gửi file đến nhóm ID = " + groupId);
		}
	}

	public void sendCreateGroup(String name, java.util.List<model.Friend> members) throws IOException {
		dos.writeUTF("CREATE_GROUP");
		dos.writeUTF(name);
		dos.writeInt(members.size());
		for (model.Friend f : members)
			dos.writeInt(f.getId());
		dos.flush();
	}

	public void requestGroupHistory(int groupId) throws IOException {
		dos.writeUTF("GET_GROUP_HISTORY");
		dos.writeInt(groupId);
		dos.flush();
	}

	public void sendGroupMessage(int groupId, String msg) throws IOException {
		dos.writeUTF("GROUP_MSG");
		dos.writeInt(groupId);
		dos.writeUTF(msg);
		dos.flush();
	}

	public void sendFile(File file) throws IOException {
		if (file != null && file.exists()) {
			FileInputStream fis = new FileInputStream(file);

			dos.writeUTF("FILE");
			dos.writeUTF(file.getName());
			dos.writeLong(file.length());

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1) {
				dos.write(buffer, 0, bytesRead);
			}
			dos.flush();
			fis.close();

			System.out.println("File đã được gửi thành công!");
		}
	}

	public void sendBroadcastMessage(String msg) throws IOException {
		if (msg != null && !msg.isEmpty()) {
			dos.writeUTF(msg);
			dos.flush();
		}
	}

	// 👉 THÊM HÀM NÀY cho audio
	private AudioFormat audioFormat() {
		float sampleRate = 16000f;      // 16 kHz
		int sampleSizeInBits = 16;      // 16-bit
		int channels = 1;               // mono
		boolean signed = true;
		boolean bigEndian = false;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

	// 👉 THÊM HÀM NÀY
	private void startAudioCapture() {
		try {
			AudioFormat fmt = audioFormat();
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
			micLine = (TargetDataLine) AudioSystem.getLine(info);
			micLine.open(fmt, 4096);
			micLine.start();

			audioRunning = true;

			new Thread(() -> {
				byte[] buf = new byte[2048];
				while (audioRunning) {
					int n = micLine.read(buf, 0, buf.length);
					if (n > 0) {

						// 👉 NẾU ĐANG MUTE THÌ KHÔNG GỬI
						if (muteAudio) {
							continue;
						}

						synchronized (dos) {
							try {
								dos.writeUTF(Protocol.CMD_AUDIO_FRAME);
								dos.writeInt(n);
								dos.write(buf, 0, n);
								dos.flush();
							} catch (IOException e) {
								e.printStackTrace();
								break;
							}
						}
					}
				}

			}, "AudioCaptureThread").start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}