package webapp.assignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

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
	private static final String pathToFile = "/infoObject.proto";
	private static final File outputFile = new File(pathToFile);   
	
	private FileOutputStream outputStream = null; //
    /**
     * @see HttpServlet#HttpServlet()
     */
	
    public AppServlet() {
        super();
        // TODO Auto-generated constructor stub
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

		System.out.println("Info json ");
		BufferedReader bufferedReader = request.getReader();
	    StringBuilder stringBuilder = new StringBuilder();
        char[] charBuffer = new char[128];
        int bytesRead = -1;
        while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
            stringBuilder.append(charBuffer, 0, bytesRead);
        }
		String reqStr = stringBuilder.toString();
		JSONObject jsonObject = new JSONObject(reqStr);
		
		System.out.println("Json request: "+jsonObject);
		response.getWriter().append("\nJson request: \n"+jsonObject);		

		System.out.println("Info protoObject ");
		InfoOuterClass.Info.Builder builder = InfoOuterClass.Info.newBuilder();
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
		InfoOuterClass.Info protoObject = builder.build();
		
		System.out.println("Proto to write: "+protoObject);
		response.getWriter().append("\nProto to write: \n"+protoObject);
				
		
		System.out.println("AsyncContext - write to file ");
        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(500);
        System.out.println("AsyncContext timeout "+asyncContext.getTimeout());
        asyncContext.addListener(new AsyncListener() {
			
			@Override
			public void onTimeout(AsyncEvent event) throws IOException {
				System.out.println("Proto write timeout.");
				System.out.println("Current thread: "+Thread.currentThread().getId());
				
				//close and reset fileStream. reset file
				if(outputStream != null) {
					System.out.println("File outputstream seems open. Closing and reseting.");
					outputStream.close();
					(new FileOutputStream(outputFile)).close();
					outputStream=null;
				} else {
					System.out.println("Timeout but outputstream was not open. Doing nothing.");
				}				
    	    	ServletResponse response = event.getAsyncContext().getResponse();
    	    	((HttpServletResponse) response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().append("\nProto timeout ");
			}
			
			@Override
			public void onStartAsync(AsyncEvent event) throws IOException {
				System.out.println("Proto write start task.");
			}
			
			@Override
			public void onError(AsyncEvent event) throws IOException {
				System.out.println("Proto write exception.");
				ServletResponse response = event.getAsyncContext().getResponse();
				((HttpServletResponse) response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().append("\nProto error: "+event.getThrowable().getMessage());
			}
			
			@Override
			public void onComplete(AsyncEvent event) throws IOException {
				System.out.println("Proto write complete.");
				ServletResponse response = event.getAsyncContext().getResponse();
				response.getWriter().append("\nProto file size: "+outputFile.length()+" bytes.");
			}
		});
        AsyncContext finalAsyncContext = asyncContext;
        System.out.println("Launching async file write.");
        finalAsyncContext.start(new Runnable() {
            @Override
            public void run () {
        		synchronized(outputFile) {
        			System.out.println("Current thread runnable: "+Thread.currentThread().getId());
    				
        			try {
        				//open and init fileStream
    					outputStream = new FileOutputStream(outputFile);
    					
    					//write fileStream
    					try {
        					System.out.println("Here is a sleep");
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
    					protoObject.writeTo(outputStream);
    					    					
    					//close and reset fileStream
    					outputStream.close();
    					outputStream=null;
    					asyncContext.complete(); 
    					
        			} catch(IOException e){
            	    	System.out.println("Exception writting to file.");
            	    	e.printStackTrace();
            	  	}
    			}
            }
        });
	}
}
