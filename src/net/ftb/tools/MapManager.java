package net.ftb.tools;

import net.ftb.data.Map;
import net.ftb.data.Settings;
import net.ftb.gui.LaunchFrame;
import net.ftb.gui.dialogs.MapOverwriteDialog;
import net.ftb.log.Logger;
import net.ftb.util.DownloadUtils;
import net.ftb.util.FileUtils;
import net.ftb.util.OSUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

public class MapManager extends JDialog {
	private static final long serialVersionUID = 6897832855341265019L;

	private JPanel contentPane;
	private double downloadedPerc;
	private final JProgressBar progressBar;
	private final JLabel label;
	public static boolean overwrite = false;
	private static String sep = File.separator;

	private class MapManagerWorker extends SwingWorker<Boolean, Void> {
		@Override
		protected Boolean doInBackground() throws Exception {
			String installPath = Settings.getSettings().getInstallPath();
			Map map = Map.getMap(LaunchFrame.getSelectedMapIndex());
			if(new File(installPath, map.getSelectedCompatible() + "/minecraft/saves/" + map.getMapName()).exists()) {
				MapOverwriteDialog dialog = new MapOverwriteDialog(LaunchFrame.getInstance(), true);
				dialog.setVisible(true);
				if(overwrite) {
					FileUtils.delete(new File(installPath, map.getSelectedCompatible() + "/minecraft/saves/" + map.getMapName()));
				} else {
					Logger.logInfo("Canceled map installation.");
					return false;
				}
			}
			downloadMap(map.getUrl(), map.getMapName());
			return false;
		}

		public void downloadUrl(String filename, String urlString) throws MalformedURLException, IOException, NoSuchAlgorithmException {
			BufferedInputStream in = null;
			FileOutputStream fout = null;
			try {
				URL url_ = new URL(urlString);
				in = new BufferedInputStream(url_.openStream());
				fout = new FileOutputStream(filename);
				byte data[] = new byte[1024];
				int count, amount = 0, steps = 0, mapSize = url_.openConnection().getContentLength();
				progressBar.setMaximum(10000);
				while((count = in.read(data, 0, 1024)) != -1) {
					fout.write(data, 0, count);
					downloadedPerc += (count * 1.0 / mapSize) * 100;
					amount += count;
					steps++;
					if(steps > 100) {
						steps = 0;
						progressBar.setValue((int)downloadedPerc * 100);
						label.setText((amount / 1024) + "Kb / " + (mapSize / 1024) + "Kb");
					}
				}
			} finally {
				in.close();
				fout.flush();
				fout.close();
			}
		}

		protected void downloadMap(String mapName, String dir) throws IOException, NoSuchAlgorithmException {
			Logger.logInfo("Downloading");
			String installPath = new File(".").getCanonicalPath();
            Logger.logInfo("Dir: " + dir);
            Logger.logInfo("Downloading Map to: " + installPath);
			new File(installPath + "/Maps/" + dir + "/").mkdirs();
			new File(installPath + "/Maps/" + dir + "/" + mapName).createNewFile();
			downloadUrl(installPath + "/Maps/" + dir + "/" + mapName, DownloadUtils.getCreeperhostLink("maps/" + mapName));
			FileUtils.extractZipTo(installPath + "/Maps/" + dir + "/" + mapName, installPath + "/Maps/" + dir);
			installMap(mapName, dir);
		}

		protected void installMap(String mapName, String dir) throws IOException {
			Logger.logInfo("Installing");
			String installPath = Settings.getSettings().getInstallPath();
			String tempPath = new File(".").getCanonicalPath();
            Logger.logInfo("Install Path:" + installPath);
            Logger.logInfo("Temp Path:" + tempPath);
			Map map = Map.getMap(LaunchFrame.getSelectedMapIndex());
			new File(installPath, map.getSelectedCompatible() + "/minecraft/saves/" + dir).mkdirs();
			FileUtils.copyFolder(new File(tempPath, "Maps/" + dir + "/" + dir), new File(installPath, map.getSelectedCompatible() + "/minecraft/saves/" + dir));
			FileUtils.copyFile(new File(tempPath, "Maps/" + dir + "/" + "version"), new File(installPath, map.getSelectedCompatible() + "/minecraft/saves/" + dir + "/version"));
		}
	}

	public MapManager(JFrame owner, Boolean model) {
		super(owner, model);
		setResizable(false);
		setTitle("Downloading...");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setBounds(100, 100, 313, 138);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		progressBar = new JProgressBar();
		progressBar.setBounds(10, 63, 278, 22);
		contentPane.add(progressBar);

		JLabel lblDownloadingMap = new JLabel("<html><body><center>Downloading map...<br/>Please Wait</center></body></html>");
		lblDownloadingMap.setHorizontalAlignment(SwingConstants.CENTER);
		lblDownloadingMap.setBounds(0, 5, 313, 30);
		contentPane.add(lblDownloadingMap);

		label = new JLabel("");
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBounds(0, 42, 313, 14);
		contentPane.add(label);

		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent arg0) {
				MapManagerWorker worker = new MapManagerWorker() {
					@Override
					protected void done() {
						setVisible(false);
						super.done();
					}
				};
				worker.execute();
			}
			@Override public void windowActivated(WindowEvent e) { }
			@Override public void windowClosed(WindowEvent e) { }
			@Override public void windowClosing(WindowEvent e) { }
			@Override public void windowDeactivated(WindowEvent e) { }
			@Override public void windowDeiconified(WindowEvent e) { }
			@Override public void windowIconified(WindowEvent e) { }
		});
	}

	public static void cleanUp() {
		Map map = Map.getMap(LaunchFrame.getSelectedMapIndex());
		File tempFolder = new File(OSUtils.getDynamicStorageLocation(), "Maps" + sep + map.getMapName() + sep);
		for(String file: tempFolder.list()) {
			if(!file.equals(map.getLogoName()) && !file.equals(map.getImageName()) && !file.equalsIgnoreCase("version")) {
				try {
					FileUtils.delete(new File(tempFolder, file));
				} catch (IOException e) {
					Logger.logError(e.getMessage(), e);
				}
			}
		}
	}
}
