// .releaserc.cjs (minimal)
module.exports = {
  branches: ["develop"],
  tagFormat: "v${version}",
  plugins: [
    ["@semantic-release/commit-analyzer", { preset: "conventionalcommits" }],
    ["@semantic-release/exec", {
      prepareCmd: "bash scripts/set-gradle-version.sh ${nextRelease.version}"
    }],
    ["@semantic-release/git", {
      assets: ["gradle.properties"],
      message: "chore(release): ${nextRelease.version} [skip ci]"
    }],
    ["@semantic-release/github", {
      successComment: false,
      failComment: false,
      labels: false,
      releasedLabels: false
    }]
  ]
};
