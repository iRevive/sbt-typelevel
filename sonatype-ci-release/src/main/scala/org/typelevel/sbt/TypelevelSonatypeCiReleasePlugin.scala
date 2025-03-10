/*
 * Copyright 2022 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.sbt

import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._
import org.typelevel.sbt.gha.GitHubActionsPlugin
import sbt._

object TypelevelSonatypeCiReleasePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlCiReleaseTags = settingKey[Boolean](
      "Controls whether or not v-prefixed tags should be released from CI (default true)")
    lazy val tlCiReleaseBranches = settingKey[Seq[String]](
      "The branches in your repository to release from in CI on every push. Depending on your versioning scheme, they will be either snapshots or (hash) releases. Leave this empty if you only want CI releases for tags. (default: [])")
  }

  import autoImport._

  override def requires = TypelevelSonatypePlugin && GitHubActionsPlugin &&
    GenerativePlugin

  override def trigger = allRequirements

  override def globalSettings =
    Seq(tlCiReleaseTags := true, tlCiReleaseBranches := Seq())

  override def buildSettings = Seq(
    githubWorkflowEnv ++= List(
      "SONATYPE_USERNAME",
      "SONATYPE_PASSWORD",
      "SONATYPE_CREDENTIAL_HOST").map(k => k -> s"$${{ secrets.$k }}").toMap,
    githubWorkflowPublishTargetBranches := {
      val branches =
        tlCiReleaseBranches.value.map(b => RefPredicate.Equals(Ref.Branch(b)))

      val tags =
        if (tlCiReleaseTags.value)
          Seq(RefPredicate.StartsWith(Ref.Tag("v")))
        else
          Seq.empty

      tags ++ branches
    },
    githubWorkflowTargetTags += "v*",
    githubWorkflowPublish := Seq(
      WorkflowStep.Sbt(List("tlRelease"), name = Some("Publish"))
    )
  )
}
