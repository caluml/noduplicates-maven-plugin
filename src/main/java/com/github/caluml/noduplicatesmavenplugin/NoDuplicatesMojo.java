package com.github.caluml.noduplicatesmavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(
	name = "checkduplicates",
	requiresDependencyCollection = ResolutionScope.RUNTIME_PLUS_SYSTEM,
	threadSafe = true,
	defaultPhase = LifecyclePhase.COMPILE)
public class NoDuplicatesMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	@Component(hint = "default")
	private DependencyGraphBuilder dependencyGraphBuilder;

	@Parameter(property = "scope")
	private String scope;


	// -------------------- Start of required but unused section
	private boolean appendOutput;
	private List<String> excludes;
	private List<String> includes;
	private String outputEncoding;
	private File outputFile;
	private String outputType;
	private String tokens;
	private boolean verbose;
	private boolean skip;
	// -------------------- End of required but unused section


	private DependencyNode rootNode;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			ArtifactFilter artifactFilter = createResolvingArtifactFilter();
			ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
			buildingRequest.setProject(project);
			rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, artifactFilter);

			Map<String, String> artifactVersions = new HashMap<>();
			int checked = recurse(artifactVersions, rootNode.getChildren(), 0, 1);
			getLog().info("Checked " + checked + " artifacts. No duplicates found");
		} catch (DependencyGraphBuilderException e) {
			throw new MojoExecutionException("Cannot build project dependency graph", e);
		}
	}

	private int recurse(Map<String, String> artifactVersions,
											List<DependencyNode> dependencyNodes,
											int checked,
											int level) {
		for (DependencyNode dependencyNode : dependencyNodes) {
			checkArtifact(dependencyNode.getArtifact(), artifactVersions, level);
			checked = recurse(artifactVersions, dependencyNode.getChildren(), checked + 1, level + 1);
		}

		return checked;
	}

	private void checkArtifact(Artifact artifact,
														 Map<String, String> artifactVersions,
														 int level) {
		String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
		String version = artifact.getVersion();
		getLog().info(spaces(level) + ": Level " + level + ": Checking " + key + " " + version);
		if (artifactVersions.containsKey(key)) {
			String existingArtifactVersion = artifactVersions.get(key);
			if (!existingArtifactVersion.equals(version)) {
				throw new RuntimeException(key + ": " + existingArtifactVersion + " != " + version);
			}
		} else {
			artifactVersions.put(key, version);
			getLog().debug("Adding " + key + " " + version);
		}
	}

	private String spaces(int spaces) {
		return String.format("%" + spaces + "s", "");
	}

	private ArtifactFilter createResolvingArtifactFilter() {
		if (scope != null) {
			getLog().debug("+ Resolving dependency tree for scope '" + scope + "'");

			return new ScopeArtifactFilter(scope);
		}

		return null;
	}

}
