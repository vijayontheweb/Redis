import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import redis.clients.jedis.Jedis;

public class HelloRedis {

	public static void main(String[] args) {

		WebServer ws = new WebServer();
		ws.start();

	}
}

class WebServer {
	Map<String, String> voteSystem = new HashMap<String, String>();
	SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss a");

	/**
	 * WebServer constructor.
	 */
	protected void start() {

		ServerSocket s;

		System.out.println("Webserver starting up on port 1234");
		try {
			// create the main server socket
			s = new ServerSocket(1234);
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}

		System.out.println("Waiting for connection");
		for (;;) {
			try {
				String htmlContent = null;
				// wait for a connection
				Socket remote = s.accept();
				// remote is now the connected socket
				System.out.println("Connection, sending data.");
				BufferedReader in = new BufferedReader(new InputStreamReader(
						remote.getInputStream()));
				PrintWriter out = new PrintWriter(remote.getOutputStream());
				String encodedStr = in.readLine();
				String str = java.net.URLDecoder.decode(encodedStr, "UTF-8");
				String sub = str.substring(str.indexOf("?") + 1,
						str.length() - 9);
				StringTokenizer reqParams = new StringTokenizer(sub, "&");

				while (reqParams.hasMoreElements()) {
					String[] keyvaluepair = reqParams.nextElement().toString()
							.split("=");
					String key = keyvaluepair[0];
					String value = keyvaluepair[1];
					if ("user".equals(key)) {
						voteSystem.put(key, value);
					} else if ("post".equals(key)) {
						voteSystem.put(key, value);
						htmlContent = savePostOnJedis();
					} else if ("vote".equals(key)) {
						voteSystem.put(key, value);
						htmlContent = saveVoteOnJedis();
					}
				}
				out.println("HTTP/1.1 200 OK");
				out.println("Content-Type: text/html");
				out.println("Server: Bot");
				// this blank line signals the end of the headers
				out.println("");
				// Send the HTML page
				out.println(htmlContent);
				out.flush();
				remote.close();
			} catch (Exception e) {
				System.out.println("Error: " + e);
			}
		}
	}


	private String savePostOnJedis() {
		Jedis j = new Jedis("localhost", 6379);
		j.connect();
		System.out.println("Connected");

		try {
			j.hset("article:" + voteSystem.get("post"), "poster", "user:"
					+ voteSystem.get("user"));
			j.hset("article:" + voteSystem.get("post"), "time",
					formatter.format(new Date()));
			j.hset("article:" + voteSystem.get("post"), "vote", "0");

			j.sadd("user:" + voteSystem.get("user"),
					"article:" + voteSystem.get("post"));
			j.zadd("vote:", new Double(1), "article:" + voteSystem.get("post"));
			j.zadd("time:", new Double(System.currentTimeMillis()), "article:"
					+ voteSystem.get("post"));
			return composePostHTML(j);
		} catch (NumberFormatException e) {
			System.out.println("Number Format Exception has occured");
			e.printStackTrace();
			return "Number Format Exception has occured";
		} finally {
			j.disconnect();
			System.out.println("\nDisconnected");
		}
	}
	
	private String composePostHTML(Jedis j) throws NumberFormatException {
		StringBuffer sb = new StringBuffer();
		sb.append("By User:" + voteSystem.get("user")
				+ "<br><table border='1'>");
		Set<String> articles = j.smembers("user:" + voteSystem.get("user"));
		for (String article : articles) {
			sb.append("<tr><td>" + article.replaceAll("article:", "")
					+ "</td></tr>");
		}
		sb.append("</table><br>");
		return composeHTMLByTimeAndVote(j,sb);		
	}

	private String saveVoteOnJedis() {
		Jedis j = new Jedis("localhost", 6379);
		j.connect();
		System.out.println("Connected");
		try {
			if (!j.sismember("vote:" + voteSystem.get("vote"), "voter:"
					+ voteSystem.get("user"))) {
				j.hincrBy("article:" + voteSystem.get("vote"), "vote", 1);
				j.sadd("vote:" + voteSystem.get("vote"), "voter:"
						+ voteSystem.get("user"));				
				j.zincrby("vote:", 1 , "article:" + voteSystem.get("vote"));
			}
			return composeVoteHTML(j);
		} catch (NumberFormatException e) {
			System.out.println("Number Format Exception has occured");
			e.printStackTrace();
			return "Number Format Exception has occured";
		} finally {
			j.disconnect();
			System.out.println("\nDisconnected");
		}
	}
	
	
	private String composeVoteHTML(Jedis j) throws NumberFormatException {
		StringBuffer sb = new StringBuffer();
		sb.append("By Article:" + voteSystem.get("vote")
				+ "<br><table border='1'>");
		Set<String> voters = j.smembers("vote:" + voteSystem.get("vote"));
		for (String voter : voters) {
			sb.append("<tr><td>" + voter.replaceAll("voter:", "")
					+ "</td></tr>");
		}
		sb.append("</table><br>");
		return composeHTMLByTimeAndVote(j,sb);		
	}
	
	private String composeHTMLByTimeAndVote(Jedis j, StringBuffer sb){
		sb.append("By Time:<br><table border='1'>");
		Set<String> timeIds = j.zrevrange("time:", 0, -1);
		sb.append("<tr><th>Article</th><th>Poster</th><th>Vote</th><th>Time</th></tr>");
		for (String id : timeIds) {
			Map<String, String> articleData = j.hgetAll(id);
			sb.append("<tr>");
			sb.append("<td>" + id.replaceAll("article:", "") + "</td>");
			sb.append("<td>"
					+ articleData.get("poster").replaceAll("user:", "")
					+ "</td>");
			sb.append("<td>" + articleData.get("vote") + "</td>");
			sb.append("<td>" + articleData.get("time") + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");

		sb.append("<br>By Vote:<br><table border='1'>");
		Set<String> voteIds = j.zrevrange("vote:", 0, -1);
		sb.append("<tr><th>Article</th><th>Poster</th><th>Vote</th><th>Time</th></tr>");
		for (String id : voteIds) {
			Map<String, String> articleData = j.hgetAll(id);
			sb.append("<tr>");
			sb.append("<td>" + id.replaceAll("article:", "") + "</td>");
			sb.append("<td>"
					+ articleData.get("poster").replaceAll("user:", "")
					+ "</td>");
			sb.append("<td>" + articleData.get("vote") + "</td>");
			sb.append("<td>" + articleData.get("time") + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}

}