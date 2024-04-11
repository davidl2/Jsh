
import java.sql.*;
import java.io.*;
import java.util.regex.*;


/**
   This class is a command-line client for an SQL database,
   similar to sqlplus or psql or isql or...

   @author David Lee Lambert
*/
public class Jsh {

   protected Connection con;
   protected LineNumberReader in;
   protected PrintWriter out;
   protected String user = "";
   protected String pass = "";
   protected String last_trace;
  
   private Jsh() {
      in = new LineNumberReader(new InputStreamReader(System.in));
      out = new PrintWriter(new OutputStreamWriter(System.out));
   }

   static final Pattern DRIVER 
      = Pattern.compile("DRIVER",Pattern.CASE_INSENSITIVE);
   static final Pattern OPEN 
      = Pattern.compile("OPEN",Pattern.CASE_INSENSITIVE);
   static final Pattern USER 
      = Pattern.compile("USER(NAME)?",Pattern.CASE_INSENSITIVE);
   static final Pattern PASS
      = Pattern.compile("PASS(WORD)?",Pattern.CASE_INSENSITIVE);
   static final Pattern TRACE
      = Pattern.compile("TRACE",Pattern.CASE_INSENSITIVE);
   static final Pattern QUIT
      = Pattern.compile("EXIT|QUIT|\\\\q",Pattern.CASE_INSENSITIVE);   
   static final Pattern commandWordPattern 
      = Pattern.compile("\\s*(\\\\?[A-Z]+)(\\s+(.*))?$",
			Pattern.CASE_INSENSITIVE);

   private String dashes(int n) { 
      StringBuffer sb = new StringBuffer(n);
      int i;
      for (i=0;i<n;i++) sb.append("-");
      return sb.toString();
   }
   private String spaces(int n) { 
      StringBuffer sb = new StringBuffer(n);
      int i;
      for (i=0;i<n;i++) sb.append(" ");
      return sb.toString();
   }

   
   String myReadLine() {
      try {
	 out.print(" > ");
	 out.flush();
	 String line = in.readLine();
	 if (line==null) return null;
	 StringBuffer real_line = new StringBuffer(line);
	 while (line.length()>0 && line.charAt(line.length()-1)=='\\') {
	    real_line.setLength(real_line.length()-1);
	    out.println("+> ");
	    out.flush();
	    line = in.readLine();
	    if (line==null) break;
	    real_line.append(line);
	 }
	 return real_line.toString();
      } catch (IOException e) {
	 System.err.println("  [IO exception]  ");
	 return null;
      }
   }
	 

   private void doInputLoop() {
      String line;
      while (null!=(line=myReadLine())) {
	 Matcher m1 = commandWordPattern.matcher(line);
	 if (! m1.matches()) {
	    if (line.matches(".*\\S.*"))
	       out.println("[no command-word found]");
	    continue;
	 }
	 String cmd = m1.group(1);
	 String arg = m1.group(3);
	 try {
	    if (DRIVER.matcher(cmd).matches()) {
	       Class.forName(arg);
	       out.println("OK.");
	    } else if (OPEN.matcher(cmd).matches()) {
	       out.println("user='"+user+"'");
	       out.println("pass='"+pass+"'");
	       out.println("url='"+arg+"'");
	       con = DriverManager.getConnection(arg,user,pass);
	       out.println("OK.");
	    } else if (USER.matcher(cmd).matches()) {
	       user=arg;
	       out.println("OK.");
	    } else if (PASS.matcher(cmd).matches()) {
	       pass=arg;
	       out.println("OK.");
	    } else if (TRACE.matcher(cmd).matches()) {
	       out.print(last_trace);
	    } else if (QUIT.matcher(cmd).matches()) {
	       break;
	    } else {
	       if (con!=null) {
		  Statement stmt = con.createStatement();
		  if (stmt.execute(line)) {
		     ResultSet rs = stmt.getResultSet();
		     ResultSetMetaData rsmd = rs.getMetaData();
		     int[] sizes = new int[rsmd.getColumnCount()];
		     String[] names = new String[sizes.length];
		     int i,off;
			
		     StringBuffer hl1=new StringBuffer(),
			hl2=new StringBuffer();
		     for (i=0;i<sizes.length;i++) {
			names[i] = rsmd.getColumnLabel(i+1);
			sizes[i] = Math.max(names[i].length(),
					    rsmd.getColumnDisplaySize(i+1));
			StringBuffer buf1 
			   = new StringBuffer(spaces(sizes[i]));
			buf1.replace(0,names[i].length(),names[i]);
			hl1.append(buf1);
			hl1.append("\t");
			hl2.append(dashes(sizes[i]));
			hl2.append("\t");
		     }
		     hl1.setLength(Math.max(hl1.length()-1,0));
		     out.println(hl1);
	      	     hl2.setLength(Math.max(hl2.length()-1,0));
		     out.println(hl2);

		     while (rs.next()) {
			StringBuffer dl 
			   = new StringBuffer(spaces(hl1.length()));
			for (i=0,off=0;i<sizes.length;off += sizes[i++]+1) {
			   if (off>0) {
			      dl.replace(off-1,off,"\t");
			   }
			   String datum = rs.getString(i+1);
			   if (datum!=null) 
			      dl.replace(off,off+datum.length(),datum);
			}
			out.println(dl);
		     }
		  } else {
		     out.println("Matched "+stmt.getUpdateCount()+" rows");
		  }
	       } else {
		  out.println("[no connection]");
	       }
	    }
	 } catch (Exception e) {
	    out.println("Got exception:  "+e.getMessage());
	    StringWriter buf = new StringWriter();
	    PrintWriter w = new PrintWriter(buf);
	    e.printStackTrace(w);
	    last_trace=buf.toString();
	 }  
      }
      out.println("");
      out.flush();
   }

   public static void main(String[] args) {
      (new Jsh()).doInputLoop();
   }
   
}
