package com.cordys.coe.util.swing;

import com.cordys.coe.util.cgc.CGCFactory;
import com.cordys.coe.util.cgc.CordysGatewayClientException;
import com.cordys.coe.util.cgc.ICordysGatewayClient;
import com.cordys.coe.util.cgc.config.CGCAuthenticationFactory;
import com.cordys.coe.util.cgc.config.CGCConfigFactory;
import com.cordys.coe.util.cgc.config.IAuthenticationConfiguration;
import com.cordys.coe.util.cgc.config.ICGCConfiguration;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.apache.log4j.Logger;

/**
 * This dialog can be used to create a connection to Cordys.
 * 
 * @author pgussow
 */
public class CGCLoginDialog extends javax.swing.JDialog
{
    /**
     * Holds the logger that is used.
     */
    private static final Logger LOG = Logger.getLogger(CGCLoginDialog.class);
    /**
     * Holds the actual connection to use.
     */
    private ICordysGatewayClient m_cgcConnection;
    /**
     * Variables declaration - do not modify.
     */
    private LocalCGCLoginPanel m_clpLogin;
    /**
     * Holds the parent frame.
     */
    private Frame m_fParentFrame;
    /** Holds whether or not the user details should be parsed. Default is false. */
    private boolean m_autoParseGetUserDetails = false;

    /**
     * Creates new form CGCLoginDialog.
     * 
     * @param parent Holds the parent frame.
     * @param modal Whether or not the dialog should be shown as a modal dialog.
     */
    public CGCLoginDialog(java.awt.Frame parent, boolean modal)
    {
        this(parent, modal, false);
    }

    /**
     * Creates new form CGCLoginDialog.
     * 
     * @param parent The parent
     * @param modal The modal
     * @param autoParseGetUserDetails Whether or not the user details should be parsed.
     */
    public CGCLoginDialog(java.awt.Frame parent, boolean modal, boolean autoParseGetUserDetails)
    {
        super(parent, modal);
        m_autoParseGetUserDetails = autoParseGetUserDetails;
        m_fParentFrame = parent;

        initComponents();

        setTitle("Select the connection for Cordys");

        Toolkit tk = Toolkit.getDefaultToolkit();
        setLocation((int) ((tk.getScreenSize().getWidth() - getWidth()) / 2),
                (int) ((tk.getScreenSize().getHeight() - getHeight()) / 2));
    }

    /**
     * This method gets the connection to use.
     * 
     * @return The connection to use.
     */
    public ICordysGatewayClient getConnection()
    {
        return m_cgcConnection;
    }

    /**
     * This method creates and returns the connection based on the filled in configuration.
     * 
     * @return The created connection.
     * @throws CordysGatewayClientException In case any exception occurred.
     */
    protected ICordysGatewayClient createConnection() throws CordysGatewayClientException
    {
        ICordysGatewayClient cgcReturn = null;

        // Create the proper configuration objects.
        IAuthenticationConfiguration acAuth = CGCAuthenticationFactory.createAuthentication(m_clpLogin.getConfiguration());
        ICGCConfiguration ccConfig = CGCConfigFactory.createConfiguration(m_clpLogin.getConfiguration());

        cgcReturn = CGCFactory.createCGC(acAuth, ccConfig);

        cgcReturn.setAutoParseGetUserDetails(m_autoParseGetUserDetails);

        return cgcReturn;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    private void initComponents()
    {
        m_clpLogin = new LocalCGCLoginPanel(true, true);
        m_clpLogin.initialize();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we)
            {
                m_clpLogin.handleCancelPressed();
            }
        });

        setName("Form"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(m_clpLogin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(m_clpLogin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)));

        pack();
    } // </editor-fold>

    /**
     * Subclass of the CGCLoginPanel to be able to react to the button presses.
     * 
     * @author pgussow
     */
    private class LocalCGCLoginPanel extends CGCLoginPanel
    {
        /**
         * Creates new form CordysLoginPanel.
         * 
         * @param bCreateButtons Whether or not the connect/cancel button should be shown.
         * @param bUseConnectionManager Whether or not the connection manager should be used.
         */
        public LocalCGCLoginPanel(boolean bCreateButtons, boolean bUseConnectionManager)
        {
            super(bCreateButtons, bUseConnectionManager);
        }

        /**
         * This method is called when the cancel button is pressed.
         * 
         * @see com.cordys.coe.util.swing.CGCLoginPanel#handleCancelPressed()
         */
        @Override
        protected void handleCancelPressed()
        {
            CGCLoginDialog.this.setVisible(false);
        }

        /**
         * This method handles the pressing of the connect button. The only thing done here is make a call to the makeConnection
         * method.
         * 
         * @see com.cordys.coe.util.swing.CGCLoginPanel#handleConnect()
         */
        @Override
        protected void handleConnect()
        {
            new Thread(new Runnable() {
                public void run()
                {
                    try
                    {
                        makeConnection();

                        if ((getConfiguration() != null) && isOk())
                        {
                            // Show the in progress Dialog
                            final InProgressDialog ipd = new InProgressDialog(null, true, "Please wait",
                                    "Building up the connection to " + m_clpLogin.getConfiguration().getServername());

                            try
                            {
                                EventQueue.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        ipd.setVisible(true);
                                    }
                                });
                                m_cgcConnection = createConnection();
                                m_cgcConnection.connect();
                                EventQueue.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        CGCLoginDialog.this.setVisible(false);
                                    }
                                });
                            }
                            catch (CordysGatewayClientException e)
                            {
                                MessageBoxUtil.showError(m_fParentFrame, "Error creating CGC: " + e.getMessage(), e);
                                LOG.error("Error handling the connect", e);
                            }
                            finally
                            {
                                if ((ipd != null) && ipd.isVisible())
                                {
                                    EventQueue.invokeLater(new Runnable() {
                                        public void run()
                                        {
                                            if (ipd.isVisible())
                                            {
                                                ipd.setVisible(false);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        MessageBoxUtil.showError(m_fParentFrame, "Error connecting CGC: " + e.getMessage(), e);
                        LOG.error("Error handling the connection", e);
                    }
                }
            }).start();
        }

        /**
         * This method is called when the test button is pressed.
         * 
         * @see com.cordys.coe.util.swing.CGCLoginPanel#handleTestPressed()
         */
        @Override
        protected void handleTestPressed()
        {
            new Thread(new Runnable() {
                public void run()
                {
                    try
                    {
                        makeConnection();

                        if ((getConfiguration() != null) && isOk())
                        {
                            // Show the in progress Dialog
                            final InProgressDialog ipd = new InProgressDialog(null, true, "Please wait",
                                    "Testing the connection to " + m_clpLogin.getConfiguration().getServername());

                            // Based on the configuration create the
                            ICordysGatewayClient cgcConnection = null;

                            try
                            {
                                EventQueue.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        ipd.setVisible(true);
                                    }
                                });

                                cgcConnection = createConnection();
                                cgcConnection.connect();
                                EventQueue.invokeAndWait(new Runnable() {
                                    public void run()
                                    {
                                        if (ipd.isVisible())
                                        {
                                            ipd.setVisible(false);
                                        }
                                    }
                                });
                                MessageBoxUtil.showInformation(m_fParentFrame, "Connection succeeded");
                            }
                            catch (CordysGatewayClientException e)
                            {
                                MessageBoxUtil.showError(m_fParentFrame, "Error testing CGC: " + e.getMessage(), e);
                                LOG.error("Error handling the test connection", e);
                            }
                            finally
                            {
                                if (cgcConnection != null)
                                {
                                    cgcConnection.disconnect();
                                }

                                if ((ipd != null) && ipd.isVisible())
                                {
                                    EventQueue.invokeLater(new Runnable() {
                                        public void run()
                                        {
                                            if (ipd.isVisible())
                                            {
                                                ipd.setVisible(false);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        MessageBoxUtil.showError(m_fParentFrame, "Error testing CGC: " + e.getMessage(), e);
                        LOG.error("Error handling the test connection", e);
                    }
                }
            }).start();
        }
    }
}
