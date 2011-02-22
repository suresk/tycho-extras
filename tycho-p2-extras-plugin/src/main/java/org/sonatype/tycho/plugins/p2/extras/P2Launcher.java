package org.sonatype.tycho.plugins.p2.extras;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.OsgiBundleProject;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.equinox.EquinoxRuntimeLocator;
import org.sonatype.tycho.equinox.launching.DefaultEquinoxInstallationDescription;
import org.sonatype.tycho.equinox.launching.EquinoxInstallation;
import org.sonatype.tycho.equinox.launching.EquinoxInstallationDescription;
import org.sonatype.tycho.equinox.launching.EquinoxInstallationFactory;
import org.sonatype.tycho.equinox.launching.EquinoxLauncher;
import org.sonatype.tycho.equinox.launching.internal.EquinoxLaunchConfiguration;

/**
 * Convenience wrapper around {@link Commandline} to run Eclipse applications from tycho-p2-runtime
 * 
 * @author igor
 */
@Component( role = P2Launcher.class, instantiationStrategy = "per-lookup" )
public class P2Launcher
{
    @Requirement
    private Logger logger;

    @Requirement
    private EquinoxInstallationFactory installationFactory;

    @Requirement
    private EquinoxLauncher launcher;

    @Requirement
    private EquinoxRuntimeLocator runtimeLocator;

    @Requirement( role = TychoProject.class, hint = ArtifactKey.TYPE_ECLIPSE_PLUGIN )
    private OsgiBundleProject osgiBundle;

    private File workingDirectory;

    private String applicationName;

    private final List<String> vmargs = new ArrayList<String>();

    private final List<String> args = new ArrayList<String>();

    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }

    public void setApplicationName( String applicationName )
    {
        this.applicationName = applicationName;
    }

    public void addArguments( String... args )
    {
        for ( String arg : args )
        {
            this.args.add( arg );
        }
    }

    public void addVMArguments( String... vmargs )
    {
        for ( String vmarg : vmargs )
        {
            this.vmargs.add( vmarg );
        }
    }

    public int execute( int forkedProcessTimeoutInSeconds )
    {
        try
        {
            File installationFolder = newTemporaryFolder();

            try
            {
                EquinoxInstallationDescription description = new DefaultEquinoxInstallationDescription();

                List<File> locations = runtimeLocator.getRuntimeLocations();

                for ( File location : locations )
                {
                    if ( location.isDirectory() )
                    {
                        for ( File file : new File( location, "plugins" ).listFiles() )
                        {
                            addBundle( description, file );
                        }
                    }
                    else
                    {
                        addBundle( description, location );
                    }
                }

                EquinoxInstallation installation =
                    installationFactory.createInstallation( description, installationFolder );

                EquinoxLaunchConfiguration launchConfiguration = new EquinoxLaunchConfiguration( installation );
                launchConfiguration.setWorkingDirectory( workingDirectory );

                // logging

                if ( logger.isDebugEnabled() )
                {
                    launchConfiguration.addProgramArguments( "-debug", "-consoleLog" );
                }

                // application and application arguments

                launchConfiguration.addProgramArguments( "-nosplash", "-application", applicationName );

                launchConfiguration.addProgramArguments( true, args.toArray( new String[args.size()] ) );

                return launcher.execute( launchConfiguration, forkedProcessTimeoutInSeconds );
            }
            finally
            {
                FileUtils.deleteDirectory( installationFolder );
            }
        }
        catch ( Exception e )
        {
            // TODO better exception?
            throw new RuntimeException( e );
        }
    }
    
    public void clearArgs(){
	    args.clear();
	    vmargs.clear();
    }

    private void addBundle( EquinoxInstallationDescription description, File file )
    {
        ArtifactKey key = osgiBundle.readArtifactKey( file );
        if ( key != null )
        {
            description.addBundle( key, file );
        }
    }

    private File newTemporaryFolder()
        throws IOException
    {
        File tmp = File.createTempFile( "tycho-p2-runtime", ".tmp" );
        tmp.delete();
        tmp.mkdir(); // anyone got any better idea?
        return tmp;
    }
}
