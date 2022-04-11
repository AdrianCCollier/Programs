package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 *
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.sound.sampled.Line;

public class WebWorker implements Runnable
{
	// used for tag replacement 
	Date currentDate;
    String dateTag;
	String serverTag;

    private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
        currentDate = new Date();
		dateTag = "<cs371date>";
		serverTag = "<cs371server>";

	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		
		String fileType = null;
        String htmlPage;
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
            // Read HTTP request and store into htmlPage
			htmlPage = readHTTPRequest(is);
			// Create new htmlPage File Object and store into file
			File file = new File(htmlPage);
            // read and recognize file type as html and write as HTTP header
			if (htmlPage.endsWith("html")) {
				fileType="html";
				writeHTTPHeader(os, file, "text/html");
			} // end if 
			    writeContent(os, fileType, file);

		    os.flush();
		    socket.close();
		} // end try 
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		} // end catch 
		System.err.println("Done handling connection.");
		return;
	} // end run method 

	/**
	 * Read the HTTP request header.
	 **/
	private String readHTTPRequest(InputStream is)
	{
		String line;
		String currParse = null;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true) {
			
			try {
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
                // if the GET request refers to an existing filename, deliver content of file as response content back to browser 
				if (line.contains("GET")) {
					currParse = line.substring(5);
					if (currParse.contains(" "))
					currParse = currParse.substring(0, currParse.indexOf(" "));
					System.out.println("\n*" + currParse + "*\n");
				}

				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;
			}
			catch (Exception e) {
				System.err.println("Request error: " + e);
				break;
			}
		}

		return currParse;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 *
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, File file, String contentType) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		// if file is found, deliver contents
		if (file.exists()) {
			os.write("HTTP/1.1 200 OK\n".getBytes());
			os.write("Date: ".getBytes());
	    	os.write((df.format(d)).getBytes());
			os.write("\n".getBytes());
			os.write("Server: Adrian's very own server\n".getBytes());
			// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
			// os.write("Content-Length: 438\n".getBytes());
			os.write("Connection: close\n".getBytes());
			os.write("Content-Type: ".getBytes());
			os.write(contentType.getBytes());
			os.write("\n\n".getBytes()); 
			return;
			} // end if 
		// otherwise, output 404 message
		else
			os.write("HTTP/1.1 404 Not Found\n".getBytes());
			return;
		
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 *
	 * @param os, fileType, file
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, String fileType, File file) throws Exception
	{
		// New DateFormat object initialized to current date
		DateFormat dateF = DateFormat.getDateTimeInstance();
		dateF.setTimeZone(TimeZone.getTimeZone("GMT"));

		
		if (fileType == "html") {
			if (file.exists() && !file.isDirectory()) {
				BufferedReader input = new BufferedReader(new FileReader(file));
				try {
					String readLine;
					while ((readLine = input.readLine()) != null) {

						// tag replace IF's
						if(readLine.contains(serverTag))
						   readLine = readLine.replace(serverTag, "Adrian's very OK Server");

						if(readLine.contains(dateTag))
						   readLine = readLine.replace(dateTag, (dateF.format(currentDate)));

						os.write(readLine.getBytes());
					}
					input.close();
				}
				catch (FileNotFoundException e) {
					e.printStackTrace();
				} // end catch 
			} // end inner if
		} // end outer if  

		

		// 404 not found
		else {
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>404 Not Found!</h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
		} // end else 

	} // end writeContent method

} // end class
