package com.onlinemeeting.client;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameGrabber;

/** Camera smoke test independent of SQL Server or meeting login. */
public final class CameraDiagnostic {
    private CameraDiagnostic() {}

    public static void main(String[] args) throws Exception {
        int index = Integer.parseInt(System.getenv().getOrDefault("OM_CAMERA_INDEX", "0"));
        System.out.println("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Testing camera index " + index);
        OpenCVFrameGrabber camera = new OpenCVFrameGrabber(index);
        try {
            camera.start();
            Frame image = camera.grab();
            if (image == null || image.image == null) {
                System.err.println("Opened camera, but no image was returned.");
            } else {
                System.out.println("Camera OK: " + camera.getImageWidth() + "x" + camera.getImageHeight());
            }
        } catch (Throwable error) {
            System.err.println("Cannot initialize camera. Full stack trace:");
            error.printStackTrace(System.err);
            throw error;
        } finally {
            try { camera.release(); }
            catch (Exception e) { System.err.println("Camera close: " + e.getMessage()); }
        }
    }
}
