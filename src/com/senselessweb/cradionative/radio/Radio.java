package com.senselessweb.cradionative.radio;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.widget.TextView;

import com.senselessweb.cradionative.radio.library.GenreService;
import com.senselessweb.cradionative.radio.library.Item;
import com.senselessweb.cradionative.radio.library.PresetsService;

public class Radio implements OnPreparedListener
{
	
	private final GenreService genreService = new GenreService();
	
	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
	
	private ScheduledFuture<?> currentGenreItemTask = null;
	
	private final PresetsService presetsService = new PresetsService();
	
	private final MediaPlayer mediaPlayer = new MediaPlayer();

	private final Activity activity;
	
	private final TextView display;
	
	public Radio(final Activity activity, final TextView display)
	{
		this.activity = activity;
		this.display = display;
		
		this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		this.mediaPlayer.setOnPreparedListener(this);
	}
	
	public synchronized void togglePlayback()
	{
		if (this.mediaPlayer.isPlaying())
			this.mediaPlayer.stop();
		
		// TODO What about play?
	}	

	public synchronized void playPreset(final int preset)
	{
		this.play(this.presetsService.getPreset(preset));
	}
	
	public synchronized void nextGenre()
	{
		this.genreService.nextGenre();
		this.playGenreItemAsync();
	}
	
	public synchronized void previousGenre()
	{
		this.genreService.previousGenre();
		this.playGenreItemAsync();
	}
	
	public synchronized void nextStation()
	{
		this.genreService.nextStation();
		this.playGenreItemAsync();
	}
	
	public synchronized void previousStation()
	{
		this.genreService.previousStation();
		this.playGenreItemAsync();
	}
	
	private void playGenreItemAsync()
	{
		this.display.setText(this.genreService.getCurrentGenre());
		this.display.setTextColor(Color.rgb(255, 165, 0));

		if (this.currentGenreItemTask != null)
			this.currentGenreItemTask.cancel(false);
		
		this.currentGenreItemTask = this.scheduledExecutorService.schedule(new Runnable() 
		{
			@Override
			public void run()
			{
				Radio.this.play(Radio.this.genreService.getCurrent());
			}
		}, 2, TimeUnit.SECONDS);
	}
	
	private void play(final Item item)
	{
		try
		{
			final Uri myUri = Uri.parse(item.getUrl());
			
			if (this.mediaPlayer.isPlaying())
				this.mediaPlayer.stop();
			
			this.activity.runOnUiThread(new Runnable() 
			{
				@Override
				public void run()
				{
					Radio.this.display.setText(Radio.this.genreService.getCurrent().getName());
					Radio.this.display.setTextColor(Color.rgb(255, 165, 0));
				}
			});
			
			this.mediaPlayer.reset();
			this.mediaPlayer.setDataSource(this.activity.getApplicationContext(), myUri);
			this.mediaPlayer.prepareAsync();
		} 
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onPrepared(final MediaPlayer mp)
	{
		mp.start();
		this.activity.runOnUiThread(new Runnable() 
		{
			@Override
			public void run()
			{
				Radio.this.display.setTextColor(Color.WHITE);
			}
		});
	}

}
