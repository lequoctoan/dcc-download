/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.download.server.fs;

import static com.google.common.collect.ImmutableList.of;
import static org.icgc.dcc.common.core.model.DownloadDataType.DONOR;
import static org.icgc.dcc.common.core.model.DownloadDataType.SAMPLE;
import static org.icgc.dcc.common.core.model.DownloadDataType.SSM_OPEN;
import static org.icgc.dcc.common.hadoop.fs.FileSystems.getDefaultLocalFileSystem;
import static org.mockito.Mockito.when;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.download.server.config.Properties;
import org.icgc.dcc.download.server.endpoint.BadRequestException;
import org.icgc.dcc.download.server.endpoint.NotFoundException;
import org.icgc.dcc.download.server.service.FileSystemService;
import org.icgc.dcc.download.server.utils.AbstractFsTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ReleaseViewTest extends AbstractFsTest {

  private static final String PROJECT_NAME = "TST1-CA";
  private static final String FAKE_PROJECT = "fake-CA";
  private static final String CURRENT_RELEASE = "release_21";

  @Mock
  FileSystemService fsService;

  ReleaseView releaseView;

  @Before
  @Override
  public void setUp() {
    super.setUp();
    val properties = new Properties.JobProperties();
    properties.setInputDir(workingDir.getAbsolutePath());
    val pathResolver = new PathResolver(properties);

    this.releaseView = new ReleaseView(getDefaultLocalFileSystem(), fsService, pathResolver);

    when(fsService.getReleaseDate(CURRENT_RELEASE)).thenReturn(Optional.of(321L));
    when(fsService.getReleaseDate("current")).thenReturn(Optional.of(321L));
  }

  @Test
  public void testListRelease_releaseName() throws Exception {
    val files = releaseView.listRelease(CURRENT_RELEASE);
    log.info("Files: {}", files);
    verifyDownloadFiles(files, of(
        newFile("/release_21/README.txt", 13L, getModificationTime("release_21/README.txt")),
        newDir("/release_21/Projects", 321L),
        newDir("/release_21/Summary", 321L)));
  }

  @Test
  public void testListRelease_current() throws Exception {
    val files = releaseView.listRelease("current");
    log.info("{}", files);
    verifyDownloadFiles(files, of(
        newFile("/current/README.txt", 13L, getModificationTime("release_21/README.txt")),
        newDir("/current/Projects", 321L),
        newDir("/current/Summary", 321L)));
  }

  @Test(expected = NotFoundException.class)
  public void testListRelease_invalid() throws Exception {
    releaseView.listRelease("bogus");
  }

  @Test
  public void testListReleaseProjects() throws Exception {
    when(fsService.getReleaseProjects(CURRENT_RELEASE)).thenReturn(
        Optional.of(ImmutableList.of(PROJECT_NAME, "TST2-CA")));

    val files = releaseView.listReleaseProjects(CURRENT_RELEASE);
    verifyDownloadFiles(files, of(
        newFile("/release_21/Projects/README.txt", 17L, getModificationTime("release_21/projects_files/README.txt")),
        newDir("/release_21/Projects/TST1-CA", 321L),
        newDir("/release_21/Projects/TST2-CA", 321L)));
  }

  @Test
  public void testListReleaseProjects_current() throws Exception {
    when(fsService.getReleaseProjects(CURRENT_RELEASE)).thenReturn(
        Optional.of(ImmutableList.of(PROJECT_NAME, "TST2-CA")));

    val files = releaseView.listReleaseProjects("current");
    verifyDownloadFiles(files, of(
        newFile("/current/Projects/README.txt", 17L, getModificationTime("release_21/projects_files/README.txt")),
        newDir("/current/Projects/TST1-CA", 321L),
        newDir("/current/Projects/TST2-CA", 321L)));
  }

  @Test(expected = NotFoundException.class)
  public void testListReleaseProjects_invalid() throws Exception {
    when(fsService.getReleaseProjects("bogus")).thenReturn(Optional.empty());
    releaseView.listReleaseProjects("bogus");
  }

  @Test
  public void testListReleaseSummary() throws Exception {
    when(fsService.getClinicalSizes(CURRENT_RELEASE)).thenReturn(ImmutableMap.of(SAMPLE, 2L, DONOR, 8L));

    val files = releaseView.listReleaseSummary(CURRENT_RELEASE);
    verifyDownloadFiles(files,
        of(
            newFile("/release_21/Summary/README.txt", 22, getModificationTime("release_21/summary_files/README.txt")),
            newFile("/release_21/Summary/donor.all_projects.tsv.gz", 8, 321),
            newFile("/release_21/Summary/sample.all_projects.tsv.gz", 2, 321),
            newFile("/release_21/Summary/simple_somatic_mutation.aggregated.vcf.gz", 8,
                getModificationTime("release_21/summary_files/simple_somatic_mutation.aggregated.vcf.gz"))
        ));
  }

  @Test
  public void testListReleaseSummary_current() throws Exception {
    when(fsService.getClinicalSizes(CURRENT_RELEASE)).thenReturn(ImmutableMap.of(SAMPLE, 2L, DONOR, 8L));

    val files = releaseView.listReleaseSummary("current");
    log.info("Files: {}", files);
    verifyDownloadFiles(files,
        of(
            newFile("/current/Summary/README.txt", 22, getModificationTime("release_21/summary_files/README.txt")),
            newFile("/current/Summary/donor.all_projects.tsv.gz", 8, 321),
            newFile("/current/Summary/sample.all_projects.tsv.gz", 2, 321),
            newFile("/current/Summary/simple_somatic_mutation.aggregated.vcf.gz", 8,
                getModificationTime("release_21/summary_files/simple_somatic_mutation.aggregated.vcf.gz"))
        ));
  }

  @Test(expected = NotFoundException.class)
  public void testListReleaseSummary_invalid() throws Exception {
    when(fsService.getReleaseDate("bogus")).thenReturn(Optional.empty());

    releaseView.listReleaseSummary("bogus");
  }

  @Test
  public void testListProject() throws Exception {
    when(fsService.getProjectSizes(CURRENT_RELEASE, PROJECT_NAME)).thenReturn(
        ImmutableMap.of(DONOR, 8L, SAMPLE, 2L, SSM_OPEN, 10L));
    when(fsService.existsProject(CURRENT_RELEASE, PROJECT_NAME)).thenReturn(true);

    val tst1Files = releaseView.listProject(CURRENT_RELEASE, PROJECT_NAME);
    verifyDownloadFiles(tst1Files,
        of(
            newFile("/release_21/Projects/TST1-CA/donor.TST1-CA.tsv.gz", 8, 321),
            newFile("/release_21/Projects/TST1-CA/sample.TST1-CA.tsv.gz", 2, 321),
            newFile("/release_21/Projects/TST1-CA/simple_somatic_mutation.open.TST1-CA.tsv.gz", 10, 321)
        ));
  }

  @Test
  public void testListProject_current() throws Exception {
    when(fsService.getProjectSizes(CURRENT_RELEASE, PROJECT_NAME)).thenReturn(
        ImmutableMap.of(DONOR, 8L, SAMPLE, 2L, SSM_OPEN, 10L));
    when(fsService.existsProject(CURRENT_RELEASE, PROJECT_NAME)).thenReturn(true);

    val tst1Files = releaseView.listProject("current", PROJECT_NAME);
    verifyDownloadFiles(tst1Files,
        of(
            newFile("/current/Projects/TST1-CA/donor.TST1-CA.tsv.gz", 8, 321),
            newFile("/current/Projects/TST1-CA/sample.TST1-CA.tsv.gz", 2, 321),
            newFile("/current/Projects/TST1-CA/simple_somatic_mutation.open.TST1-CA.tsv.gz", 10, 321)
        ));
  }

  @Test(expected = NotFoundException.class)
  public void testListProject_invalidRelease() throws Exception {
    when(fsService.getReleaseDate("bogus")).thenReturn(Optional.empty());
    when(fsService.getProjectSizes(CURRENT_RELEASE, PROJECT_NAME)).thenReturn(
        ImmutableMap.of(DONOR, 8L, SAMPLE, 2L, SSM_OPEN, 10L));

    releaseView.listProject("bogus", FAKE_PROJECT);
  }

  @Test(expected = NotFoundException.class)
  public void testListProject_invalidProject() throws Exception {
    when(fsService.getReleaseDate(CURRENT_RELEASE)).thenReturn(Optional.of(123L));
    when(fsService.existsProject(CURRENT_RELEASE, FAKE_PROJECT)).thenReturn(false);

    releaseView.listProject(CURRENT_RELEASE, FAKE_PROJECT);
  }

  @Test(expected = NotFoundException.class)
  public void testListLegacy_notExist() throws Exception {
    releaseView.listLegacy("/fake");
  }

  @Test(expected = BadRequestException.class)
  public void testListLegacy_notDirectory() throws Exception {
    releaseView.listLegacy("/legacy_releases/README_file.txt");
  }

}
