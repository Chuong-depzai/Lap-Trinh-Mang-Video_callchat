package controller;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

import common.Protocol;



public class RegisterController {

    private static final String SERVER_HOST = "localhost"; // địa chỉ server của bạn
    private static final int SERVER_PORT = 2209;

    public static boolean registerUser( String username, String password,
                                        String phone, String gender) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // Gửi lệnh "REGISTER" sang server
            dos.writeUTF(Protocol.CMD_REGISTER);
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.writeUTF(phone);
            dos.writeUTF(gender);
            dos.writeUTF(LocalDateTime.now().toString()); // Thời gian tạo
            dos.flush();

            // Nhận phản hồi
            String resp = dis.readUTF();
            if (Protocol.RESP_REGISTER_OK.equals(resp)) {
                return true;
            } else {
                System.err.println("Phản hồi từ server: " + resp);
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
