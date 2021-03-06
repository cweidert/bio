package com.heliomug.bio.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CancellationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import com.heliomug.bio.GenomeQuery;
import com.heliomug.bio.ProbeSet;
import com.heliomug.bio.QueriableGenome;
import com.heliomug.bio.repository.GenomeRepository;
import com.heliomug.utils.FileUtils;
import com.heliomug.utils.GlobalStatusDisplayer;
import com.heliomug.utils.StatusDisplay;

public class MainProbeQuery extends JFrame implements ActionListener, StatusDisplay {
	private static final long serialVersionUID = -4721791747633762165L;

	private static final String CURRENT_REPOSITORY_STRING = 	"Current Repository: \t%s";
	private static final String CURRENT_QUERY_STRING = 			"Current Query: \t%s";
	private static final String CURRENT_RESULTS_STRING = 		"Current Results: \t%s";
	
	private static final int GRAPHICS_WIDTH = 640;
	private static final int GRAPHICS_HEIGHT = 360;
	
	private static final int HISTOGRAM_WIDTH = 640;
	private static final int HISTOGRAM_HEIGHT = 360;
	
	private static MainProbeQuery instance;
	
	private QueriableGenome repo;
	private ProbeSet results;
	private GenomeQuery currentQuery;
	
	private QueryWidget queryMin;
	private QueryWidget queryMax;
	private JButton executeButton;
	private JButton cancelButton;
	private Thread queryThread;
	
	private JLabel statusLabel;
	
	private OneDimVisPanel statsPanel;
	private ThreeDimVisPanel graphicsPanel;
	private TextPanel textPanel;
	
	private MainProbeQuery(String title) {
		super(title);
		
		MainProbeQuery.instance = this;
		GlobalStatusDisplayer.setStatusDisplayer(this);
		
		this.repo = null;
		this.results = null;
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(mainPanel());
		setJMenuBar(menuBar());
		pack();
		this.repaint();
	}
	
	public static MainProbeQuery get() {
		return instance;
	}

	
	private JPanel mainPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		JPanel subPanel = new JPanel();
		subPanel.setLayout(new BorderLayout());
		
		subPanel.add(statusPanel(), BorderLayout.NORTH);
		subPanel.add(resultsPane(), BorderLayout.CENTER);
		subPanel.add(queryPanel(), BorderLayout.SOUTH);
		mainPanel.add(subPanel, BorderLayout.CENTER);
		
		statusLabel = new JLabel("No Repository Loaded Yet!");
		statusLabel.setBorder(StandardPanel.STANDARD_BORDER);
		mainPanel.add(statusLabel, BorderLayout.SOUTH);

		return mainPanel;
	}
	
	private JTabbedPane resultsPane() {
		JTabbedPane tabbedPane = new JTabbedPane();
		textPanel = new TextPanel();
		tabbedPane.addTab("Results Text", null, textPanel, "Results in text form");
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_T);
		tabbedPane.setDisplayedMnemonicIndexAt(0, 8);
		graphicsPanel = new ThreeDimVisPanel(GRAPHICS_WIDTH, GRAPHICS_HEIGHT);
		tabbedPane.addTab("Results Graph", null, graphicsPanel, "Results customizable graphical form");
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_G);
		statsPanel = new OneDimVisPanel(HISTOGRAM_WIDTH, HISTOGRAM_HEIGHT);
		tabbedPane.addTab("Results Stats", null, statsPanel, "Histogram & Stats for various results measures");
		tabbedPane.setMnemonicAt(2, KeyEvent.VK_S);
		tabbedPane.setDisplayedMnemonicIndexAt(2, 8);
		return tabbedPane;
	}
	
	@SuppressWarnings("serial")
	private JPanel statusPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1));
		panel.add(new JLabel(" ") {
			@Override
			public void paint(Graphics g) {
				this.setText(String.format(CURRENT_REPOSITORY_STRING, getRepositoryString()));
				super.paint(g);
			}
		});
		panel.add(new JLabel(" ") {
			@Override
			public void paint(Graphics g) {
				this.setText(String.format(CURRENT_QUERY_STRING, getQueryString()));
				super.paint(g);
			}
		});
		panel.add(new JLabel(" ") {
			@Override
			public void paint(Graphics g) {
				this.setText(String.format(CURRENT_RESULTS_STRING, getResultsString()));
				super.paint(g);
			}
		});
		return panel;
	}
	
	private JPanel queryPanel() {
		JPanel panel = new JPanel();
		JPanel subpanel;
		
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		
		subpanel = new JPanel();
		subpanel.add(new JLabel("Query from "));
		queryMin = new QueryWidget();
		subpanel.add(queryMin);
		subpanel.add(new JLabel(" to "));
		queryMax = new QueryWidget(); 
		subpanel.add(queryMax);
		panel.add(subpanel, BorderLayout.NORTH);
		
		subpanel = new JPanel();
		executeButton = new JButton("Execute Query");
		executeButton.setEnabled(false);
		executeButton.addActionListener(this);
		executeButton.setActionCommand("EXECUTE QUERY");
		executeButton.setMnemonic(KeyEvent.VK_E);
		subpanel.add(executeButton);
		cancelButton = new JButton("Cancel Query");
		cancelButton.setEnabled(false);
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("CANCEL QUERY");
		cancelButton.setMnemonic(KeyEvent.VK_C);
		subpanel.add(cancelButton);
		panel.add(subpanel, BorderLayout.CENTER);
		return panel;
	}
	
	private JMenuBar menuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu menu, submenu;
		JMenuItem item;
		
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		item = new JMenuItem("New Repository");
		item.setMnemonic(KeyEvent.VK_N);
		item.setActionCommand("NEW REPOSITORY");
		item.addActionListener(this);
		item.setAccelerator(KeyStroke.getKeyStroke("control N"));
		menu.add(item);
		item = new JMenuItem("Open Repository");
		item.setMnemonic(KeyEvent.VK_O);
		item.setActionCommand("OPEN REPOSITORY");
		item.addActionListener(this);
		item.setAccelerator(KeyStroke.getKeyStroke("control O"));
		menu.add(item);
		menu.addSeparator();
		submenu = new JMenu("Save Results");
		submenu.setMnemonic(KeyEvent.VK_S);
		item = new JMenuItem("Text");
		item.setActionCommand("SAVE RESULTS TEXT");
		item.addActionListener(this);
		submenu.add(item);
		item = new JMenuItem("Stats");
		item.setActionCommand("SAVE STATS");
		item.addActionListener(this);
		submenu.add(item);
		item = new JMenuItem("Graph");
		item.setActionCommand("SAVE GRAPH");
		item.addActionListener(this);
		submenu.add(item);
		item = new JMenuItem("Histogram");
		item.setActionCommand("SAVE HISTOGRAM");
		item.addActionListener(this);
		submenu.add(item);
		menu.add(submenu);
		menu.addSeparator();
		item = new JMenuItem("Exit");
		item.setMnemonic(KeyEvent.VK_X);
		item.setActionCommand("EXIT");
		item.addActionListener(this);
		menu.add(item);
		bar.add(menu);
		
		return bar; 
	}

	
	public String getRepositoryString() {
		if (repo == null) {
			return "[no repository]";
		} else {
			return repo.getBaseDirectory().getPath();
		}
	}

	public String getQueryString() {
		if (currentQuery == null) {
			return "[no query]";
		} else {
			return currentQuery.toString();
		}
	}
	
	private String getResultsString() {
		if (results == null) {
			return "[no results]";
		} else if (results.size() == 1) {
			return "1 result";
		} else {
			return results.size() + " results";
		}
	}
	
	
	private void executeQuery() {
		displayStatus("executing query");
		
		if (repo != null) {
			Thread t = new Thread(() -> {
				executeButton.setEnabled(false);
				cancelButton.setEnabled(true);

				String startChromo = queryMin.getChromo();
				int startOffset = queryMin.getOffset();
				String endChromo = queryMax.getChromo();
				int endOffset = queryMax.getOffset();
				GenomeQuery query = new GenomeQuery(startChromo, startOffset, endChromo, endOffset);
				
				clearResults();

				try {
					results = repo.query(query);
					currentQuery = query;
					displayStatus("Finished query.");
					new Thread(() -> {
						broadcastResults();
					}).start();
				} catch (InterruptedException e) {
					displayStatus("Query Cancelled.");
				} catch (ClassNotFoundException | IOException e) {
					displayStatus("ERROR: Query Failed");
					e.printStackTrace();
				}
				
				executeButton.setEnabled(true);
				cancelButton.setEnabled(false);
			});
			
			t.start();
			this.queryThread = t;
			displayStatus("query started thread");
		}
	}

	private void clearResults() {
		results = null;
		currentQuery = null;
		graphicsPanel.clear();
		textPanel.clear();
		statsPanel.clear();
		repaint();
	}
	
	private void broadcastResults() {
		statsPanel.setResults(results);
		graphicsPanel.setResults(results);
		textPanel.setResults(results);
		graphicsPanel.setChromosomes(results.getChromoList());
	}
	
	
	private void setRepository(GenomeRepository repo) {
		this.repo = repo;
		displayStatus("Set Repository.");
		queryMin.setChromos(repo.getChromoList());
		queryMax.setChromos(repo.getChromoList());
		executeButton.setEnabled(true);
		this.repaint();
	}
	
	private void openRepository() {
		//File inputDirectory = new File("/home/cweidert/prog/data/biodiscovery/repository"); 
		File inputDirectory = FileUtils.selectDirectory("Select Repository Directory to Load");
		if (inputDirectory != null) {
			Thread t = new Thread(() -> {
				try {
					displayStatus("Loading repository at " + inputDirectory.getAbsolutePath() + "...");
					setRepository(GenomeRepository.loadRepository(inputDirectory));
					displayStatus("Repository at " + inputDirectory.getAbsolutePath() + " loaded.");
				} catch (FileNotFoundException e) {
					displayStatus("ERROR: Could not open repository.  File not found.");
					e.printStackTrace();
				} catch (ClassNotFoundException | IOException e) {
					displayStatus("ERROR: Could not open repository");
					e.printStackTrace();
				}
			});
			t.start();
		}
	}
	
	private void createRepository() {
		File inputFile = FileUtils.selectFile("Select File for Input");
		File outputDirectory = FileUtils.selectDirectory("Select Directory to Save Repository"); 
		if (inputFile != null && outputDirectory != null) {
			Thread t = new Thread(() -> {
				try {
					String inPath = inputFile.getAbsolutePath();
					String outPath = outputDirectory.getAbsolutePath();
					displayStatus("Creating repository at " + outPath + " from " + inPath + "...");
					GenomeRepository.createRepository(inputFile, outputDirectory);
					setRepository(GenomeRepository.loadRepository(outputDirectory));
					displayStatus("Repository created at " + outputDirectory.getAbsolutePath() + ".");
				} catch (ClassNotFoundException | IOException e) {
					displayStatus("ERROR: could not create repository");
					e.printStackTrace();
				}
			});
			t.start();
		}
	}
	
	
	private void saveGraph() {
		if (results == null) {
			displayStatus("Need results first!");
		} else {
			new Thread(() -> {
				if (results != null) {
					try {
						displayStatus("Saving Graph...");
						String fileName = graphicsPanel.saveGraph();
						displayStatus("Saved graph as " + fileName + ".");
					} catch (CancellationException e) {
						displayStatus("Saving graph cancelled.");
					} catch (IOException e) {
						displayStatus("Saving graph failed.");
					}
				}
			}).start();
		}
	}
	
	private void saveHistogram() {
		if (results == null) {
			displayStatus("Need results first!");
		} else {
			new Thread(() -> {
				if (results != null) {
					try {
						displayStatus("Saving histogram...");
						String fileName = statsPanel.saveHistogram();
						displayStatus("Saved histogram as " + fileName + ".");
					} catch (CancellationException e) {
						displayStatus("Saving histogram Cancelled.");
					} catch (IOException e) {
						displayStatus("Saving histogram Failed.");
					}
				}
			}).start();
		}
	}
	
	private void saveStats() {
		if (results == null) {
			displayStatus("Need results first!");
		} else {
			new Thread(() -> {
				try {
					displayStatus("Saving stats summary...");
					String fileName = statsPanel.saveStatsSummary();
					displayStatus("Saved stats summary as " + fileName + ".");
				} catch (CancellationException e) {
					displayStatus("Saving stats summary cancelled.");
				} catch (FileNotFoundException e) {
					displayStatus("Saving stats summary failed.");
				}
			}).start();
		}
	}
	
	private void saveText() {
		if (results == null) {
			displayStatus("Need results first!");
		} else {
			new Thread(() -> {
				try {
					displayStatus("Saving results text...");
					String fileName = FileUtils.saveTextAs(results.longString(), "Save Results As");
					displayStatus("Saved results text as " + fileName + ".");
				} catch (CancellationException e) {
					displayStatus("Saving results text cancelled.");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}).start();
		}
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand(); 
		if (command.equals("NEW REPOSITORY")) {
			createRepository();
		} else if (command.equals("OPEN REPOSITORY")) {
			openRepository();
		} else if (command.equals("EXECUTE QUERY")) {
			executeQuery();
			this.repaint();
		} else if (command.equals("CANCEL QUERY")) {
			if (queryThread != null) queryThread.interrupt();
		} else if (command.equals("SAVE GRAPH")) {
			saveGraph();
		} else if (command.equals("SAVE HISTOGRAM")) {
			saveHistogram();
		} else if (command.equals("SAVE RESULTS TEXT")) {
			saveText();
		} else if (command.equals("SAVE STATS")) {
			saveStats();
		} else if (command.equals("EXIT")) {
			System.exit(0);
		} else {
			System.out.println("UNKNOWN COMMAND: " + command);
		}
	}

	@Override
	public void displayStatus(Object status) {
		statusLabel.setText(status.toString());
		repaint();
	}

	
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			MainProbeQuery frame = new MainProbeQuery("Probe Querier");
			frame.setVisible(true);
		});
	}
}
