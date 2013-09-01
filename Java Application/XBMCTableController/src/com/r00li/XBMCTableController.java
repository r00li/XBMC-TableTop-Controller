/*
 * XBMC TableTop Controller version 1.0
 * http://www.r00li.com
 * Copyright 2013 Andrej Rolih
 * Licensed under GPLv3 - see LICENSE.txt
 */

package com.r00li;

// The Client sessions package
import com.thetransactioncompany.jsonrpc2.client.*;

// The Base package for representing JSON-RPC 2.0 messages
import com.thetransactioncompany.jsonrpc2.*;

// The JSON Smart package for JSON encoding/decoding (optional)
import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minidev.json.*;

// For creating URLs
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.JOptionPane;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

class MyConfigurator implements ConnectionConfigurator {

    public void configure(java.net.HttpURLConnection connection) {
        String basicAuth = "Basic " + new String(new sun.misc.BASE64Encoder().encode(connection.getURL().getUserInfo().getBytes()));
        connection.setRequestProperty("Authorization", basicAuth);
    }
}

/**
 *
 * @author roli
 */
class Controller {

    JSONRPC2Session session;
    int requestID;
    long activePlayer;
    long paused;
    String playingTitle;
    String currTime;
    String endTime;
    long percentage;

    boolean working = false;
    String errorMessage = "";

    public String adress = "localhost:8080";
    public String username = "xbmc";
    public String password = "";


    private class ControlException extends Exception {

        public String shortMessage;

        ControlException(String message, String shortMessage) {
            super(message);
            this.shortMessage = shortMessage;
            working = false;
            errorMessage = shortMessage;
            System.out.println("Controller error: " + message);
        }

        public String getShortMessage() {
            return shortMessage;
        }
    }

    public void newConnection() throws ControlException {

        requestID = 0;
        activePlayer = -1;

        // The JSON-RPC 2.0 server URL
        URL serverURL = null;
        try {
            serverURL = new URL("http://" + username + ":" + password +"@" + adress + "/jsonrpc");
        } catch (MalformedURLException e) {
            //e.printStackTrace();
            throw new ControlException(e.getMessage(), "Wrong URL");
        }
        try {
            session = new JSONRPC2Session(serverURL);
            session.setConnectionConfigurator(new MyConfigurator());
            checkConnection();
            getPlayerStatus();
            working = true;
        } catch (ControlException e) {
            working = false;
            errorMessage = e.getShortMessage();
        }
    }

    public void checkConnection() throws ControlException {
        requestID++;
        Map params = new HashMap();
        ArrayList props = new ArrayList();
        props.add("version");
        params.put("properties", props);
        JSONRPC2Request request = new JSONRPC2Request("Application.GetProperties", params, requestID);
        JSONRPC2Response response = null;

        try {
            response = session.send(request);
        } catch (JSONRPC2SessionException e) {
            throw new ControlException(e.getMessage(), "Unable to connect");
        }

        if (response.indicatesSuccess()) {
            working = true;
            return;
        }
        else {
            //System.out.print(response.getError());
            throw new ControlException(response.getError().toString(), "Wrong response");
        }
    }

    public void playerInput(String direction) throws ControlException {

        requestID++;
        JSONRPC2Request request = new JSONRPC2Request("Input." + direction, requestID);
        JSONRPC2Response response = null;

        try {
            response = session.send(request);
        } catch (JSONRPC2SessionException e) {
            throw new ControlException(e.getMessage(), "Unable to connect");
        }

        if (response.indicatesSuccess()) {
            working = true;
            return;
        }
        else {
            throw new ControlException(response.getError().toString(), "Error sending input");
        }
    }

    private void getActivePlayerId() throws ControlException{

        requestID++;
        JSONRPC2Request request = new JSONRPC2Request("Player.GetActivePlayers", requestID);
        // Send request
        JSONRPC2Response response = null;
        try {
            response = session.send(request);
        } catch (JSONRPC2SessionException e) {
            throw new ControlException(e.getMessage(), "Unable to connect");
        }

        if (response.indicatesSuccess()) {
            List a = (List) response.getResult();

            if (a.size() > 0) {
                Iterator iter = a.iterator();
                while (iter.hasNext()) {
                    HashMap player = (HashMap) iter.next();
                    if (((String) player.get("type")).equals("video") || ((String) player.get("type")).equals("audio")) {
                        activePlayer = new Long((Long) player.get("playerid"));
                        working = true;
                    }
                }
            }
            else {
                activePlayer = -1; //-1 indicates no active players or error retriving the list
            }
        }
        else {
            //System.out.println(response.getError().getMessage());
            throw new ControlException(response.getError().toString(), "Error prcoessing data");
        }
    }

    private void getPlayingTitle() throws ControlException {
        getActivePlayerId();

        if (activePlayer == -1) {
            return;
        }

        requestID++;
        Map params = new HashMap();
        ArrayList requested = new ArrayList();
        requested.add("title");
        params.put("properties", requested);
        params.put("playerid", activePlayer);
        JSONRPC2Request request = new JSONRPC2Request("Player.GetItem", params, requestID);
        JSONRPC2Response response = null;
        try {
            response = session.send(request);
        } catch (JSONRPC2SessionException e) {
            throw new ControlException(e.getMessage(), "Unable to connect");
        }

        if (response.indicatesSuccess()) {
            playingTitle = (String) ((HashMap) ((HashMap) response.getResult()).get("item")).get("label");
            working = true;
        }
        else {
            throw new ControlException(response.getError().toString(), "Error prcoessing data");
        }

    }

    public void getPlayerStatus() throws ControlException {
        getActivePlayerId();
        getPlayingTitle();

        if (activePlayer == -1) {
            return;
        }

        requestID++;
        Map params = new HashMap();
        ArrayList requested = new ArrayList();
        requested.add("percentage");
        requested.add("time");
        requested.add("totaltime");
        requested.add("speed");
        params.put("properties", requested);
        params.put("playerid", activePlayer);
        JSONRPC2Request request = new JSONRPC2Request("Player.GetProperties", params, requestID);
        JSONRPC2Response response = null;
        try {
            response = session.send(request);
        } catch (JSONRPC2SessionException e) {
            throw new ControlException(e.getMessage(), "Unable to connect");
        }

        if (response.indicatesSuccess()) {
            //System.out.print(response.getResult());
            long hours = new Long((Long) ((HashMap) ((HashMap) response.getResult()).get("time")).get("hours"));
            long minutes = new Long((Long) ((HashMap) ((HashMap) response.getResult()).get("time")).get("minutes"));
            long seconds = new Long((Long) ((HashMap) ((HashMap) response.getResult()).get("time")).get("seconds"));

            long totalhours = new Long((Long) ((HashMap) ((HashMap) response.getResult()).get("totaltime")).get("hours"));
            long totalminutes = new Long((Long) ((HashMap) ((HashMap) response.getResult()).get("totaltime")).get("minutes"));
            long totalseconds = new Long((Long) ((HashMap) ((HashMap) response.getResult()).get("totaltime")).get("seconds"));

            paused = new Long((Long) ((HashMap) response.getResult()).get("speed"));
            percentage = ((Number) ((HashMap) response.getResult()).get("percentage")).longValue();

            if (hours == 0) {
                currTime = String.format("%02d:%02d  >", minutes, seconds);
            }
            else {
                currTime = String.format("%01d:%02d:%02d>", hours, minutes, seconds);
            }

            if (totalhours == 0) {
                endTime = String.format("<  %02d:%02d", totalminutes, totalseconds);
            }
            else {
                endTime = String.format("<%01d:%02d:%02d", totalhours, totalminutes, totalseconds);
            }

            working = true;
        }
        else {
            throw new ControlException(response.getError().toString(), "Error prcoessing data");
        }

    }

    public void controlPlayback(String control) throws ControlException {
        getPlayerStatus();

        if (activePlayer == -1) {
            return;
        }

        Map params = new HashMap();
        params.put("playerid", activePlayer);

        JSONRPC2Request request;
        if (control.equals("Pause")) {
            request = new JSONRPC2Request("Player.PlayPause", params, requestID);
        }
        else {
            request = new JSONRPC2Request("Player.Stop", params, requestID);
        }

        JSONRPC2Response response = null;
        try {
            response = session.send(request);
        } catch (JSONRPC2SessionException e) {
            throw new ControlException(e.getMessage(), "Unable to connect");
        }

        if (response.indicatesSuccess()) {
            working = true;
            return;
        }
        else {
            throw new ControlException(response.getError().toString(), "Error prcoessing data");
        }

    }
}

public class XBMCTableController {

    static SerialPort serialPort;
    static String buffer = "";
    static Controller control;
    static String port = "";

    static class SerialPortReader implements SerialPortEventListener {

        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR()) {//If data is available
                try {
                    buffer = buffer + serialPort.readString(1);
                    if (buffer.charAt(buffer.length() - 1) == '\0') {
                        messageRecv();
                    }
                } catch (SerialPortException ex) {
                    //System.out.println(ex);
                }
            }
        }
    }

    public static void messageRecv() {
        try {
            if (buffer.contains("B4")) {
                control.playerInput("Up");
            }
            else if (buffer.contains("B2")) {
                control.playerInput("Down");
            }
            else if (buffer.contains("B1")) {
                control.playerInput("Left");
            }
            else if (buffer.contains("B3")) {
                control.playerInput("Right");
            }
            else if (buffer.contains("B5")) {
                if (control.activePlayer != -1) {
                    control.controlPlayback("Pause");
                }
                else {
                    control.playerInput("Select");
                }
            }
            else if (buffer.contains("B6")) {
                control.playerInput("Back");
            }
            else if (buffer.contains("B7")) {
                if (control.activePlayer != -1) {
                    control.controlPlayback("Stop");
                }
            }
            else if (buffer.contains("B8")) {
            }
            else if (buffer.contains("B9")) {
            }
            else if (buffer.contains("B10")) {
            }
            else if (buffer.contains("B11")) {
            }
            buffer = "";
        } catch (Exception ex) {
            //Exception has been already handled and printed to screen. Nothing to do here
        }
    }



    public static void createGUI() {
        final TrayIcon trayIcon;

        if (SystemTray.isSupported()) {

            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage("icon.png");
            final PopupMenu popup = new PopupMenu();

            trayIcon = new TrayIcon(image, "XBMC Controller");

            trayIcon.setImageAutoSize(true);

            // Create a pop-up menu components
            MenuItem URLItem = new MenuItem("XBMC IP/URL");
            MenuItem usernameItem = new MenuItem("XBMC username");
            MenuItem passwordItem = new MenuItem("XBMC password");
            Menu portsMenu = new Menu("Serial port");
            MenuItem exitItem = new MenuItem("Exit");

            ItemListener portSelectListener = new ItemListener() {

                public void itemStateChanged(ItemEvent ie) {
                    int id = ie.getStateChange();

                    if (id == ItemEvent.SELECTED) {
                        port = ie.getItem().toString();
                        saveSettings();
                    }
                }

            };

            String[] portNames = SerialPortList.getPortNames();
            for (int i = 0; i < portNames.length; i++) {
                CheckboxMenuItem serialItem = new CheckboxMenuItem(portNames[i]);
                serialItem.addItemListener(portSelectListener);
                if (portNames[i].equals(port))
                    serialItem.setState(true);
                portsMenu.add(serialItem);
            }

            popup.add(URLItem);
            popup.add(usernameItem);
            popup.add(passwordItem);
            popup.addSeparator();
            popup.add(portsMenu);
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);


            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }



            URLItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String input = JOptionPane.showInputDialog(null, "Enter the IP/URL for the XBMC host and port (default: 8080).\nWITHOUT leading http://.\n\nExample: 192.168.1.2:8080", control.adress);

                    if (input != null) {
                        control.adress = input;
                        saveSettings();
                        JOptionPane.showMessageDialog(null, "Settings saved. You may need to restart this application for changes to take effect.");
                    }
                }
            });

            usernameItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String input = JOptionPane.showInputDialog(null, "Enter the XBMC username (default: xbmc)", control.username);

                    if (input != null) {
                        control.username = input;
                        saveSettings();
                        JOptionPane.showMessageDialog(null, "Settings saved. You may need to restart this application for changes to take effect.");
                    }
                }
            });

            passwordItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String input = JOptionPane.showInputDialog(null, "Enter the XBMC password", control.password);

                    if (input != null) {
                        control.password = input;
                        saveSettings();
                        JOptionPane.showMessageDialog(null, "Settings saved. You may need to restart this application for changes to take effect.");
                    }
                }
            });

            exitItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });

        }
        else {

            System.out.println("Tray is not supported");
            //  System Tray is not supported

        }
    }


    public static void loadSettings() {
        Properties loadProps = new Properties();
        try {
            loadProps.loadFromXML(new FileInputStream("XBMCController_settings.xml"));

            control.adress = loadProps.getProperty("URL");
            control.username = loadProps.getProperty("username");
            control.password = loadProps.getProperty("password");
            port = loadProps.getProperty("port");

        } catch (Exception ex) {
            System.out.println("Controller error: Could not find settings file. Creating a new file...");
            saveSettings();
        }
    }



    public static void saveSettings() {
        try {
            Properties saveProps = new Properties();
            saveProps.setProperty("URL", control.adress);
            saveProps.setProperty("username", control.username);
            saveProps.setProperty("password", control.password);
            saveProps.setProperty("port", port);
            saveProps.storeToXML(new FileOutputStream("XBMCController_settings.xml"), "");
        } catch (IOException ex) {
            System.out.println("Controller error: Could not save settings...\n" + ex.getMessage());
        }
    }


    public static void main(String[] args) {

        //Create a new XBMC controller
        control = new Controller();

        loadSettings();
        createGUI();

        serialPort = new SerialPort(port);
        try {
            serialPort.openPort();//Open serial port
            serialPort.setParams(SerialPort.BAUDRATE_9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);//Set params. Also you can set params by this string: serialPort.setParams(9600, 8, 1, 0);
            int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;//Prepare mask
            serialPort.setEventsMask(mask);//Set mask
            serialPort.addEventListener(new SerialPortReader());//Add SerialPortEventListener
        } catch (SerialPortException ex) {
            System.out.println("Controller serial port error: " + ex);
        }

        //Try to do the first connection
        try {
            control.newConnection();
        }
        catch (Exception ex) {
            //Error has already been printed by the control exception. Nothing to do here
        }

        //Check playback status indefinitely
        while (true) {

            //Something is wrong, controller isn't working. Write the error message to the lcd, and try to reconnect after 20 seconds
            if (!control.working) {
                try {
                    serialPort.writeBytes(("B" + "" + control.errorMessage + '\0').getBytes());
                    Thread.sleep(20000);
                    control.newConnection();
                }
                catch (Exception e) {
                    if (e.getClass() == SerialPortException.class) {
                        System.out.print(e.getMessage()); //There was an error writing the error message to the LCD
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }

            //Try to get the player status, write the correct data to the LCD
            try {
                control.getPlayerStatus();
                
                if (control.activePlayer != -1) { //Player is active, send the data to the LCD
                    serialPort.writeBytes(("P" + control.percentage + '\0').getBytes());
                    serialPort.writeBytes(("L" + control.currTime + '\0').getBytes());
                    serialPort.writeBytes(("R" + control.endTime + '\0').getBytes());
                    serialPort.writeBytes(("S" + control.paused + " " + control.playingTitle.substring(0, Math.min(control.playingTitle.length(), 22)) + '\0').getBytes());
                }
                else if (control.activePlayer == -1 && control.working) { //Player is not active but is connected
                    Date current_date = new Date();
                    current_date.getTime();
                    SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                    serialPort.writeBytes(("B" + "    " + date.format(current_date) + '\0').getBytes());
                }

                Thread.sleep(1000);

            } catch (Exception ex) {
                if (ex.getClass() == InterruptedException.class || ex.getClass() == SerialPortException.class)
                    System.out.println("Controller serial port error: " + ex);
            }
        }
    }
}
