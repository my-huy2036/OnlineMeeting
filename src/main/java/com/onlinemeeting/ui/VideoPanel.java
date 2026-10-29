package com.onlinemeeting.ui;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Video preview and prototype UDP transport.
 * A single JPEG per datagram is NOT a production-grade media transport:
 * large UDP datagrams are fragmented and can be lost across the Internet.
 * For production meetings, replace this transport with WebRTC/RTP + codec + congestion control.
 */
public class VideoPanel extends JPanel {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;
    private final String meetingCode;
    private final int userId;
    private final InetAddress serverAddress;
    private final int serverPort;
    private final DatagramSocket socket;
    private final JPanel videoGrid = new JPanel(new GridLayout(0, 2, 8, 8));
    private final JLabel localVideo = new JLabel("Camera đang tắt", SwingConstants.CENTER);
    private final Map<Integer, JLabel> remoteVideos = new ConcurrentHashMap<>();
    private final JButton cameraButton = new JButton("Bật camera");
    private volatile boolean cameraRunning;
    private volatile boolean cameraOpening;
    private volatile boolean closed;
    private OpenCVFrameGrabber grabber;
    private Thread captureThread;

    public VideoPanel(String host, int port, String meetingCode, int userId) throws IOException {
        this.meetingCode = meetingCode;
        this.userId = userId;
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
        this.socket = new DatagramSocket();
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        localVideo.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        localVideo.setOpaque(true);
        localVideo.setBackground(Color.DARK_GRAY);
        localVideo.setForeground(Color.WHITE);

        JPanel localTile = new JPanel(new BorderLayout(4, 4));
        localTile.add(new JLabel("Bạn", SwingConstants.CENTER), BorderLayout.NORTH);
        localTile.add(localVideo, BorderLayout.CENTER);
        videoGrid.add(localTile);

        JScrollPane scrollPane = new JScrollPane(videoGrid);
        add(scrollPane, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controls.add(cameraButton);
        add(controls, BorderLayout.SOUTH);
        cameraButton.addActionListener(e -> {
            if (cameraRunning) stopCamera();
            else startCamera();
        });

        Thread receiver = new Thread(this::receiveVideo, "video-receiver-" + userId);
        receiver.setDaemon(true);
        receiver.start();
    }

    private void startCamera() {
        if (cameraRunning || cameraOpening || closed) return;
        cameraOpening = true;
        cameraButton.setEnabled(false);
        cameraButton.setText("Đang mở...");
        localVideo.setText("Đang xin quyền / khởi động camera...");

        // Native device initialization can take seconds. Never do it on Swing EDT.
        new SwingWorker<OpenCVFrameGrabber, Void>() {
            @Override
            protected OpenCVFrameGrabber doInBackground() throws Exception {
                String indexText = System.getenv().getOrDefault("OM_CAMERA_INDEX", "0");
                int cameraIndex = Integer.parseInt(indexText);
                OpenCVFrameGrabber device = new OpenCVFrameGrabber(cameraIndex);
                device.setImageWidth(640);
                device.setImageHeight(480);
                try {
                    device.start();
                    return device;
                } catch (Exception | LinkageError ex) {
                    releaseDevice(device);
                    throw ex;
                }
            }

            @Override
            protected void done() {
                cameraOpening = false;
                cameraButton.setEnabled(!closed);
                try {
                    OpenCVFrameGrabber device = get();
                    if (closed) {
                        releaseDevice(device);
                        return;
                    }
                    grabber = device;
                    cameraRunning = true;
                    cameraButton.setText("Tắt camera");
                    localVideo.setText("Đang đọc hình ảnh...");
                    captureThread = new Thread(() -> captureAndSend(device), "video-capture-" + userId);
                    captureThread.setDaemon(true);
                    captureThread.start();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showCameraError(ex);
                } catch (ExecutionException ex) {
                    showCameraError(ex.getCause());
                }
            }
        }.execute();
    }

    private void showCameraError(Throwable error) {
        cameraRunning = false;
        cameraButton.setEnabled(!closed);
        cameraButton.setText("Bật camera");
        localVideo.setIcon(null);
        localVideo.setText("Không mở được camera");
        System.err.println("Camera initialization failed:");
        error.printStackTrace(System.err);
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String detail = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
        String guidance = System.getProperty("os.name", "").toLowerCase().contains("mac")
                ? "\n\nTrên macOS: kiểm tra System Settings > Privacy & Security > Camera;"
                + " cấp quyền cho IntelliJ/Terminal hoặc ứng dụng đã đóng gói."
                + "\nThoát Zoom/FaceTime nếu đang giữ camera. Kiểm tra Info.plist khi đóng gói .app."
                : "\n\nKiểm tra quyền camera và ứng dụng khác đang sử dụng webcam.";
        if (!closed) JOptionPane.showMessageDialog(this,
                "Không mở được camera: " + detail + guidance + "\nChi tiết lỗi đã ghi ra console.",
                "Camera", JOptionPane.ERROR_MESSAGE);
    }

    private void captureAndSend(OpenCVFrameGrabber device) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        try {
            while (cameraRunning && !closed) {
                Frame frame = device.grab();
                if (frame == null || frame.image == null) {
                    Thread.sleep(90);
                    continue;
                }
                BufferedImage source = converter.convert(frame);
                if (source == null) continue;
                BufferedImage resized = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = resized.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(source, 0, 0, WIDTH, HEIGHT, null);
                } finally {
                    graphics.dispose();
                }
                SwingUtilities.invokeLater(() -> {
                    if (cameraRunning && !closed) {
                        localVideo.setIcon(new ImageIcon(resized));
                        localVideo.setText("");
                    }
                });
                sendFrame(resized);
                Thread.sleep(90);
            }
        } catch (Exception | LinkageError e) {
            if (cameraRunning && !closed) {
                System.err.println("Video capture failed:");
                e.printStackTrace(System.err);
                SwingUtilities.invokeLater(() -> showCameraError(e));
            }
        } finally {
            releaseDevice(device);
            if (grabber == device) grabber = null;
            cameraRunning = false;
            SwingUtilities.invokeLater(() -> {
                if (!closed) {
                    cameraButton.setText("Bật camera");
                    cameraButton.setEnabled(true);
                    localVideo.setIcon(null);
                    localVideo.setText("Camera đang tắt");
                }
            });
        }
    }

    private void sendFrame(BufferedImage image) throws IOException {
        byte[] codeBytes = meetingCode.getBytes(StandardCharsets.US_ASCII);
        if (codeBytes.length < 6 || codeBytes.length > 30) return;
        byte[] jpeg = jpegBytes(image);
        if (jpeg.length + codeBytes.length + 9 > 60000) return;
        ByteArrayOutputStream packetBytes = new ByteArrayOutputStream(jpeg.length + codeBytes.length + 9);
        DataOutputStream output = new DataOutputStream(packetBytes);
        output.write(new byte[]{'O', 'M', 'V', '1'});
        output.writeByte(codeBytes.length);
        output.write(codeBytes);
        output.writeInt(userId);
        output.write(jpeg);
        byte[] data = packetBytes.toByteArray();
        socket.send(new DatagramPacket(data, data.length, serverAddress, serverPort));
    }

    private byte[] jpegBytes(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("Không tìm thấy JPEG encoder");
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ImageOutputStream output = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.55f);
            writer.write(null, new IIOImage(image, null, null), params);
            output.flush();
            return buffer.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private void receiveVideo() {
        byte[] buffer = new byte[65507];
        while (!closed) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                if (data.length < 10 || data[0] != 'O' || data[1] != 'M' || data[2] != 'V' || data[3] != '1') continue;
                int codeLength = data[4] & 0xff;
                if (codeLength < 6 || data.length <= 9 + codeLength) continue;
                String code = new String(data, 5, codeLength, StandardCharsets.US_ASCII);
                if (!meetingCode.equalsIgnoreCase(code)) continue;
                int remoteUserId = ByteBuffer.wrap(data, 5 + codeLength, 4).getInt();
                if (remoteUserId == userId) continue;
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(data, 9 + codeLength, data.length - 9 - codeLength));
                if (image != null) SwingUtilities.invokeLater(() -> displayRemoteVideo(remoteUserId, image));
            } catch (IOException e) {
                if (!closed) System.err.println("Video receive error: " + e.getMessage());
                break;
            }
        }
    }

    private void displayRemoteVideo(int remoteUserId, BufferedImage image) {
        JLabel label = remoteVideos.get(remoteUserId);
        if (label == null) {
            label = new JLabel("", SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(Color.DARK_GRAY);
            label.setForeground(Color.WHITE);
            label.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            JPanel tile = new JPanel(new BorderLayout(4, 4));
            tile.add(new JLabel("Thành viên " + remoteUserId, SwingConstants.CENTER), BorderLayout.NORTH);
            tile.add(label, BorderLayout.CENTER);
            remoteVideos.put(remoteUserId, label);
            videoGrid.add(tile);
            videoGrid.revalidate();
        }
        label.setIcon(new ImageIcon(image.getScaledInstance(WIDTH, HEIGHT, Image.SCALE_FAST)));
        label.setText("");
        videoGrid.repaint();
    }

    private static void releaseDevice(OpenCVFrameGrabber device) {
        try { device.release(); }
        catch (Exception ex) { System.err.println("Camera release error: " + ex.getMessage()); }
    }

    private void stopCamera() {
        cameraRunning = false;
        cameraButton.setText("Bật camera");
        localVideo.setIcon(null);
        localVideo.setText("Camera đang tắt");
        // Device is released by capture thread's finally (no EDT blocking).
    }

    public void closeMedia() {
        closed = true;
        stopCamera();
        socket.close();
    }
}
