package xlsrv.com.action;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
 
import javax.servlet.http.HttpServletResponse;
 
import org.apache.commons.io.IOUtils;


public class Exec {

//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		Exec obj = new Exec();
//		String command = "";
//		String output = obj.executeCommand(command, new File("F:/"));
//		System.out.println(output);
//	}
	
	public static String Run(String command, String file_dir) {
		// TODO Auto-generated method stub
		String output = Exec.executeCommand(command, new File(file_dir));
		return output;
	}
	
	public static String executeCommand(String command, File file_dir) {
		
		StringBuffer output = new StringBuffer();
		Process p;
		InputStreamReader inputStreamReader = null;
		BufferedReader reader = null;
		String err="";
		try {
			p = Runtime.getRuntime().exec(command, null, file_dir);
			p.waitFor();
			inputStreamReader = new InputStreamReader(p.getInputStream(), "GBK");
			reader = new BufferedReader(inputStreamReader);
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
				//output.append(line + "\r\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			err=e.toString();
		} catch (InterruptedException e) {
			e.printStackTrace();
			err=e.toString();
		} finally {
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(inputStreamReader);
		}
		if (err.isEmpty())
			return output.toString();
		else
			return err;
	}


}
