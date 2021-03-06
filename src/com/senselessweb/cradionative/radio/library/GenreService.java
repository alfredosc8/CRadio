package com.senselessweb.cradionative.radio.library;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.util.Log;

public class GenreService
{
	private static final String genresLocation = "http://v-serv.dyndns.org/remote-radio/genres.txt";
	
	private static final String shoutcastRequestUrl = "http://www.shoutcast.com/search-ajax/";

	private static final List<NameValuePair> parameters = new ArrayList<NameValuePair>(); 
	
	static
	{
		parameters.add(new BasicNameValuePair("count", "15"));
		parameters.add(new BasicNameValuePair("mode", "listenershead2"));
		parameters.add(new BasicNameValuePair("order", "desc2"));
	}

	final HttpClient client = new DefaultHttpClient();

	private final List<String> genres = new ArrayList<String>();
	
	private int genrePointer = 0;
	
	/* If genre is set, it overrides the genre identified by the genrePointer */
	private String genre = null;
	
	private final Map<String, List<Item>> itemsByGenre = new HashMap<String, List<Item>>();
	
	private int itemPointer = 0;
	
	
	
	public GenreService()
	{
		Executors.newFixedThreadPool(1).execute(new Runnable() {
			
			@Override
			public void run()
			{
				GenreService.this.init();
			}
		});
	}
	
	private synchronized void init()
	{
		try
		{
			Log.d(GenreService.class.toString(), "Loading genres");
			final BufferedReader reader = new BufferedReader(new InputStreamReader(
					new URL(genresLocation).openConnection().getInputStream()));
			for (String line = reader.readLine(); line != null; line = reader.readLine())
				this.genres.add(line);
			Log.d(GenreService.class.toString(), "Loaded " + this.genres.size() + " genres");
			
			GenreService.this.loadGenreItems(GenreService.this.genres.get(0));
		}
		catch (final Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	

	public synchronized Item getCurrent()
	{
		return this.getCurrentGenreItems().get(this.itemPointer);
	}
	
	public synchronized Item getCurrentIfAvailable()
	{
		return this.itemsByGenre.containsKey(this.getCurrentGenre()) ?
				this.getCurrent() : null;
	}
	
	public synchronized void nextGenre()
	{
		this.genrePointer++;
		this.genre = null;
		if (this.genrePointer >= this.genres.size()) this.genrePointer = 0;
		this.itemPointer = 0;
	}
	
	public synchronized void previousGenre()
	{
		this.genrePointer--;
		this.genre = null;
		if (this.genrePointer < 0) this.genrePointer = this.genres.size() - 1;
		this.itemPointer = 0;
	}
	
	public synchronized void nextStation()
	{
		this.itemPointer++;
		if (this.itemPointer >= this.getCurrentGenreItems().size()) this.itemPointer = 0;
	}
	
	public synchronized void previousStation()
	{
		this.itemPointer--;
		if (this.itemPointer < 0) this.itemPointer = this.getCurrentGenreItems().size() - 1;
	}
	
	private synchronized List<Item> getCurrentGenreItems()
	{
		final String genre = this.genre != null ? this.genre : this.genres.get(this.genrePointer);
		if (!this.itemsByGenre.containsKey(genre))
		{
			this.loadGenreItems(genre);
			this.itemPointer = 0;
		}
		return this.itemsByGenre.get(genre);
	}
	
	private synchronized void loadGenreItems(final String genre)
	{
		Log.d(GenreService.class.toString(), "Loading items for " + genre);
		try
		{
			// Send the request
			final HttpPost post = new HttpPost(shoutcastRequestUrl + URLEncoder.encode(genre));
			post.setEntity(new UrlEncodedFormEntity(parameters));
			
			final String response = this.client.execute(post, new BasicResponseHandler());
		
			// Parse the response
			final Document document = Jsoup.parse(response);
			final List<Item> result = new ArrayList<Item>();
			for (final Element element : document.getElementsByClass("dirlist"))
			{
				final String url = element.select("a.clickabletitle").attr("href");
				final String title = element.select("a.clickabletitle").text();
				result.add(new Item(genre + " - " + title, url, true));
			}
			this.itemsByGenre.put(genre, result);
			Log.d(GenreService.class.toString(), "Loaded " + this.itemsByGenre.get(genre).size() + " items");
		}
		catch (final Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public synchronized String getCurrentGenre()
	{
		return this.genres.get(this.genrePointer);
	}

	public synchronized void setGenre(final String genre)
	{
		if (this.genres.contains(genre))
		{
			this.genrePointer = this.genres.indexOf(genre);
			this.itemPointer = 0;
		}
		else
		{
			this.genre = genre;
		}
	}
}
