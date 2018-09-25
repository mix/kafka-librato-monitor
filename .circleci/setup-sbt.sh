#!/bin/bash -xeu

mkdir -p ~/.sbt/1.0/plugins
echo -e "[repositories]\n  mix-ivy-releases: https://mix.artifactoryonline.com/mix/ivy-releases/, [organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]\n  mix-maven-releases: https://mix.artifactoryonline.com/mix/mvn-releases/" > ~/.sbt/repositories
echo -e "realm=Artifactory Realm\nhost=mix.artifactoryonline.com\nuser=${ARTIFACTORY_DEPLOY_USER}\npassword=${ARTIFACTORY_DEPLOY_PWD}" > ~/.sbt/.credentials
echo "credentials += Credentials(Path.userHome / \".sbt\" / \".credentials\")" > ~/.sbt/1.0/plugins/credentials.sbt
