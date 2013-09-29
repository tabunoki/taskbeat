package com.binarysprite.taskbeat;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import org.jdesktop.swingworker.SwingWorker;

/**
 * プログラマークロックです。
 * ストップウォッチで時間を計測し当日の通算時間をはてなグラフへ登録します。
 * 
 * @author Tabunoki
 *
 */
public class Clock {
	
	/**
	 * 経過時間を表示するモニターです。
	 */
	private final JButton monitor = new JButton("00:00:00");
	
	/**
	 * 通信時の進捗を表示するプログレスバーです。
	 */
	private final JProgressBar progressBar = new JProgressBar();
	
	/**
	 * 表示色の設定です。
	 */
	private static final Color WAITING_FOREGROUND_COLOR = new Color(
			Integer.parseInt(ClockProperties.WAITING_FOREGROUND_COLOR_R.get()),
			Integer.parseInt(ClockProperties.WAITING_FOREGROUND_COLOR_G.get()),
			Integer.parseInt(ClockProperties.WAITING_FOREGROUND_COLOR_B.get()));
	private static final Color WAITING_BACKGROUND_COLOR = new Color(
			Integer.parseInt(ClockProperties.WAITING_BACKGROUND_COLOR_R.get()),
			Integer.parseInt(ClockProperties.WAITING_BACKGROUND_COLOR_G.get()),
			Integer.parseInt(ClockProperties.WAITING_BACKGROUND_COLOR_B.get()));
	private static final Color RUNNING_FOREGROUND_COLOR = new Color(
			Integer.parseInt(ClockProperties.RUNNING_FOREGROUND_COLOR_R.get()),
			Integer.parseInt(ClockProperties.RUNNING_FOREGROUND_COLOR_G.get()),
			Integer.parseInt(ClockProperties.RUNNING_FOREGROUND_COLOR_B.get()));
	private static final Color RUNNING_BACKGROUND_COLOR = new Color(
			Integer.parseInt(ClockProperties.RUNNING_BACKGROUND_COLOR_R.get()),
			Integer.parseInt(ClockProperties.RUNNING_BACKGROUND_COLOR_G.get()),
			Integer.parseInt(ClockProperties.RUNNING_BACKGROUND_COLOR_B.get()));
	
	/**
	 * 計測開始の基準時間です。
	 */
	private long baseTime;
	
	/**
	 * 計測停止の時間です。
	 */
	private long stopTime;
	
	/**
	 * 計測中を表す真偽値です。
	 */
	private boolean running;
	
	/**
	 * はてなグラフと通信を行うAPIオブジェクトです。
	 */
	private final HatenaGraph hatenaGraph = new HatenaGraph(
			ClockProperties.USERNAME.get(), ClockProperties.PASSWORD.get());
	
	/**
	 * 日付表示用のフォーマットです。
	 */
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * 時刻表示用のフォーマットです。
	 */
	private final DecimalFormat decimalFormat = new DecimalFormat("00");

	/**
	 * 時計の時間を初期化するタスククラスです。
	 * @author Tabunoki
	 *
	 */
	private class Initialize extends SwingWorker<Long, Long> {

		/* (non-Javadoc)
		 * @see org.jdesktop.swingworker.SwingWorker#doInBackground()
		 */
		@Override
		protected Long doInBackground() throws Exception {
			
			Date now = new Date();
			
			stopTime = now.getTime();
			baseTime = (long) (stopTime - hatenaGraph.getValue(now, ClockProperties.GRAPH_NAME.get()) * 1000.0d);
			
			new Update().execute();
			
			logTime("Initialised.", System.out);
			
			return null;
		}

		/* (non-Javadoc)
		 * @see org.jdesktop.swingworker.SwingWorker#done()
		 */
		@Override
		protected void done() {
			monitor.setForeground(WAITING_FOREGROUND_COLOR);
			monitor.setBackground(WAITING_BACKGROUND_COLOR);
			progressBar.setVisible(false);
		}
		
	}
	
	/**
	 * 時計の表示を更新するタスククラスです。
	 * @author Tabunoki
	 *
	 */
	private class Update extends SwingWorker<Long, Long> {
		
		/* (non-Javadoc)
		 * @see org.jdesktop.swingworker.SwingWorker#doInBackground()
		 */
		@Override
		protected Long doInBackground() throws Exception {
			
			long now = System.currentTimeMillis();
			
			long border = getBorder(now);
			
			if (baseTime < border) {
				new Record(new Date(baseTime), border - baseTime).execute();
				baseTime = border;
			}
			
			return (now - baseTime) / 1000;
		}

		/* (non-Javadoc)
		 * @see org.jdesktop.swingworker.SwingWorker#done()
		 */
		@Override
		protected void done() {
			
			try {
				long time = this.get();
				
				monitor.setText(
						decimalFormat.format(time / 3600) + ":" +
						decimalFormat.format(time % 3600 / 60) + ":" +
						decimalFormat.format(time % 3600 % 60));
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * 時計の時間を記録するタスククラスです。
	 * @author Tabunoki
	 *
	 */
	private class Record extends SwingWorker<Long, Long> {
		
		private final Date date;
		
		private final long time;

		/**
		 * @param date
		 * @param time
		 */
		public Record(Date date, long time) {
			super();
			this.date = date;
			this.time = time;
		}

		/* (non-Javadoc)
		 * @see org.jdesktop.swingworker.SwingWorker#doInBackground()
		 */
		@Override
		protected Long doInBackground() throws Exception {
			
			this.publish(time);
			
			hatenaGraph.post(ClockProperties.GRAPH_NAME.get(), date, time / 1000.0d);
			
			return null;
		}

		/* (non-Javadoc)
		 * @see org.jdesktop.swingworker.SwingWorker#done()
		 */
		@Override
		protected void done() {
			if (running) {
				monitor.setForeground(RUNNING_FOREGROUND_COLOR);
				monitor.setBackground(RUNNING_BACKGROUND_COLOR);
			} else {
				monitor.setForeground(WAITING_FOREGROUND_COLOR);
				monitor.setBackground(WAITING_BACKGROUND_COLOR);
			}
			progressBar.setVisible(false);
		}

		/* (non-Javadoc)
		 * @see org.jdesktop.swingworker.SwingWorker#process(java.util.List)
		 */
		@Override
		protected void process(List<Long> arg0) {
			progressBar.setVisible(true);
		}
		
	}
	
	
	/**
	 * プログラマークロックを起動します。
	 * @param args
	 */
	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Clock();
			}
		});
	}
	
	/**
	 * プログラマークロックを生成します。
	 */
	public Clock() {
		
		Font tempFont = null;
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(
					ClockProperties.BASEDIR +"/"+ ClockProperties.FONT.get()));
			
			tempFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (tempFont == null) {
				tempFont = this.monitor.getFont();
			}
		}
		final Font monitorFont = tempFont;
		
		/*
		 * コンポーネントの生成
		 */
		final JFrame frame = new JFrame();
		final Container container = frame.getContentPane();
		final JLabel taskLabel = new JLabel("テスト");
		
		/*
		 * コンポーネントの編集
		 */
		
		monitor.setOpaque(true);
		monitor.setForeground(WAITING_FOREGROUND_COLOR);
		monitor.setBackground(WAITING_BACKGROUND_COLOR);
		monitor.setBorderPainted(false);
		monitor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				action();
			}
		});
		monitor.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				
				monitor.setFont(monitorFont.deriveFont(
						(float) Math.sqrt(monitor.getWidth() * monitor.getHeight()) / 4));
				
				taskLabel.setFont(taskLabel.getFont().deriveFont(
						(float) Math.sqrt(monitor.getWidth() * monitor.getHeight()) / 16));
			}
		});
		
		progressBar.setIndeterminate(true);
		
		frame.setTitle(ClockProperties.GRAPH_NAME.get());
		
		/*
		 * コンポーネントのレイアウト
		 */
		final SpringLayout layout = new SpringLayout();
		

		layout.putConstraint(SpringLayout.EAST, monitor, 0, SpringLayout.EAST, container);
		layout.putConstraint(SpringLayout.SOUTH, monitor, 0, SpringLayout.SOUTH, container);
		layout.putConstraint(SpringLayout.WEST, monitor, 0, SpringLayout.WEST, container);
		layout.putConstraint(SpringLayout.NORTH, monitor, 0, SpringLayout.NORTH, container);
		
		layout.putConstraint(SpringLayout.EAST, progressBar, -20, SpringLayout.EAST, container);
		layout.putConstraint(SpringLayout.SOUTH, progressBar, -10, SpringLayout.SOUTH, container);
		layout.putConstraint(SpringLayout.WEST, progressBar, 20, SpringLayout.WEST, container);
		layout.putConstraint(SpringLayout.NORTH, progressBar, -50, SpringLayout.SOUTH, container);
		
		container.setLayout(layout);
		
		
		/*
		 * コンポーネントの組立
		 */
		container.add(taskLabel);
		container.add(progressBar);
		container.add(monitor);
		
		/*
		 * フレームの編集
		 */
		frame.setPreferredSize(new Dimension(
				Integer.parseInt(ClockProperties.WINDOW_WIDTH.get()),
				Integer.parseInt(ClockProperties.WINDOW_HEIGHT.get())));
		frame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent event) {}
			
			@Override
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		/*
		 * 
		 */
		new Initialize().execute();
	}
	
	/**
	 * 計測を開始、もしくは停止します。
	 */
	private void action() {
		
		long now = System.currentTimeMillis();
		
		if (this.running) {
			
			this.stopTime = now;
			
			this.monitor.setForeground(WAITING_FOREGROUND_COLOR);
			this.monitor.setBackground(WAITING_BACKGROUND_COLOR);
			this.running = false;
			
			new Record(new Date(this.stopTime), this.stopTime - this.baseTime).execute();
			
			this.logTime("Stop.", System.out);
			
		} else {
			
			if (this.baseTime < this.getBorder(now)) {
				this.baseTime = now;
			} else {
				this.baseTime += now - this.stopTime;
			}
			
			this.monitor.setForeground(RUNNING_FOREGROUND_COLOR);
			this.monitor.setBackground(RUNNING_BACKGROUND_COLOR);
			this.running = true;
			
			new Thread(new Runnable() {
				public void run() {
					while (running) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						new Update().execute();
					}
				}
			}).start();
			
			this.logTime("Start.", System.out);
		}
	}
	
	/**
	 * 指定日時の零時零分零秒の日時をミリ秒で返します。
	 * @param time
	 * @return
	 */
	private long getBorder(long time) {
		
		Calendar calendar = Calendar.getInstance();
		
		calendar.setTimeInMillis(time);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
//		System.out.println("border: " + this.dateFormat.format(calendar.getTime()));
		
		return calendar.getTimeInMillis();
	}
	
	/**
	 * ログを出力します。
	 * @param message
	 * @param stream
	 */
	private void logTime(String message, PrintStream stream) {
		
		StringBuffer buffer = new StringBuffer(message);
		
		buffer.append("\n");
		buffer.append("\tBase Time: ");
		buffer.append(dateFormat.format(new Date(this.baseTime)));
		buffer.append("\n");
		buffer.append("\tStop Time: ");
		buffer.append(dateFormat.format(new Date(this.stopTime)));
		buffer.append("\n");
		
		stream.print(buffer.toString());
	}
}
