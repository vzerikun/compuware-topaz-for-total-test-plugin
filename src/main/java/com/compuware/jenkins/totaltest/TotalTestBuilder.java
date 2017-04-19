package com.compuware.jenkins.totaltest;

import hudson.Launcher;
import hudson.Util;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.util.Collections;
import java.util.List;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link TotalTestRunner} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields to remember
 * remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. 
 *
 */
public class TotalTestBuilder extends Builder implements SimpleBuildStep
{
	private static final String COLON = ":"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	
	private String hostPort;
	private String projectFolder;
	private String credentialsId;
	private String testSuite;
	private String jcl;
	
	/**
	 * Constructor 
	 * 
	 * @param hostPort
	 * 			The host and port for running Total Test.
	 * @param credentialsId
	 * 			The user's credential id.
	 * @param projectFolder
	 * 			The name of the folder containing the Total Test project.
	 * @param testSuite
	 * 			The name of the test scenario or test suite to run.
	 * @param jcl
	 * 			The name of the JCL file to use.
	 */
	@DataBoundConstructor
	public TotalTestBuilder(String hostPort, String credentialsId, String projectFolder, String testSuite, String jcl)
	{
		this.hostPort = StringUtils.trimToEmpty(hostPort);
		this.credentialsId = StringUtils.trimToEmpty(credentialsId);
		this.projectFolder = StringUtils.trimToEmpty(projectFolder);
		this.testSuite = StringUtils.trimToEmpty(testSuite);
		this.jcl = StringUtils.trimToEmpty(jcl);
	}
	
	/**
	 * Returns the host and port for running Total Test.
	 * <p>
	 * The host and port must be specified as "host:port"
	 * 
	 * @return	The host and port for running Total Test.
	 */
	public String getHostPort()
	{
		return hostPort;
	}
	
	/**
	 * Returns the credential id of the user.
	 * 
	 * @return	The user's credential id.
	 */
	public String getCredentialsId()
	{
		return credentialsId;
	}
	
	/**
	 * Returns the folder name containing the Total Test project.
	 * 
	 * @return	The name of the folder containing the Total Test project.
	 */
	public String getProjectFolder()
	{
		return projectFolder;
	}

	/**
	 * Returns the name of the test scenario or test suite to
	 * run.
	 * <p>
	 * The test scenario or test suite must be in the specified
	 * project.
	 * 
	 * @return	The name of the test scenario or test suite to run.
	 */
	public String getTestSuite()
	{
		return testSuite;
	}
	
	/**
	 * Returns the name of the JCL file to use.
	 * <p>
	 * The JCL file must be in the specified project.
	 * 
	 * @return	The name of the JCL file to use.
	 */
	public String getJcl()
	{
		return jcl;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)
	 */
    @Override
    public void perform(final Run<?,?> build, final FilePath workspaceFilePath, final Launcher launcher, final TaskListener listener) throws AbortException
    {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
    	listener.getLogger().println(Messages.displayNameTotalTest());
    	
		try
		{
			validateParameters(launcher, listener, build.getParent());
			
			TotalTestRunner runner = new TotalTestRunner(this);
			boolean success = runner.run(build, launcher, workspaceFilePath, listener);
			if (success == false) //NOSONAR
			{
				throw new AbortException(Messages.totalTestFailure());
			}
			else
			{
				listener.getLogger().println(Messages.totalTestSuccess());
			}
					
		}
		catch (Exception e)
		{
			listener.getLogger().println(e.getMessage());
			throw new AbortException();
		}
    }
    
    /*
     * (non-Javadoc)
     * @see hudson.tasks.Builder#getDescriptor()
     */
    @Override
    public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl)super.getDescriptor();
    }

	/**
	 * Validates the configuration parameters.
	 * 
	 * @param launcher
	 *         An instance of <code>Launcher</code> for launching the plugin.
	 * @param listener
	 *          An instance of <code>TaskListener</code> for the build listener.
	 * @param project
	 * 			An instance of <code>Item</code> for the Jenkins project.
	 */
	public void validateParameters(final Launcher launcher, final TaskListener listener, final Item project)
	{
		if (TotalTestRunnerUtils.getLoginInformation(project, getCredentialsId()) != null)
		{
			listener.getLogger().println(Messages.username() + " = " + TotalTestRunnerUtils.getLoginInformation(project, getCredentialsId()).getUsername()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.missingParameterError(Messages.loginCredentials()));
		}

		TotalTestRunnerUtils.validateHostPort(listener, getHostPort());
		
		if (getProjectFolder().isEmpty() == false) // NOSONAR
		{
			listener.getLogger().println(Messages.project() + " = " + getProjectFolder());			
		}
		else
		{
			throw new IllegalArgumentException(Messages.missingParameterError(Messages.project()));
		}
		
		if (getTestSuite().isEmpty() == false) // NOSONAR
		{
			String lcTestSuite = getTestSuite().toLowerCase();
			if ((lcTestSuite.endsWith(".testsuite") == false) && (lcTestSuite.endsWith(".testscenario") == false)) //NOSONAR
			{
				throw new IllegalArgumentException(Messages.testSuiteError(getTestSuite()));
			}
			
			listener.getLogger().println(Messages.testsuite() + " = " + getTestSuite());			
		}
		else
		{
			throw new IllegalArgumentException(Messages.missingParameterError(Messages.testsuite()));
		}
		
		if (getJcl().isEmpty() == false) // NOSONAR
		{
			listener.getLogger().println(Messages.jcl() + " = " + getJcl());			
		}
		else
		{
			throw new IllegalArgumentException(Messages.missingParameterError(Messages.jcl()));
		}
	}
	
	/**
     * Descriptor for {@link TotalTestRunner}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See {@code src/main/resources/hudson/plugins/totatest/TotalTestRunner/*.jelly}
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> 
    {
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl()
        {
            load();
        }

		/**
		 * Validator for the 'hostPort' text field
		 * 
		 * @param value
		 *            value passed from the config.jelly "hostPort" field
		 *            
		 * @return validation message
		 * 
		 * @throws ServletException
		 * 			If an error occurred validating field.
		 */
		public FormValidation doCheckHostPort(@QueryParameter final String value) throws ServletException
		{
			String trimmedValue =  StringUtils.trimToEmpty(value);
			if (trimmedValue.isEmpty())
			{
				return FormValidation.error(Messages.checkHostPortEmptyError());
			}
			else
			{
				String[] hostPort = trimmedValue.split(COLON);
				if (hostPort.length != 2)
				{
					return FormValidation.error(Messages.checkHostPortFormatError());
				}
				else
				{
					String host = StringUtils.trimToEmpty(hostPort[0]);
					if (host.isEmpty())
					{
						return FormValidation.error(Messages.checkHostPortMissingHostError());
					}

					String port = StringUtils.trimToEmpty(hostPort[1]);
					if (port.isEmpty())
					{
						return FormValidation.error(Messages.checkHostPortMissingPortError());
					}
					else if (StringUtils.isNumeric(port) == false) //NOSONAR
					{
						return FormValidation.error(Messages.checkHostPortInvalidPorttError());
					}
				}
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'projectFolder' text field
		 * 
		 * @param value
		 *            value passed from the config.jelly "projectFolder" field
		 *            
		 * @return validation message
		 * 
		 * @throws ServletException
		 * 			If an error occurred validating field.
		 */
		public FormValidation doCheckProjectFolder(@QueryParameter final String value) throws ServletException
		{
			String trimmedValue = value.trim();
			if (trimmedValue.isEmpty() || trimmedValue.length() == 0)
			{
				return FormValidation.error(Messages.checkProjectEmptyError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'testSuite' text field
		 * 
		 * @param value
		 *            value passed from the config.jelly "testSuite" field
		 *            
		 * @return validation message
		 * 
		 * @throws ServletException
		 * 			If an error occurred validating field.
		 */

		public FormValidation doCheckTestSuite(@QueryParameter final String value) throws ServletException
		{
			String trimmedValue = value.trim();
			if (trimmedValue.isEmpty() || trimmedValue.length() == 0)
			{
				return FormValidation.error(Messages.checkTestSuiteEmptyError());
			}
			
			String lcTestSuite = trimmedValue.toLowerCase();
			if ((lcTestSuite.endsWith(".testsuite") == false) && (lcTestSuite.endsWith(".testscenario") == false)) //NOSONAR
			{
				return FormValidation.error(Messages.testSuiteError(value));
			}


			return FormValidation.ok();
		}
		
		/**
		 * Validator for the 'jcl' text field
		 * 
		 * @param value
		 *            value passed from the config.jelly "jcl" field
		 *            
		 * @return validation message
		 * 
		 * @throws ServletException
		 * 			If an error occurred validating field.
		 */
		public FormValidation doCheckJcl(@QueryParameter final String value) throws ServletException
		{
			String trimmedValue = value.trim();
			if (trimmedValue.isEmpty() || trimmedValue.length() == 0)
			{
				return FormValidation.error(Messages.checkJCLEmptyError());
			}

			return FormValidation.ok();
		}
		
		/**
		 * Validator for the 'Login Credential' field
		 * 
		 * @param value
		 *            value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 * 
		 * @throws ServletException
		 * 			If an error occurred validating field.
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter final String value) throws ServletException
		{
			if (value.equalsIgnoreCase(EMPTY_STRING))
			{
				return FormValidation.error(Messages.checkLoginCredentialError());
			}

			return FormValidation.ok();
		}
		

		/**
		 * Fills in the Login Credential selection box with applicable Jenkins credentials
		 * 
		 * @param context
		 *            Jenkins context.
		 * @param credentialsId
		 * 			  The credendtial id for the user.
		 * @param project
		 * 			  The Jenkins projec.
		 * 
		 * @return credential selections
		 * 
		 * @throws ServletException
		 * 			If an error occurred validating field.
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Jenkins context, @QueryParameter final String credentialsId, @AncestorInPath final Item project) throws  ServletException
		{
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider
					.lookupCredentials(StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
							Collections.<DomainRequirement> emptyList());

			StandardListBoxModel model = new StandardListBoxModel();

			model.add(new Option(EMPTY_STRING, EMPTY_STRING, false));

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;

				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername()
						+ (description != null ? " (" + description + ")" : EMPTY_STRING), c.getId(), isSelected)); //$NON-NLS-1$ //$NON-NLS-2$
			}

			return model;
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@SuppressWarnings("rawtypes")
		@Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass)
        {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
        public String getDisplayName()
        {
            return Messages.displayNameTotalTest();
        }

		/**
		 * The method is called when the global configuration page is submitted. In the method the data in the web form should
		 * be copied to the Descriptor's fields. To persist the fields to the global configuration XML file, the
		 * <code>save()</code> method must be called. Data is defined in the global.jelly page.
		 * 
		 * @param req
		 *            Stapler request
		 * @param formData
		 *            Form data
		 * @return TRUE if able to configure and continue to next page
		 * 
		 * @throws FormException
		 * 			If an error occurred processing configuration.
		 */
        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException
        {
            save();
            return super.configure(req,formData);
        }
    }
}
