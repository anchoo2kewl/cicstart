package ca.ualberta.physics.cicstart.cml.command;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ualberta.physics.cssdp.configuration.MacroServer;
import ca.ualberta.physics.cssdp.domain.macro.Instance;

import com.google.common.net.InetAddresses;

public class On implements Command {

	private static final Logger jobLogger = LoggerFactory
			.getLogger("JOBLOGGER");

	// the host these commands should be run on
	private final String host;
	private final String serverVar;
	private final List<CommandDefinition> cmdsToRun;
	private final String script;

	private int maxRetries = 10;
	private int retryCount = 0;

	public On(Instance instance, List<CommandDefinition> cmdsToRun,
			String script, String serverVar) {
		String thisHost = "localhost";
		try {
			thisHost = instance != null ? instance.ipAddress : InetAddress
					.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			jobLogger.error("Can get localhost host address");
		} finally {
			this.host = thisHost;
		}
		this.serverVar = serverVar;
		this.cmdsToRun = cmdsToRun;
		this.script = script;
	}

	@Override
	public void execute(CMLRuntime runtime) {
		if (retryCount > 0) {
			jobLogger.info("This is retry # " + retryCount);
		}
		boolean correctServer = false;
		try {
			// localhost and address of spawned server runs the commands
			for (InetAddress inetAddr : InetAddress.getAllByName(InetAddress
					.getLocalHost().getHostName())) {
				if (inetAddr.getHostAddress().equals(host)
						|| inetAddr.getHostName().equals(host)) {
					jobLogger.info("On: running commands for " + host);
					runtime.run(getCmdsToRun());
					correctServer = true;
					break;
				}
			}

			String cicstartServer = MacroServer.properties().getString(
					"cicstart.server.host");

			// we're not on the right host to run commands directly
			if (!correctServer) {

				boolean remoteRequested = false;
				// but we may be on the cicstart server, so request remove VM to
				// run commands
				for (InetAddress inetAddr : InetAddress
						.getAllByName(InetAddress.getLocalHost().getHostName())) {

					jobLogger.info("Found host " + inetAddr.getHostAddress());

					if (inetAddr.getHostAddress().equals(cicstartServer)
							|| inetAddr.getHostName().equals(cicstartServer)) {

						jobLogger.info("On: requesting spawned VM " + host
								+ " to run the commands via remote ssh session");

						SSHClient client = new SSHClient();
						client.addHostKeyVerifier(new PromiscuousVerifier());
						try {

							try {

								client.connect(InetAddresses.forString(host));
								KeyProvider keys = client.loadKeys(new File(
										MacroServer.properties().getString(
												"cicstart.pemfile")).getPath());
								client.authPublickey("ubuntu", keys);

								runOnRemote(client,
										"sudo apt-get -y update --fix-missing");
								runOnRemote(client,
										"sudo apt-get -y install openjdk-6-jre");
								runOnRemote(
										client,
										"curl -H CICSTART.session:\""
												+ runtime.getCICSTARTSession()
												+ "\" -H Content-Type:\"application/octet-stream\" --data-binary "
												+ "'"
												+ script.replaceAll("\\$"
														+ serverVar, "\""
														+ host + "\"")
												+ "'"
												+ " -X POST \"http://10.0.28.3/macro/api"
												// + Common.properties()
												// .getString(
												// "external.macro.api.url")
												+ "/macro.json/bin?include_jre=false&use_internal_network=true\" > client.tar.gz");

								runOnRemote(client, "tar zxvf client.tar.gz");
								runOnRemote(client, "cd bin && ./run");

							} finally {
								client.disconnect();
								client.close();
							}

						} catch (Exception e) {
							if (retryCount < maxRetries) {
								retryCount++;
								try {
									Thread.sleep(100);
								} catch (InterruptedException e1) {
									// TODO handle interrupt.
								}
								execute(runtime);
							}
						}

						remoteRequested = true;
						break;
					} else {
						jobLogger.info("... but we're not on a CICSTART server: "
								+ cicstartServer);
					}
				}

				// we're on the wrong VM so don't run anything
				if (!remoteRequested) {
					InetAddress localhost;
					localhost = InetAddress.getLocalHost();
					String localIpAddress = localhost.getHostAddress();
					String localHostName = localhost.getHostName();
					jobLogger.info("On: skipping -> This is " + localIpAddress
							+ "(" + localHostName + ")"
							+ " but these cmds are for " + host);
				}
			}

		} catch (UnknownHostException e) {
			if (retryCount < maxRetries) {
				retryCount++;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO handle interrupt.
				}
				execute(runtime);
			}
		}

	}

	private void runOnRemote(SSHClient client, String command)
			throws ConnectionException, TransportException, IOException {

		Session session = client.startSession();
		try {
			jobLogger.info("Going to Run ' " + command + "' on remote host");
			net.schmizz.sshj.connection.channel.direct.Session.Command cmd = session
					.exec(command);
			jobLogger.info("On (" + host + "): "
					+ IOUtils.readFully(cmd.getInputStream()).toString());
			cmd.join(15, TimeUnit.SECONDS);
			jobLogger.info("On (" + host + "): exit status: "
					+ cmd.getExitStatus());

		} finally {
			session.close();
		}
	}

	@Override
	public Object getResult() {
		return null;
	}

	public List<CommandDefinition> getCmdsToRun() {
		return cmdsToRun;
	}

}
