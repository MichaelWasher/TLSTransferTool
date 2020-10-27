package com.michaelwasher.tftp;// -------- Basic com.michaelwasher.tftp.TFTP File Server ----------
//Name: Michael Washer
//
// ------------------------------------------

/*
 * This application is a simple file-transfer client that uses TLS to encrypt
 * the files before they are sent / decrypts them when they are received.
 */

import java.lang.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileHandler
{
	private static final Logger LOGGER = Logger.getLogger(FileServer.class.getName());


	//Returns boolean on whether file exists or not
	public static boolean fileExist(String filePath)
	{
		File f = new File(filePath);
		if(f.exists() && !f.isDirectory())
			return true;
		return false;
	}

	//Returns boolean on whether file exists or not
	public static boolean directoryExist(String filePath)
	{
		File f = new File(filePath);
		if(f.exists() && f.isDirectory())
			return true;
		return false;
	}

	//Takes all bytes from the InStream and sends them through the BufferedOutStream using a byte array.
	//Sleep timer has been added for makeshift slow connection.
	public static void copyFile(InputStream in, OutputStream out){//inspect for errors
		LOGGER.fine("Sending files.");
		try {
			int c;
			byte[] fixedByteArray = new byte[1024];
			while ((c = in.read(fixedByteArray)) != -1) {
				LOGGER.fine(String.format("Copied %d bytes.", c));
				out.write(fixedByteArray);
				out.flush();
			}
		} catch (IOException e){
			LOGGER.log(Level.SEVERE, "File copy failed: " + e.getMessage(), e );
		}
	} //copyFile()

}