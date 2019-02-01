package webapp.assignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import org.json.JSONObject;
import generated.InfoOuterClass;

/*
 * Servlet implementation class AppServletpro
 */
public class AppServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
	private final File outputFile;
    public AppServlet() {
        super();
        // TODO Auto-generated constructor stub
        outputFile = new File("info.proto");
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse resnullponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Read json
		BufferedReader bufferedReader = request.getReader();
	    StringBuilder stringBuilder = new StringBuilder();
        char[] charBuffer = new char[128];
        int bytesRead = -1;
        while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
            stringBuilder.append(charBuffer, 0, bytesRead);
        }
		String reqStr = stringBuilder.toString();
		
		System.out.println("Json request: "+reqStr);
		response.getWriter().append("Json request: "+reqStr+"\n");
		
		JSONObject jsonObject = new JSONObject(reqStr);
		
		InfoOuterClass.Info.Builder builder = InfoOuterClass.Info.newBuilder();
		System.out.println("Json itr ");
		
		Iterator<String> keyItr = jsonObject.keys();
		while(keyItr.hasNext()) {
			String key = keyItr.next();
			Object valObj = jsonObject.get(key);
			
			if("name".equals(key) && valObj instanceof String)
				builder.setName((String)valObj);
			else if("id".equals(key) && valObj instanceof Integer)
				builder.setId((Integer)valObj);
			else
				throw new IllegalArgumentException("Unexpected value type for key "+key);
			
		}
		System.out.println("Info protoObject ");
		InfoOuterClass.Info protoObject = builder.build();
		
		FileOutputStream outputStream = new FileOutputStream(outputFile);
		
		System.out.println("AsyncContext ");
        AsyncContext asyncContext = request.startAsync();
       
        asyncContext.setTimeout(500);
        System.out.println("AsyncContext timeout "+asyncContext.getTimeout());
        
        asyncContext.addListener(new AsyncListener() {
			
			@Override
			public void onTimeout(AsyncEvent event) throws IOException {
				System.out.println("Proto write timeout.");
				
				//reset
				outputStream.close();
	    	    (new FileOutputStream(outputFile)).close();
				
    	    	ServletResponse response = event.getAsyncContext().getResponse();
				response.getWriter().append("Proto timeout \n");
			}
			
			@Override
			public void onStartAsync(AsyncEvent event) throws IOException {
				System.out.println("Proto write start task.");
			}
			
			@Override
			public void onError(AsyncEvent event) throws IOException {
				System.out.println("Proto write exception.");
				ServletResponse response = event.getAsyncContext().getResponse();
				response.getWriter().append("Proto error: "+event.getThrowable().getMessage()+"\n");
			}
			
			@Override
			public void onComplete(AsyncEvent event) throws IOException {
				System.out.println("Proto write complete.");
				ServletResponse response = event.getAsyncContext().getResponse();
				System.out.println("Proto response: "+protoObject);
				response.getWriter().append("Proto request: "+protoObject+"\n");				
			}
		});
        AsyncContext finalAsyncContext = asyncContext;
        System.out.println("Launching async file write.");
        finalAsyncContext.start(new Runnable() {
            @Override
            public void run () {
        		synchronized(outputFile) {
    				try {
        				protoObject.writeTo(outputStream);
        				outputStream.close();
        			} catch(IOException e){
            	    	System.out.println("Exception writting to file.");
            	    	System.out.println(e);
            	  	}
    			}
        		asyncContext.complete();
            }
        });
	}
}
