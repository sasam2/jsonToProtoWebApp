package webapp.assignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import generated.InfoOuterClass.InfoList;

/*
 * Servlet implementation class AppServletpro
 */
public class AppServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String pathToFile = "infoObject.proto";
	
	private static final File messageFile = new File(pathToFile);   
	
	private FileInputStream inputStream = null;
    private FileOutputStream outputStream = null;
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
				
				//close and reset fileStreams. reset file
				if(inputStream != null) {
					System.out.println("Timeout with file inputStream open. Closing.");
					inputStream.close();
				}
				if(outputStream != null) {
					System.out.println("Timeout with file outputstream open. Closing and reseting file.");
					outputStream.close();
					(new FileOutputStream(messageFile)).close();
					outputStream=null;
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
				response.getWriter().append("\nProto file size: "+messageFile.length()+" bytes.");
			}
		});
        AsyncContext finalAsyncContext = asyncContext;
        System.out.println("Launching async file write.");
        System.out.println("Absolute path to file: "+messageFile.getAbsolutePath()+".");
        finalAsyncContext.start(new Runnable() {
            @Override
            public void run () {
            		
    			addObjectToFile: {
            		InfoOuterClass.InfoList.Builder infoListBuilder = InfoOuterClass.InfoList.newBuilder();
    			
    				synchronized(messageFile) {
            			System.out.println("Thread "+Thread.currentThread().getId()+" entered sync block.");
                    
	    				//READ object list from file
	    				try {
	        				inputStream = new FileInputStream(messageFile);
	    					infoListBuilder.mergeFrom(inputStream); 
	    					inputStream.close();
	    					inputStream=null;
	    				} catch (FileNotFoundException e1) {
	    					System.out.println("Error opening the file."); //will assume file doesnt exist and this is the first enty	    					e1.printStackTrace();
	    				} catch (IOException e) {
	    					System.out.println("Error reading file from disk.");
	    					e.printStackTrace();
	    					break addObjectToFile;
	    				}
	            		infoListBuilder.addList(protoObject);
	            		InfoList infoList = infoListBuilder.build();
						System.out.println("Object list has now "+infoList.getListCount()+" entries.");
	        			
	    				//WRITE object list to file
						try {
	        				outputStream = new FileOutputStream(messageFile);
	    					infoList.writeTo(outputStream);
	    					outputStream.close();
	    					outputStream=null;
	        			} catch(IOException e){
	            	    	System.out.println("Exception writting to file.");
	            	    	e.printStackTrace();
	            	    	break addObjectToFile;
	            	  	}
						System.out.println("Saved "+infoList.getListCount()+" object list entries to file. File size: "+messageFile.length()+".");
    				}
					asyncContext.complete();
    			}
			}
            
        });
	}
}
