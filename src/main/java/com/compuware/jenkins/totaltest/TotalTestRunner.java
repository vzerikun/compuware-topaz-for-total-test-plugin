package com.compuware.jenkins.totaltest;

import java.io.IOException;
import java.util.Properties;

import com.compuware.jenkins.totaltest.global.TotalTestGlobalConfiguration;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

public class TotalTestRunner
{
	private static final String TOTAL_TEST_CLI_BAT = "TotalTestCLI.bat"; //$NON-NLS-1$
	private static final String TOTAL_TEST_CLI_SH = "TotalTestCLI.sh"; //$NON-NLS-1$
	public static final String TOPAZ_CLI_WORKSPACE = "TopazCliWkspc"; //$NON-NLS-1$
	
	private static final String COMMAND_PARM = "-cmd=runtest"; //$NON-NLS-1$
	private static final String HOST_PARM = "-host="; //$NON-NLS-1$
	private static final String PORT_PARM = "-port="; //$NON-NLS-1$
	private static final String USER_PARM = "-user="; //$NON-NLS-1$
	private static final String PASSWORD_PARM = "-pw="; //$NON-NLS-1$
	private static final String PROJECT_PARM = "-project="; //$NON-NLS-1$
	private static final String TESTSUITE_PARM = "-ts=";
	private static final String JCL_PARM = "-jcl=";
	private static final String EXTERNAL_TOOLS_WS_PARM = "-externaltoolsws=";
	private static final String EXTERNAL_TOOLS_PARM = "-externaltools=";
	private static final String DATA_PARM = "-data"; //$NON-NLS-1$
	
	
	private static final String PROPERTY_FILE_SEPARATOR = "file.separator";
	
	private final TotalTestBuilder tttBuilder;
	
	public TotalTestRunner(TotalTestBuilder tttBuilder)
	{
		this.tttBuilder = tttBuilder;
	}
	
	/**
	 * Runs the Total Test CLI
	 * 
	 * @param build
	 *			  The current running Jenkins build
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param workspaceFilePath
	 *            a directory to check out the source code.
	 * @param listener
	 *            Build listener
	 * @return <code>boolean</code> if the build was successful
	 * 
	 * @throws IOException
	 * 			If an error occurred execute Total Test run.
	 * @throws InterruptedException
	 * 			If the Total Test run was interrupted.
	 */
	public boolean run(final Run<?,?> build, final Launcher launcher, final FilePath workspaceFilePath, final TaskListener listener) throws IOException, InterruptedException
	{
        ArgumentListBuilder args = new ArgumentListBuilder();
        EnvVars env = build.getEnvironment(listener);

        VirtualChannel vChannel = launcher.getChannel();
        Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
        String remoteFileSeparator = remoteProperties.getProperty(PROPERTY_FILE_SEPARATOR);
        
		boolean isShell = launcher.isUnix();
		String osFile = isShell ? TOTAL_TEST_CLI_SH : TOTAL_TEST_CLI_BAT;
		
		TotalTestGlobalConfiguration globalConfig = TotalTestGlobalConfiguration.get();
		String topazCLILocation = globalConfig.getTopazCLILocation(launcher);
        
		String cliScriotFile = topazCLILocation  + remoteFileSeparator + osFile;
		listener.getLogger().println("Topaz for Total Test CLI script file path: " + cliScriotFile); //$NON-NLS-1$
		
		FilePath cliBatchFileRemote = new FilePath(vChannel, cliScriotFile);
		listener.getLogger().println("Topaz for Total Test CLI script file remote path: " + cliBatchFileRemote.getRemote()); //$NON-NLS-1$
		
		args.add(cliBatchFileRemote.getRemote());
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + TOPAZ_CLI_WORKSPACE;
		listener.getLogger().println("Topaz for Total Test CLI workspace: " + topazCliWorkspace); //$NON-NLS-1$
		
		String host = TotalTestRunnerUtils.escapeForScript(HOST_PARM + TotalTestRunnerUtils.getHost(tttBuilder.getHostPort()), isShell);
		String port = TotalTestRunnerUtils.escapeForScript(PORT_PARM + TotalTestRunnerUtils.getPort(tttBuilder.getHostPort()), isShell);
		String user = TotalTestRunnerUtils.escapeForScript(USER_PARM + TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getUsername(), isShell);
		String password = TotalTestRunnerUtils.escapeForScript(PASSWORD_PARM + TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getPassword().getPlainText(), isShell);
		
		String projectFolder = TotalTestRunnerUtils.escapeForScript(PROJECT_PARM + tttBuilder.getProjectFolder(), isShell);
		String testSuite = TotalTestRunnerUtils.escapeForScript(TESTSUITE_PARM + tttBuilder.getTestSuite(), isShell);
		String jcl = TotalTestRunnerUtils.escapeForScript(JCL_PARM + tttBuilder.getJcl(), isShell);
		String externalToolsWS = TotalTestRunnerUtils.escapeForScript(EXTERNAL_TOOLS_WS_PARM + workspaceFilePath.getRemote(), isShell);
		String externalTool = TotalTestRunnerUtils.escapeForScript(EXTERNAL_TOOLS_PARM + "jenkins", isShell);
		String data = TotalTestRunnerUtils.escapeForScript(topazCliWorkspace, isShell);
		
		args.add(COMMAND_PARM);
		args.add(host);
		args.add(port);
		args.add(user);
		args.add(password, true);
		args.add(projectFolder);
		args.add(testSuite);
		args.add(jcl);
		args.add(externalToolsWS);
		args.add(externalTool);
		args.add(DATA_PARM, data);
		
		FilePath workDir = new FilePath (vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();

		listener.getLogger().println(osFile + " exited with exit value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$

		return exitValue == 0;
	}
}
