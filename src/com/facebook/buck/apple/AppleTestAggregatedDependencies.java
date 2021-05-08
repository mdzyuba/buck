/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.buck.apple;

import com.facebook.buck.apple.toolchain.AppleCxxPlatform;
import com.facebook.buck.apple.toolchain.ApplePlatform;
import com.facebook.buck.core.build.buildable.context.BuildableContext;
import com.facebook.buck.core.build.context.BuildContext;
import com.facebook.buck.core.filesystems.RelPath;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.impl.BuildTargetPaths;
import com.facebook.buck.core.rulekey.AddToRuleKey;
import com.facebook.buck.core.rules.ActionGraphBuilder;
import com.facebook.buck.core.rules.BuildRule;
import com.facebook.buck.core.rules.BuildRuleResolver;
import com.facebook.buck.core.rules.common.BuildableSupport;
import com.facebook.buck.core.rules.impl.AbstractBuildRule;
import com.facebook.buck.core.sourcepath.ExplicitBuildTargetSourcePath;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.toolchain.tool.Tool;
import com.facebook.buck.io.filesystem.BuildCellRelativePath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.io.filesystem.impl.ProjectFilesystemUtils;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.fs.MakeCleanDirectoryStep;
import com.facebook.buck.step.isolatedsteps.common.WriteFileIsolatedStep;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * Creates a directory containing the static resources along with a merged static library containing
 * all of the symbols that a test target depends on. Useful for use with external build systems like
 * Xcode.
 */
public class AppleTestAggregatedDependencies extends AbstractBuildRule {

  private static final String RESOURCES_BASENAME = "resources";
  private static final String CODE_BASENAME = "code";

  private final Path aggregationRoot;
  @AddToRuleKey private final ImmutableList<SourcePath> staticLibDeps;
  @AddToRuleKey private final AppleBundleResources resources;
  @AddToRuleKey private final ApplePlatform applePlatform;
  @AddToRuleKey private final Tool libTool;
  @AddToRuleKey private final boolean withDownwardApi;
  @AddToRuleKey private final SourcePath processedResourceDir;
  private BuildableSupport.DepsSupplier depsSupplier;

  AppleTestAggregatedDependencies(
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      ActionGraphBuilder graphBuilder,
      Path aggregationRoot,
      AppleBundleResources resources,
      AppleCxxPlatform appleCxxPlatform,
      ImmutableList<SourcePath> staticLibDeps,
      boolean withDownwardApi,
      SourcePath processedResourceDir) {
    super(buildTarget, projectFilesystem);
    this.aggregationRoot = aggregationRoot;
    this.resources = resources;
    this.applePlatform = appleCxxPlatform.getAppleSdk().getApplePlatform();
    this.libTool = appleCxxPlatform.getLibtool();
    this.staticLibDeps = ImmutableList.copyOf(staticLibDeps);
    this.withDownwardApi = withDownwardApi;
    this.processedResourceDir = processedResourceDir;
    this.depsSupplier = BuildableSupport.buildDepsSupplier(this, graphBuilder);
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context, BuildableContext buildableContext) {
    ImmutableList.Builder<Step> stepsBuilder = ImmutableList.builder();

    stepsBuilder.addAll(
        MakeCleanDirectoryStep.of(
            BuildCellRelativePath.fromCellRelativePath(
                context.getBuildCellRootPath(), getProjectFilesystem(), aggregationRoot)));

    Path resourcesDir = aggregationRoot.resolve(RESOURCES_BASENAME);
    stepsBuilder.addAll(
        MakeCleanDirectoryStep.of(
            BuildCellRelativePath.fromCellRelativePath(
                context.getBuildCellRootPath(), getProjectFilesystem(), resourcesDir)));

    Path codeDir = aggregationRoot.resolve(CODE_BASENAME);
    stepsBuilder.addAll(
        MakeCleanDirectoryStep.of(
            BuildCellRelativePath.fromCellRelativePath(
                context.getBuildCellRootPath(), getProjectFilesystem(), codeDir)));

    AppleResourceProcessing.addStepsToCopyResources(
        context,
        stepsBuilder,
        resources,
        ImmutableList.of(),
        resourcesDir,
        AppleBundleDestinations.platformDestinations(applePlatform),
        getProjectFilesystem(),
        processedResourceDir,
        () -> false,
        Optional::empty,
        Optional.empty(),
        ImmutableList::of);

    if (staticLibDeps.size() > 0) {
      RelPath argsFile =
          BuildTargetPaths.getScratchPath(getProjectFilesystem(), getBuildTarget(), "argsfile.tmp");
      String output =
          staticLibDeps.stream()
              .map(t -> context.getSourcePathResolver().getAbsolutePath(t).toString())
              .collect(Collectors.joining("\n"));
      stepsBuilder.add(WriteFileIsolatedStep.of(output, argsFile.getPath(), false));
      stepsBuilder.add(
          new LibtoolStep(
              getProjectFilesystem(),
              ProjectFilesystemUtils.relativize(
                  getProjectFilesystem().getRootPath(), context.getBuildCellRootPath()),
              libTool.getEnvironment(context.getSourcePathResolver()),
              libTool.getCommandPrefix(context.getSourcePathResolver()),
              argsFile.getPath(),
              codeDir.resolve("TEST_DEPS.a"),
              ImmutableList.of("-no_warning_for_no_symbols"),
              LibtoolStep.Style.STATIC,
              withDownwardApi));
    }

    return stepsBuilder.build();
  }

  @Override
  public SourcePath getSourcePathToOutput() {
    return ExplicitBuildTargetSourcePath.of(getBuildTarget(), aggregationRoot);
  }

  @Override
  public SortedSet<BuildRule> getBuildDeps() {
    return depsSupplier.get();
  }

  @Override
  public void updateBuildRuleResolver(BuildRuleResolver ruleResolver) {
    this.depsSupplier = BuildableSupport.buildDepsSupplier(this, ruleResolver);
  }
}
