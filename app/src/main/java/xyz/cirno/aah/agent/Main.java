package xyz.cirno.aah.agent;

import android.net.LocalServerSocket;
import android.net.LocalSocket;

import java.nio.channels.Channels;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: aah-agent <socket-name>");
            System.out.println("  <socket-name> is the name of the abstract socket to listen on");
            System.exit(1);
        }
        try (LocalServerSocket s = new LocalServerSocket(args[0])) {
            System.out.printf("bootstrap connection: listening on AF_UNIX abstract: %s\n", args[0]);
            LocalSocket conn = s.accept();
            System.out.println("bootstrap connection accepted");
            ControlConnection.newBootstrapConnection(Channels.newChannel(conn.getInputStream()), Channels.newChannel(conn.getOutputStream())).run();
            // the bootstrap connection is closed, wait for sub-connections to finish
            ConnectionManager.blockingShutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
