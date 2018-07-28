/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.vcs.internal.services;

import org.gradle.api.GradleException;
import org.gradle.cache.CacheAccess;
import org.gradle.cache.CacheRepository;
import org.gradle.cache.FileLockManager;
import org.gradle.cache.PersistentCache;
import org.gradle.cache.internal.CleanupActionFactory;
import org.gradle.cache.internal.LeastRecentlyUsedCacheCleanup;
import org.gradle.cache.internal.SingleDepthFilesFinder;
import org.gradle.internal.concurrent.Stoppable;
import org.gradle.internal.resource.local.ModificationTimeFileAccessTimeJournal;
import org.gradle.util.GFileUtils;
import org.gradle.vcs.VersionControlSpec;
import org.gradle.vcs.git.GitVersionControlSpec;
import org.gradle.vcs.git.internal.GitVersionControlSystem;
import org.gradle.vcs.internal.VcsDirectoryLayout;
import org.gradle.vcs.internal.VersionControlSystem;
import org.gradle.vcs.internal.VersionControlSystemFactory;
import org.gradle.vcs.internal.VersionRef;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;

import static org.gradle.cache.internal.LeastRecentlyUsedCacheCleanup.DEFAULT_MAX_AGE_IN_DAYS_FOR_RECREATABLE_CACHE_ENTRIES;
import static org.gradle.cache.internal.filelock.LockOptionsBuilder.mode;

public class DefaultVersionControlSystemFactory implements VersionControlSystemFactory, Stoppable {
    private final PersistentCache vcsWorkingDirCache;

    public DefaultVersionControlSystemFactory(VcsDirectoryLayout directoryLayout, CacheRepository cacheRepository, CleanupActionFactory cleanupActionFactory) {
        this.vcsWorkingDirCache = cacheRepository
            .cache(directoryLayout.getCheckoutDir())
            .withLockOptions(mode(FileLockManager.LockMode.None))
            .withDisplayName("VCS Checkout Cache")
            .withCleanup(cleanupActionFactory.create(new LeastRecentlyUsedCacheCleanup(new SingleDepthFilesFinder(1), new ModificationTimeFileAccessTimeJournal(), DEFAULT_MAX_AGE_IN_DAYS_FOR_RECREATABLE_CACHE_ENTRIES)))
            .open();
    }

    @Override
    public VersionControlSystem create(VersionControlSpec spec) {
        // TODO: Register these mappings somewhere
        VersionControlSystem vcs;
        if (spec instanceof GitVersionControlSpec) {
            vcs = new GitVersionControlSystem();
        } else {
            throw new IllegalArgumentException(String.format("Don't know how to create a VCS from spec %s.", spec));
        }
        return new LockingVersionControlSystem(vcs, vcsWorkingDirCache);
    }

    @Override
    public void stop() {
        vcsWorkingDirCache.close();
    }

    private static final class LockingVersionControlSystem implements VersionControlSystem {
        private final VersionControlSystem delegate;
        private final CacheAccess cacheAccess;

        private LockingVersionControlSystem(VersionControlSystem delegate, CacheAccess cacheAccess) {
            this.delegate = delegate;
            this.cacheAccess = cacheAccess;
        }

        @Override
        public VersionRef getDefaultBranch(VersionControlSpec spec) {
            try {
                return delegate.getDefaultBranch(spec);
            } catch (Exception e) {
                throw new GradleException(String.format("Could not locate default branch for %s.", spec.getDisplayName()), e);
            }
        }

        @Nullable
        @Override
        public VersionRef getBranch(VersionControlSpec spec, String branch) {
            try {
                return delegate.getBranch(spec, branch);
            } catch (Exception e) {
                throw new GradleException(String.format("Could not locate branch '%s' for %s.", branch, spec.getDisplayName()), e);
            }
        }

        @Override
        public Set<VersionRef> getAvailableVersions(VersionControlSpec spec) {
            try {
                return delegate.getAvailableVersions(spec);
            } catch (Exception e) {
                throw new GradleException(String.format("Could not list available versions for %s.", spec.getDisplayName()), e);
            }
        }

        @Override
        public void populate(final File workingDir, final VersionRef ref, final VersionControlSpec spec) {
            cacheAccess.useCache(new Runnable() {
                @Override
                public void run() {
                    try {
                        GFileUtils.mkdirs(workingDir);
                        delegate.populate(workingDir, ref, spec);
                    } catch (Exception e) {
                        throw new GradleException(String.format("Could not populate %s from %s.", workingDir, spec.getDisplayName()), e);
                    }
                }
            });
        }
    }
}