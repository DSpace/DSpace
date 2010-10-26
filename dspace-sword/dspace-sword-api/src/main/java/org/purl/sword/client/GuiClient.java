/**
 * Copyright (c) 2008, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */
package org.purl.sword.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingworker.SwingWorker;

import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.SWORDErrorDocument;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.SwordValidationInfoType;

/**
 * Main class that creates the GUI interface for the demonstration SWORD client.
 * 
 * @author Neil Taylor
 * @author Jim Downing (enhancements so the GUI will work entirely from a single
 *         jar)
 */
public class GuiClient extends JFrame implements ClientType,
		ServiceSelectedListener {
	/**
	 * The property file that contains the main properties that can be used to
	 * configure the application.
	 */
	private static final String PROPERTY_FILE = "SwordClient.properties";

	/**
	 * Label for the onBehalfOf property file label.
	 */
	private static final String ON_BEHALF_OF = "onBehalfOf";

	/**
	 * The dialog to get details for the service location.
	 */
	private ServiceDialog serviceDialog;

	/**
	 * The post dialog. This is used to determine which file should be posted.
	 */
	private PostDialog postDialog;

	/**
	 * The main panel that holds the service details and the message panel.
	 */
	private MainPanel mainPanel = null;

	/**
	 * The action that posts data to a remote collection.
	 */
	private PostAction postAction = null;

	/**
	 * The debug menu item.
	 */
	JCheckBoxMenuItem debug = null;

	/**
	 * Action that processes requests to access service documents.
	 */
	Action serviceAction = null;

	/**
	 * List of properties.
	 */
	Properties props = null;

	/**
	 * The connection to the client.
	 */
	private Client swordclient;

	/**
	 * The logger.
	 */
	private static Logger log = Logger.getLogger(GuiClient.class);

	private File propFile;

	/**
	 * Create a new instance of the tool.
	 */
	public GuiClient() {
		super("SWORD Demonstration Client");
	}

	/**
	 * Load the properties from a file.
	 */
	private void loadProperties() {
		log.debug("Loading props");
		InputStream stream = null;
		try {
			props = new Properties();
			URL propUrl =
                  Thread.currentThread().getContextClassLoader().getResource(PROPERTY_FILE);
            log.debug("The property file url is: " + propUrl);
            if (propUrl == null) {
				throw new IOException("Could not find properties file.");
			}
			if ("file".equals(propUrl.getProtocol())) {
				propFile = new File(propUrl.toURI());
			} else {
				propFile = new File(PROPERTY_FILE);
				FileUtils.copyURLToFile(propUrl, propFile);
			}
			stream = new FileInputStream(propFile);
			props.load(stream);

		} catch (IOException ioe) {
			log.error("Unable to load property file");
			JOptionPane.showMessageDialog(GuiClient.this,
					"Unable to load properties file " + ioe.getMessage(),
					"Properties", JOptionPane.ERROR_MESSAGE);
		} catch (URISyntaxException e) {
			throw new RuntimeException(
					"Most unexpectedly, a file URL is not a URI.", e);
		} finally {
			IOUtils.closeQuietly(stream);
		}
		log.info("Loaded props");
	}

	/**
	 * Process the core properties that will affect the client. This will
	 * currently set the proxyHost value, if it is set.
	 */
	private void processProperties() {
		if (props != null) {
			String value = props.getProperty("proxyHost");
			log.debug("the proxy host is set to: " + value);
			if (value != null && value.trim().length() > 0) {
				try {
					URL url = new URL(value);
					int port = url.getPort();
					if (port == -1) {
						port = 80;
					}

					log.debug("host is : " + url.getHost());
					swordclient.setProxy(url.getHost(), port);
				} catch (MalformedURLException mue) {
					JOptionPane.showMessageDialog(GuiClient.this,
							"Unable to set Proxy Host " + mue.getMessage(),
							"Properties", JOptionPane.ERROR_MESSAGE);
				}

			} else {
				swordclient.clearProxy();
			}

		}

	}

	/**
	 * Run the client. This is the main entry point into this GUI client. The
	 * client should process the options and start running.
	 * 
	 * @param options
	 *            The list of options extracted from the command line.
	 */
	public void run(ClientOptions options) {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				saveProperties();
				log.debug("Exiting.");
				System.exit(0);
			}
		});

		// add the menus
		JMenuBar menubar = new JMenuBar();
		Action quitAction = new QuitAction();
		serviceAction = new AddServiceAction();
		postAction = new PostAction();

		JMenu fileMenu = new JMenu("File");
		fileMenu.add(serviceAction);
		fileMenu.add(postAction);
		fileMenu.addSeparator();
		fileMenu.add(quitAction);

		menubar.add(fileMenu);

		JMenu optionsMenu = new JMenu("Options");
		debug = new JCheckBoxMenuItem(new DebugAction());
		optionsMenu.add(debug);
		optionsMenu.add(new EditPropertiesAction());
        optionsMenu.add(new ValidationInfoAction());
        
		menubar.add(optionsMenu);

		JMenu actionMenu = new JMenu("Help");
		actionMenu.add(new HelpAction());
		actionMenu.add(new AboutAction());
		menubar.add(actionMenu);

		setJMenuBar(menubar);

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		add(toolbar, BorderLayout.PAGE_START);

		toolbar.add(serviceAction);
		toolbar.add(postAction);
		Container c = getContentPane();
		log.debug("Creating main panel ...");
		mainPanel = new MainPanel(options.isNoCapture());
		c.add(mainPanel);

		log.debug("Initialising client ...");
		swordclient = new Client();
		log.debug("Loading props ...");
		loadProperties();
		processProperties();

		pack();
		setVisible(true);
	}

	/**
	 * Save the properties to a file.
	 */
	private void saveProperties() {
		// if the properties is not null then save it to file
        if (props != null && propFile != null) {
			OutputStream out = null;
			try {
				out = new FileOutputStream(propFile);
				log.debug("saving to... " + propFile);
				props.store(out, null);
			} catch (FileNotFoundException e) {
				log.error("Unable to store the file: " + e.getMessage(), e);
			} catch (IOException e) {
				log.error("Error storing the file: " + e.getMessage(), e);
			} finally {
				IOUtils.closeQuietly(out);
			}
		} else {
			log.warn("Either props: " + props + " or prop file: " + propFile
					+ " were null - not saving.");
		}
	}

	/**
	 * Set the enabled status for the service and post actions.
	 * 
	 * @param enabled
	 *            The status.
	 */
	private void enableActions(boolean enabled) {
		serviceAction.setEnabled(enabled);
		postAction.setEnabled(enabled);
	}

	/**
	 * Initialise the server connection information. If there is a username and
	 * password, the basic credentials will also be set. Otherwise, the
	 * credentials will be cleared.
	 * 
	 * @param location
	 *            The location to connect to. This is a URL, of the format,
	 *            http://a.host.com:port/. The host name and port number will be
	 *            extracted. If the port is not specified, a default port of 80
	 *            will be used.
	 * @param username
	 *            The username. If this is null or an empty string, the basic
	 *            credentials will be cleared.
	 * @param password
	 *            The password. If this is null or an empty string, the basic
	 *            credentials will be cleared.
	 * 
	 * @throws MalformedURLException
	 *             if there is an error processing the URL.
	 */
	private void initialiseServer(String location, String username,
			String password) throws MalformedURLException {
		URL url = new URL(location);
		int port = url.getPort();
		if (port == -1) {
			port = 80;
		}

		swordclient.setServer(url.getHost(), port);

		if (username != null && username.length() > 0 && password != null
				&& password.length() > 0) {
			swordclient.setCredentials(username, password);
		} else {
			swordclient.clearCredentials();
		}
        swordclient.setUserAgent(ClientConstants.SERVICE_NAME);
	}

	/***************************************************************************
	 * 
	 * Actions
	 * 
	 **************************************************************************/

	/**
	 * Action to quit the application.
	 * 
	 * @author Neil Taylor
	 */
	protected class QuitAction extends AbstractAction {
		/**
		 * Create the Quit action.
		 */
		public QuitAction() {
			super("Quit");
			this.putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
					KeyEvent.VK_Q, java.awt.Toolkit.getDefaultToolkit()
							.getMenuShortcutKeyMask()));
		}

		/**
		 * Exit the application. This method does not confirm the exit.
		 * 
		 * @param event
		 *            The event information for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			saveProperties();
			System.exit(0);
		}
	}

	/**
	 * Action to quit the application.
	 * 
	 * @author Neil Taylor
	 */
	protected class EditPropertiesAction extends AbstractAction {
		/**
		 * Create the Edit Properties action.
		 */
		public EditPropertiesAction() {
			super("Edit Properties");
		}

		/**
		 * Exit the application. This method does not confirm the exit.
		 * 
		 * @param event
		 *            The event information for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			PropertiesDialog dialog = new PropertiesDialog(GuiClient.this,
					props);
			dialog.show();
			processProperties();
		}
	}

	/**
	 * Action to access a service document and process the results.
	 * 
	 * @author Neil Taylor
	 */
	protected class AddServiceAction extends AbstractAction {
		/**
		 * Create a new instance.
		 */
		public AddServiceAction() {
			super("Add Service");
			ClassLoader loader = this.getClass().getClassLoader();
			URL s = loader.getResource("images/AddServiceButton.gif");
			Icon icon = new ImageIcon(s);
			putValue(Action.SMALL_ICON, icon);
			putValue(Action.SHORT_DESCRIPTION, "Add Service");
		}

		/**
		 * Start the process to access the service document. This launches the
		 * dialog and then starts a worker thread to access the remote service
		 * document.
		 */
		public void actionPerformed(ActionEvent event) {
			initialiseServiceDialog();
			int result = serviceDialog.show();

			if (result != JOptionPane.OK_OPTION) {
				return;
			}

			final String location = serviceDialog.getLocation();
			if (location == null || location.length() == 0) {
				JOptionPane.showMessageDialog(GuiClient.this,
						"You did not specify a URL", "Service Access Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// create the worker process that will run the process.
			SwingWorker<String, String> worker = new SwingWorker<String, String>() {

				/**
				 * Run the thread.
				 */
				@Override
				protected String doInBackground() throws Exception {
					try {
						enableActions(false);
						setCursor(new Cursor(Cursor.WAIT_CURSOR));

						String username = serviceDialog.getUsername();
						String password = serviceDialog.getPassword();

						initialiseServer(location, username, password);

						publish("Requesting the document from " + location);

						ServiceDocument document = swordclient
								.getServiceDocument(location, serviceDialog
										.getOnBehalfOf());
						publish("Got the document");
						Status status = swordclient.getStatus();
						publish("The status is: " + status);

                        SwordValidationInfo info = swordclient.getLastUnmarshallInfo();
                        if( info != null && info.getType() == SwordValidationInfoType.VALID)
                        {
                            publish("The document was valid");
                        }
                        else if( info != null )
                        {
                            publish("This document did not validate.");
                            StringBuffer buffer = new StringBuffer();
                            info.createString(info, buffer, " ");
                            publish(buffer.toString());
                        }

						if (status.getCode() == 200) {
							mainPanel.processServiceDocument(location, document);
							mainPanel.addMessage(document.marshall());
							publish("Data received for location: " + location);
						} else {
							JOptionPane.showMessageDialog(GuiClient.this,
									"Unable to access resource. Status is: "
											+ status.toString(),
									"Service Access",
									JOptionPane.WARNING_MESSAGE);
						}
					} catch (MalformedURLException ex) {
						JOptionPane.showMessageDialog(GuiClient.this,
								"There is an error with the URL. "
										+ ex.getMessage(),
								"Service Access Error",
								JOptionPane.ERROR_MESSAGE);
						ex.printStackTrace();
					} catch (SWORDClientException sce) {
						JOptionPane.showMessageDialog(GuiClient.this,
								"There was an error accessing the resource. "
										+ sce.getMessage(),
								"Service Access Error",
								JOptionPane.ERROR_MESSAGE);
						sce.printStackTrace();
					}

					return "Finished";
				}

				/**
				 * Called when the worker thread is complete.
				 */
				@Override
				protected void done() {
					enableActions(true);
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}

				/**
				 * Process the output from the worker thread.
				 * 
				 * @param chunks
				 *            The list of output to show.
				 */
				@Override
				protected void process(List<String> chunks) {
					for (String row : chunks) {
						String message = "status: " + row;
						mainPanel.addMessage(message);
						mainPanel.setStatus(message);
					}
				}
			};

			worker.execute();
		}

		/**
		 * Initialise the service dialog.
		 */
		private void initialiseServiceDialog() {
			if (serviceDialog == null) {
				serviceDialog = new ServiceDialog(GuiClient.this);
			}

			String value = props.getProperty("serviceurls");
			if (value != null) {
				String[] services = value.split(",");
				serviceDialog.addServiceUrls(services);
			}

			value = props.getProperty("users");
			if (value != null) {
				String[] users = value.split(",");
				serviceDialog.addUserIds(users);
			}

			value = props.getProperty(ON_BEHALF_OF);
			if (value != null) {
				String[] users = value.split(",");
				serviceDialog.addOnBehalfOf(users);
			}
		}
	}

    /**
	 * Action to process the toggle to show and hide the debug panel.
	 *
	 * @author Neil Taylor
	 */
	protected class ValidationInfoAction extends AbstractAction {
		/**
		 * Create a new instance.
		 */
		public ValidationInfoAction() {
			super("Show Last Validation Info");
		}

		/**
		 * Handle the action. Update the debug status, based on the value in the
		 * debug menu item.
		 *
		 * @param event
		 *            The event.
		 */
		public void actionPerformed(ActionEvent event) {
			JOptionPane.showOptionDialog(GuiClient.this,
	           createPanel(),
	           "View Validation Info",
	           JOptionPane.OK_OPTION,
	           JOptionPane.INFORMATION_MESSAGE,
	           null, new String[] { "OK" }, "OK");
            
		}
        
        private JPanel createPanel()
        {
            JPanel panel = new JPanel();
            
            JTextArea text = new JTextArea();
            //text.setAutoscrolls(true);

            JScrollPane areaScrollPane = new JScrollPane(text);
            areaScrollPane.setVerticalScrollBarPolicy(
		             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            areaScrollPane.setPreferredSize(new Dimension(500, 400));
            
            if( swordclient != null ) 
            {
               SwordValidationInfo info = swordclient.getLastUnmarshallInfo();
               if( info != null )
               {
                  StringBuffer buffer = new StringBuffer();
                  info.createString(info, buffer, "");
                  text.setText(buffer.toString());
               }
               else
               {
                   text.setText("There is no validaiton information to display");
               }
            }
            else
            {
               text.setText("There is no validaiton information to display");
            }

            panel.add(areaScrollPane, BorderLayout.CENTER);
            panel.setSize(500, 400);
            return panel;
        }




	}


	/**
	 * Action to process the toggle to show and hide the debug panel.
	 * 
	 * @author Neil Taylor
	 */
	protected class DebugAction extends AbstractAction {
		/**
		 * Create a new instance.
		 */
		public DebugAction() {
			super("Show Debug Panel");
		}

		/**
		 * Handle the action. Update the debug status, based on the value in the
		 * debug menu item.
		 * 
		 * @param event
		 *            The event.
		 */
		public void actionPerformed(ActionEvent event) {
			boolean setDebug = debug.isSelected();
			mainPanel.showDebugTab(setDebug);
		}
	}

	/**
	 * Controlling action to post a file to the server.
	 * 
	 * @author Neil Taylor
	 */
	protected class PostAction extends AbstractAction {
		/**
		 * The collection location to post to.
		 */
		private String collection = null;

		/**
		 * Create a post action.
		 */
		public PostAction() {
			super("Post");
			ClassLoader loader = this.getClass().getClassLoader(); 
			URL url = loader.getResource("images/PostButton.gif");
			Icon icon = new ImageIcon(url);
			putValue(Action.SMALL_ICON, icon);
			putValue(Action.SHORT_DESCRIPTION, "Post file");
		}

		/**
		 * Display the post dialog to run the post process.
		 * 
		 * @param event
		 *            The event.
		 */
		public void actionPerformed(ActionEvent event) {
			initialisePostDialog();

			int result = postDialog.show();
			if (result == JOptionPane.OK_OPTION) {
				// create the worker process that will run the process.
				SwingWorker<String, String> worker = new SwingWorker<String, String>() {

					/**
					 * Run the thread.
					 */
					@Override
					protected String doInBackground() throws Exception {
						enableActions(false);
						setCursor(new Cursor(Cursor.WAIT_CURSOR));

						PostDestination[] destinations = postDialog
								.getDestinations();

						String location;
						String username;
						String password;
						for (PostDestination destination : destinations) {
							try {
								location = destination.getUrl();
								username = destination.getUsername();
								password = destination.getPassword();
								initialiseServer(location, username, password);

								if (username != null && username.length() > 0
										&& password != null
										&& password.length() > 0) {
									publish("Setting the username/password: "
											+ username + " " + password);
									swordclient.setCredentials(username,
											password);
								} else {
									swordclient.clearCredentials();
								}

								PostMessage message = new PostMessage();
								message.setDestination(location);
								message.setFilepath(postDialog.getFile());
								message.setFiletype(postDialog.getFileType());
								message.setFormatNamespace(postDialog
										.getFormatNamespace());
								message.setUseMD5(postDialog.useMd5());
								message.setVerbose(postDialog.useVerbose());
								message.setOnBehalfOf(destination
										.getOnBehalfOf());
								message.setNoOp(postDialog.useNoOp());
								message.setChecksumError(postDialog
										.corruptMD5());
								message.setCorruptRequest(postDialog
										.corruptRequest());
                                message.setUserAgent(ClientConstants.SERVICE_NAME);

								publish("Posting file to: " + location);

								DepositResponse document = swordclient
										.postFile(message);
								Status status = swordclient.getStatus();
								publish("The status is: " + status);

                                SwordValidationInfo info = swordclient.getLastUnmarshallInfo();
                                if( info != null &&
                                    info.getType() == SwordValidationInfoType.VALID)
                                {
                                   publish("The document was valid");
                                }
                                else if( info != null )
                                {
                                   publish("This document did not validate.");
                                   StringBuffer buffer = new StringBuffer();
                                   info.createString(info, buffer, " ");
                                   publish(buffer.toString());
                                }

								if (status.getCode() == 201
										|| status.getCode() == 202) {
									mainPanel.processDespositResponse(location,
											document);
									mainPanel.addMessage(document.marshall());
									publish("Data received for location: "
											+ location);
								} else {
									publish("Unable to post file to: "
											+ location);
									mainPanel.addMessage(document.marshall());

                                    // build up the error message, taking into
                                    // account the exception condition.
                                    String outputMessage;
                                    try{
                                        SWORDErrorDocument errorDoc = document.getErrorDocument();
                                        outputMessage = "Unable to post file to "
													+ location
													+ ".\r\nStatus is: "
													+ status.toString()
													+ ".\r\nThe Error URI is: "
													+ errorDoc.getErrorURI()
                                                    + "\r\nSummary is: "
                                                    + errorDoc.getSummary();
                                    }catch (SWORDException se){
                                        outputMessage = se.getMessage();
                                    }

                                    // display the error - using the string created above
									JOptionPane.showMessageDialog(
											GuiClient.this,
											outputMessage,
											"Post File",
											JOptionPane.WARNING_MESSAGE);
								}
							} catch (MalformedURLException ex) {
								publish("Unable to access resource. Error with URL.");
								JOptionPane.showMessageDialog(GuiClient.this,
										"There is an error with the URL. "
												+ ex.getMessage(),
										"Service Access Error",
										JOptionPane.ERROR_MESSAGE);
							} catch (SWORDClientException sce) {
								publish("Unable to access resource.");
								JOptionPane.showMessageDialog(GuiClient.this,
										"There was an error accessing the resource. "
												+ sce.getMessage(),
										"Service Access Error",
										JOptionPane.ERROR_MESSAGE);
							}
						}

						return "Finished";
					}

					/**
					 * Called when the worker thread is complete.
					 */
					@Override
					protected void done() {
						enableActions(true);
						setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					}

					/**
					 * Process the output from the worker thread.
					 * 
					 * @param chunks
					 *            The list of output to show.
					 */
					@Override
					protected void process(List<String> chunks) {
						for (String row : chunks) {
							String message = "status: " + row;
							mainPanel.addMessage(message);
							mainPanel.setStatus(message);
						}
					}
				};

				worker.execute();
			}

		}

		/**
		 * Called when there has been a new selection selected in the service
		 * dialog. This value is stored and used to set the currently selected
		 * collection in any post operation.
		 * 
		 * @param collection
		 *            The url of the post area of a collection.
		 */
		public void setCollection(String collection) {
			this.collection = collection;
		}

		/**
		 * Initialise the post dialog. Create it if necessary and then set any
		 * values from the property store.
		 */
		protected void initialisePostDialog() {
			if (postDialog == null) {
				postDialog = new PostDialog(GuiClient.this);
			}

			// add any known locations from the list of collections in the
			// service panel.
			String[] locations = mainPanel.getCollectionLocations();
			postDialog.addDepositUrls(locations);

			String value = props.getProperty("depositurls");
			if (value != null) {
				String[] services = value.split(",");
				postDialog.addDepositUrls(services);
			}

			value = props.getProperty("users");
			if (value != null) {
				String[] users = value.split(",");
				postDialog.addUserIds(users);
			}

			value = props.getProperty("formatNamespaceList");
			if (value != null) {
				String[] namespaces = value.split(",");
				postDialog.addFormatNamespaces(namespaces);
			}

			value = props.getProperty(ON_BEHALF_OF);
			if (value != null) {
				String[] users = value.split(",");
				postDialog.addOnBehalfOf(users);
			}

			value = props.getProperty("files");
			if (value != null) {
				String[] files = value.split(",");
				postDialog.addFiles(files);
			}

			value = props.getProperty("fileTypes");
			if (value != null) {
				String[] fileTypes = value.split(",");
				postDialog.addFileTypes(fileTypes);
			}

			if (collection != null) {
				// set the current collection
				log.debug("setting collection: " + collection);
				postDialog.setDepositLocation(collection);
			}
		}
	}

	/**
	 * Action that process the About operation.
	 * 
	 * @author Neil Taylor
	 */
	protected static class AboutAction extends AbstractAction {
		/**
		 * Create a new instance.
		 */
		public AboutAction() {
			super("About");
		}

		/**
		 * Process the action.
		 */
		public void actionPerformed(ActionEvent event) {
			JOptionPane
					.showMessageDialog(
							null,
							"Demonstration client for SWORD Project - supporting SWORD Profile 1.3\n"
									+ "Copyright 2007-2009 CASIS, University of Wales Aberystwyth\n\n"
									+ "Version "
									+ ClientConstants.CLIENT_VERSION, "About",
							JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Action that process the Help operation.
	 * 
	 * @author Neil Taylor
	 */
	protected static class HelpAction extends AbstractAction {
		/**
		 * Create a new instance.
		 */
		public HelpAction() {
			super("Help");
		}

		/**
		 * Display the help information.
		 * 
		 * @param event
		 *            The event that generated this call.
		 */
		public void actionPerformed(ActionEvent event) {

			try {
				File helpDir = File.createTempFile("swordHelp", null);
				if (!helpDir.delete()) {
					throw new IOException("Couldn't create tmp dir: " + helpDir);
				}
				if (!helpDir.mkdirs()) {
					throw new IOException("Couldn't create tmp dir: " + helpDir);
				}
				extractHelp(helpDir);
				String osname = System.getProperty("os.name");
				log.info("osname is: " + osname);
				String runCmd = "";
				if ("Mac OS X".equals(osname)) {
					runCmd = "open";
				} else if ("Windows XP".equals(osname)) {
					runCmd = "rundll32 url.dll,FileProtocolHandler";
				} else if ("Linux".equals(osname)) {
					// Take a punt on firefox!
					runCmd = "firefox";
				} else {
					log.error(osname + " not supported.");
				}

				String helpFile = helpDir.getCanonicalPath() + File.separator
						+ "index.html";
				Runtime.getRuntime().exec(runCmd + " " + helpFile);
			} catch (IOException ioe) {
				log.error("Error accessing help files");
				ioe.printStackTrace();
			} catch (URISyntaxException e) {
				log.error("Error accessing help files");
				e.printStackTrace();
			}
		}

		/**
		 * Here be dragons. Extracting the help resources from a jar file to a
		 * temporary directory where the user's web browser can get at them is
		 * gnarly.
		 * 
		 * @param helpDir
		 * @throws URISyntaxException
		 * @throws IOException
		 */
		private void extractHelp(File helpDir) throws URISyntaxException,
				IOException {
			ClassLoader cl = getClass().getClassLoader();
			URL help = cl.getResource("help");
			if ("file".equals(help.getProtocol())) {
				File from = new File(help.toURI());
				FileUtils.copyDirectory(from, helpDir);
			} else if ("jar".equals(help.getProtocol())) {
				// This is ugly. If there's a way round, I'd love to know about
				// it.

				// Strip between 'jar:file:'
				log.debug("Help url: " + help);
				String jarLoc = help.toString().substring(9,
						help.toString().lastIndexOf("!"));
				File f = new File(jarLoc);
				if (!f.exists()) {
					log
							.error("Cannot display help - can't find help files to make a local temp copy");
				}
				JarFile jarFile = new JarFile(jarLoc);
				for (Enumeration<JarEntry> entries = jarFile.entries(); entries
						.hasMoreElements();) {
					JarEntry je = entries.nextElement();
					if (je.getName().startsWith("help/")) {
						log.debug(je.getName() + " | Directory? "
								+ je.isDirectory());
						// Trim the 'help/' off and fix up the file separators
						String filename = je.getName().substring(5).replaceAll(
								"/", File.separator);
						File destination = new File(helpDir, filename);
						File directory = je.isDirectory() ? destination
								: destination.getParentFile();
						log.debug("Creating " + directory
								+ " and copying resource to " + destination);
						if (!(directory.exists() || directory.mkdirs())) {
							throw new IOException(
									"Problem creating temp help directory, couldn't create: "
											+ directory);
						}
						if (!je.isDirectory()) {
							FileUtils.copyURLToFile(cl
									.getResource(je.getName()), destination);
						}
					}
				}
			} else {
				throw new RuntimeException(
						"Don't know how to unpack help files from " + help);
			}
		}
	}

	/**
	 * Respond to the notification that a collection has been selected in the
	 * Service panel.
	 * 
	 * @see org.purl.sword.client.ServiceSelectedListener#selected()
	 */
	public void selected(String value) {
		postAction.setCollection(value);
	}

	/***************************************************************************
	 * Panels
	 */

	/**
	 * The main panel in the GUI application. This host a service panel, at the
	 * top, and a message panel at the bottom. Methods are provided to allow
	 * messages to be displayed in the message panel.
	 */
	protected class MainPanel extends JPanel {
		/**
		 * The message output panel. This is displayed in the tabbedMessages
		 * pane.
		 */
		private MessageOutputPanel messages = null;

		/**
		 * The service panel. This displayes the list of services and references
		 * to posted files.
		 */
		private ServicePanel services = null;

		/**
		 * The tabbed pane that contains the messages and debug panels.
		 */
		private JTabbedPane tabbedMessages = null;

		/**
		 * The debug output panel. This is displayed in the tabbedMessages pane.
		 */
		private MessageOutputPanel debugPanel = null;

		/**
		 * The status part of the screen.
		 */
		private JLabel statusLabel = null;

		/**
		 * Create a new instance.
		 * 
		 * @param noCaptureOutput
		 *            If true, the System.out and System.err streams will not be
		 *            captured and shown in a debug panel. Otherwise, the output
		 *            will be captured.
		 */
		public MainPanel(boolean noCaptureOutput) {
			super(new BorderLayout());
			log.debug("Constructing MainPanel ...");
			services = new ServicePanel();
			services.setServiceSelectedListener(GuiClient.this);

			messages = new MessageOutputPanel();
			debugPanel = new MessageOutputPanel();
			DebugOutputStream output = new DebugOutputStream(debugPanel);

			if (!noCaptureOutput) {
				log.debug("Capturing output ...");
				System.setErr(new java.io.PrintStream(output));
				System.setOut(new java.io.PrintStream(output));
				// reset the log4j tool because the streams have changed.
				PropertyConfigurator.configure(getClass().getClassLoader()
						.getResource(ClientConstants.LOGGING_PROPERTY_FILE));
			}

			tabbedMessages = new JTabbedPane();
			tabbedMessages.addTab("Messages", messages);

			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					services, tabbedMessages);
			splitPane.setOneTouchExpandable(true);
			splitPane.setResizeWeight(0.5);
			splitPane.setDividerLocation(300);

			statusLabel = new JLabel(" ");
			add(splitPane, BorderLayout.CENTER);
			add(statusLabel, BorderLayout.SOUTH);
		}

		/**
		 * Show or hide the debug panel.
		 * 
		 * @param enabled
		 *            True if the debug panel should be shown. False if the
		 *            panel should be hidden.
		 */
		public void showDebugTab(boolean enabled) {
			if (enabled) {
				if (tabbedMessages.getTabCount() == 1) {
					tabbedMessages.addTab("Debug", debugPanel);
					tabbedMessages.setSelectedComponent(debugPanel);
				}
			} else {
				if (tabbedMessages.getTabCount() == 2) {
					tabbedMessages.remove(debugPanel);
				}
			}
		}

		/**
		 * Add a message to the main panel.
		 * 
		 * @param message
		 *            The message to display.
		 */
		public void addMessage(String message) {
			messages.addMessage(message);
		}

		/**
		 * Process the service document and display the details in the service
		 * panel.
		 * 
		 * @param url
		 *            The original URL that was accessed to obtain the service
		 *            document.
		 * @param service
		 *            The service document.
		 */
		public void processServiceDocument(String url, 
                                           ServiceDocument service) {
			services.processServiceDocument(url, service);
		}

		/**
		 * Process a deposit response and display the information in the service
		 * panel.
		 * 
		 * @param response
		 *            The DepositResponse to process.
		 */
		public void processDespositResponse(String url, 
                                            DepositResponse response) {
			services.processDepositResponse(url, response);
		}

		/**
		 * Get the list of collection locations.
		 * 
		 * @return A list of collection locations.
		 */
		public String[] getCollectionLocations() {
			return services.getCollectionLocations();
		}

		/**
		 * Set the status label on the panel.
		 * 
		 * @param statusMessage
		 */
		public void setStatus(String statusMessage) {
			statusLabel.setText(statusMessage);
		}
	}
}
