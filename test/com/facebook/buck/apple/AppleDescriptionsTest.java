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

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.BuildTargetFactory;
import com.facebook.buck.core.rules.BuildRuleResolver;
import com.facebook.buck.core.rules.resolver.impl.TestActionGraphBuilder;
import com.facebook.buck.core.sourcepath.FakeSourcePath;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.cxx.CxxLibraryDescriptionArg;
import com.facebook.buck.rules.coercer.SourceSortedSet;
import com.facebook.buck.rules.macros.StringWithMacros;
import com.facebook.buck.util.environment.Platform;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class AppleDescriptionsTest {

  @Before
  public void setUp() {
    Assume.assumeThat(Platform.detect(), not(Platform.WINDOWS));
  }

  @Test
  public void parseAppleHeadersForUseFromOtherTargetsFromSet() {
    SourcePathResolverAdapter resolver = new TestActionGraphBuilder().getSourcePathResolver();
    assertEquals(
        ImmutableMap.<String, SourcePath>of(
            "prefix/some_file.h", FakeSourcePath.of("path/to/some_file.h"),
            "prefix/another_file.h", FakeSourcePath.of("path/to/another_file.h"),
            "prefix/a_file.h", FakeSourcePath.of("different/path/to/a_file.h"),
            "prefix/file.h", FakeSourcePath.of("file.h")),
        AppleDescriptions.parseAppleHeadersForUseFromOtherTargets(
            BuildTargetFactory.newInstance("//:foobar"),
            resolver::getCellUnsafeRelPath,
            Paths.get("prefix"),
            SourceSortedSet.ofUnnamedSources(
                ImmutableSortedSet.of(
                    FakeSourcePath.of("path/to/some_file.h"),
                    FakeSourcePath.of("path/to/another_file.h"),
                    FakeSourcePath.of("different/path/to/a_file.h"),
                    FakeSourcePath.of("file.h")))));
  }

  @Test
  public void parseAppleHeadersForUseFromTheSameFromSet() {
    SourcePathResolverAdapter resolver = new TestActionGraphBuilder().getSourcePathResolver();
    assertEquals(
        ImmutableMap.<String, SourcePath>of(
            "some_file.h", FakeSourcePath.of("path/to/some_file.h"),
            "another_file.h", FakeSourcePath.of("path/to/another_file.h"),
            "a_file.h", FakeSourcePath.of("different/path/to/a_file.h"),
            "file.h", FakeSourcePath.of("file.h")),
        AppleDescriptions.parseAppleHeadersForUseFromTheSameTarget(
            BuildTargetFactory.newInstance("//:foobar"),
            resolver::getCellUnsafeRelPath,
            SourceSortedSet.ofUnnamedSources(
                ImmutableSortedSet.of(
                    FakeSourcePath.of("path/to/some_file.h"),
                    FakeSourcePath.of("path/to/another_file.h"),
                    FakeSourcePath.of("different/path/to/a_file.h"),
                    FakeSourcePath.of("file.h")))));
  }

  @Test
  public void parseAppleHeadersForUseFromOtherTargetsFromMap() {
    ImmutableSortedMap<String, SourcePath> headerMap =
        ImmutableSortedMap.of(
            "virtual/path.h", FakeSourcePath.of("path/to/some_file.h"),
            "another/path.h", FakeSourcePath.of("path/to/another_file.h"),
            "another/file.h", FakeSourcePath.of("different/path/to/a_file.h"),
            "file.h", FakeSourcePath.of("file.h"));
    SourcePathResolverAdapter resolver = new TestActionGraphBuilder().getSourcePathResolver();
    assertEquals(
        headerMap,
        AppleDescriptions.parseAppleHeadersForUseFromOtherTargets(
            BuildTargetFactory.newInstance("//:foobar"),
            resolver::getCellUnsafeRelPath,
            Paths.get("prefix"),
            SourceSortedSet.ofNamedSources(headerMap)));
  }

  @Test
  public void parseAppleHeadersForUseFromTheSameTargetFromMap() {
    ImmutableSortedMap<String, SourcePath> headerMap =
        ImmutableSortedMap.of(
            "virtual/path.h", FakeSourcePath.of("path/to/some_file.h"),
            "another/path.h", FakeSourcePath.of("path/to/another_file.h"),
            "another/file.h", FakeSourcePath.of("different/path/to/a_file.h"),
            "file.h", FakeSourcePath.of("file.h"));
    SourcePathResolverAdapter resolver = new TestActionGraphBuilder().getSourcePathResolver();
    assertEquals(
        ImmutableMap.of(),
        AppleDescriptions.parseAppleHeadersForUseFromTheSameTarget(
            BuildTargetFactory.newInstance("//:foobar"),
            resolver::getCellUnsafeRelPath,
            SourceSortedSet.ofNamedSources(headerMap)));
  }

  @Test
  public void convertToFlatCxxHeadersWithPrefix() {
    SourcePathResolverAdapter resolver = new TestActionGraphBuilder().getSourcePathResolver();
    assertEquals(
        ImmutableMap.<String, SourcePath>of(
            "prefix/some_file.h", FakeSourcePath.of("path/to/some_file.h"),
            "prefix/another_file.h", FakeSourcePath.of("path/to/another_file.h"),
            "prefix/a_file.h", FakeSourcePath.of("different/path/to/a_file.h"),
            "prefix/file.h", FakeSourcePath.of("file.h")),
        AppleDescriptions.convertToFlatCxxHeaders(
            BuildTargetFactory.newInstance("//:foobar"),
            Paths.get("prefix"),
            resolver::getCellUnsafeRelPath,
            ImmutableSet.of(
                FakeSourcePath.of("path/to/some_file.h"),
                FakeSourcePath.of("path/to/another_file.h"),
                FakeSourcePath.of("different/path/to/a_file.h"),
                FakeSourcePath.of("file.h"))));
  }

  @Test
  public void convertToFlatCxxHeadersWithoutPrefix() {
    SourcePathResolverAdapter resolver = new TestActionGraphBuilder().getSourcePathResolver();
    assertEquals(
        ImmutableMap.<String, SourcePath>of(
            "some_file.h", FakeSourcePath.of("path/to/some_file.h"),
            "another_file.h", FakeSourcePath.of("path/to/another_file.h"),
            "a_file.h", FakeSourcePath.of("different/path/to/a_file.h"),
            "file.h", FakeSourcePath.of("file.h")),
        AppleDescriptions.convertToFlatCxxHeaders(
            BuildTargetFactory.newInstance("//:foobar"),
            Paths.get(""),
            resolver::getCellUnsafeRelPath,
            ImmutableSet.of(
                FakeSourcePath.of("path/to/some_file.h"),
                FakeSourcePath.of("path/to/another_file.h"),
                FakeSourcePath.of("different/path/to/a_file.h"),
                FakeSourcePath.of("file.h"))));
  }

  @Test
  public void testModularFlags() {
    BuildRuleResolver resolver = new TestActionGraphBuilder();
    CxxLibraryDescriptionArg.Builder cxxLibraryDescriptionArgBuilder =
        CxxLibraryDescriptionArg.builder();
    cxxLibraryDescriptionArgBuilder.setName("hello");
    AppleLibraryDescriptionArg.Builder appleLibraryDescriptionArgBuilder =
        AppleLibraryDescriptionArg.builder();
    appleLibraryDescriptionArgBuilder.setModular(true);
    appleLibraryDescriptionArgBuilder.setModuleName("MyModule");
    appleLibraryDescriptionArgBuilder.setName("hello");
    appleLibraryDescriptionArgBuilder.setUsesModules(true);
    BuildTarget target = BuildTargetFactory.newInstance("//:hello");

    AppleDescriptions.populateCxxLibraryDescriptionArg(
        resolver,
        cxxLibraryDescriptionArgBuilder,
        Optional.empty(),
        appleLibraryDescriptionArgBuilder.build(),
        target,
        false);

    CxxLibraryDescriptionArg arg = cxxLibraryDescriptionArgBuilder.build();

    assertTrue(arg.getCompilerFlags().contains(StringWithMacros.ofConstantString("-fmodules")));
    assertTrue(arg.getCompilerFlags().contains(StringWithMacros.ofConstantString("-fcxx-modules")));
    assertTrue(
        arg.getCompilerFlags().contains(StringWithMacros.ofConstantString("-fmodule-name=hello")));
  }
}
