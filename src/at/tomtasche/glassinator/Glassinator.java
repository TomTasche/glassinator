package at.tomtasche.glassinator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.Handler;
import android.os.HandlerThread;

public class Glassinator {

	public static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	public static final String SCOPE = "https://www.googleapis.com/auth/glass.timeline https://www.googleapis.com/auth/userinfo.profile";

	private static final String TIMELINE_URL = "https://www.googleapis.com/mirror/v1/timeline";

	private static final String API_KEY = "YOUR_API_KEY";
	private static final String CLIENT_ID = "YOUR_CLIENT_ID";
	private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET";

	private String token;
	private HandlerThread handlerThread;
	private Handler handler;
	private Handler mainHandler;

	public Glassinator(String token, Handler mainHandler) {
		this.token = token;
		this.mainHandler = mainHandler;

		handlerThread = new HandlerThread("glassinator");
		handlerThread.start();

		handler = new Handler(handlerThread.getLooper());
	}

	public void getTimeline(final Callback callback) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					URL url = new URL(TIMELINE_URL + "?key=" + API_KEY);
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.addRequestProperty("client_id", CLIENT_ID);
					conn.addRequestProperty("client_secret", CLIENT_SECRET);
					conn.setRequestProperty("Authorization", "OAuth " + token);

					mainHandler.post(callback.setStatus(readResponse(conn)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void postTimelineCard(final String title, final String text,
			final Callback callback) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					URL url = new URL(TIMELINE_URL + "?key=" + API_KEY);
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.setRequestMethod("POST");
					conn.addRequestProperty("client_id", CLIENT_ID);
					conn.addRequestProperty("client_secret", CLIENT_SECRET);
					conn.setRequestProperty("Authorization", "OAuth " + token);
					conn.setRequestProperty("Content-Type", "application/json");

					String item = "{\"text\": \""
							+ title
							+ "\", \"speakableText\": \""
							+ text
							+ "\", \"menuItems\": [{\"action\": \"READ_ALOUD\"}]}";

					OutputStreamWriter writer = new OutputStreamWriter(conn
							.getOutputStream());
					BufferedWriter bufferedWriter = new BufferedWriter(writer);
					try {
						bufferedWriter.write(item);
						bufferedWriter.newLine();
						bufferedWriter.flush();
						writer.flush();
					} finally {
						bufferedWriter.close();
						writer.close();
					}

					mainHandler.post(callback.setStatus(readResponse(conn)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private String readResponse(HttpURLConnection conn) throws IOException {
		InputStreamReader reader = new InputStreamReader(conn.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(reader);
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(
					conn.getResponseCode() + ": " + conn.getResponseMessage()
							+ ":").append(LINE_SEPARATOR);
			for (String s = bufferedReader.readLine(); s != null; s = bufferedReader
					.readLine()) {
				builder.append(s).append(LINE_SEPARATOR);
			}

			return builder.toString();
		} finally {
			bufferedReader.close();
			reader.close();
		}
	}

	public static abstract class Callback implements Runnable {

		protected String status;

		public Callback setStatus(String status) {
			this.status = status;

			return this;
		}

		@Override
		public abstract void run();
	}
}
