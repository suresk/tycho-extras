package org.sonatype.tycho.plugins.p2.extras;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal mirror-repository
 * @requiresProject false
 */
public class MirrorRepositoryMojo extends AbstractMojo {
	
	private static String MIRROR_ARTIFACT_NAME = "org.eclipse.equinox.p2.artifact.repository.mirrorApplication";
	
	private static String MIRROR_METADATA_NAME = "org.eclipse.equinox.p2.metadata.repository.mirrorApplication";
	
	/**
	 * @parameter expression="${sourceRepositories}"
	 * @required
	 */
	private List<String> sourceRepositories;
	
	/**
	 * @parameter default-value="repository" expression="${destinationRepository}"
	 */
	private String destinationRepository;
	
	/**
	 * @parameter expression="${destinationMetadataRepository}"
	 */
	private String destinationMetadataRepository;
	
	/**
	 * @parameter expression="${destinationArtifactRepository}"
	 */
	private String destinationArtifactRepository;
	
	/**
	 * @parameter expression="${verbose}"
	 */
	private boolean verbose;
	
	/**
	 * @parameter expression="${compare}"
	 */
	private boolean compare;
	
	/**
	 * @parameter expression="${ignoreErrors}"
	 */
	private boolean ignoreErrors;
	
    /**
     * Kill the forked process after a certain number of seconds. If set to 0, wait forever for the process, never
     * timing out.
     * 
     * @parameter expression="${p2.timeout}" default-value="0"
     */
    private int forkedProcessTimeoutInSeconds;
	
    /** @parameter expression="${project}" */
    private MavenProject project;

    /** @component */
    private P2Launcher launcher;
    
    private void configureLocalRepos() {
    	if(destinationArtifactRepository == null) {
    		destinationArtifactRepository = destinationRepository;
    	}
    	
    	if(destinationMetadataRepository == null) {
    		destinationMetadataRepository = destinationRepository;
    	}
    }

	public void execute() throws MojoExecutionException, MojoFailureException {
		configureLocalRepos();
		for(String source : sourceRepositories) {
			System.out.println(String.format("Building repository for %s.", source));
			buildRepositories(source);
		}
	}
	
	private void buildRepositories(String source) throws MojoFailureException, MojoExecutionException {
		generateArtifactRepository(source);
		generateMetadataRepository(source);
	}
	
	private void generateMetadataRepository(String source) throws MojoFailureException, MojoExecutionException {
		generateRepository(source, destinationMetadataRepository, MIRROR_METADATA_NAME);
	}
	
	private void generateArtifactRepository(String source) throws MojoFailureException, MojoExecutionException {
		generateRepository(source, destinationArtifactRepository, MIRROR_ARTIFACT_NAME);
	}
	
	private void generateRepository(String source, String destination, String applicationName) throws MojoExecutionException, MojoFailureException {
		try {
			launcher.clearArgs();
	        File targetDir = new File(destination).getCanonicalFile();
	        
            List<String> contentArgs = new ArrayList<String>();
            contentArgs.add("-source");
            contentArgs.add(source);
            
	        launcher.setWorkingDirectory(project.getBasedir());
	        launcher.setApplicationName(applicationName);
	        launcher.addArguments("-destination", targetDir.toString());
	        
	        if(verbose) {
	        	launcher.addArguments("-verbose");
	        }
	        
	        if(compare) {
	        	launcher.addArguments("-compare");
	        }
	        
	        if(ignoreErrors) {
	        	launcher.addArguments("-ignoreErrors");
	        }
	        
	        launcher.addArguments(contentArgs.toArray(new String[contentArgs.size()]));
	        
	        int result = launcher.execute(forkedProcessTimeoutInSeconds);
	        getLog().info("Done running command. Result: " + result);
	        
            if (result != 0){
                throw new MojoFailureException("P2 publisher return code was " + result);
            }
        } catch ( IOException ioe ) {
            throw new MojoExecutionException("Unable to execute the publisher", ioe);
        }
	}

}