package org.webjars;

class MavenProperties {
    final String groupId;
    final String artifactId;
    final String version;

    MavenProperties(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    // For future usage ?
    public String getVersion() {
        return version;
    }
}
