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

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.icgc.dcc.common.core.util.Joiners.PATH;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.isDirectory;
import static org.icgc.dcc.download.core.model.DownloadFileType.DIRECTORY;
import static org.icgc.dcc.download.core.model.DownloadFileType.FILE;
import static org.icgc.dcc.download.server.utils.DfsPaths.getFileName;
import static org.icgc.dcc.download.server.utils.DownloadDirectories.DATA_DIR;
import static org.icgc.dcc.download.server.utils.DownloadDirectories.HEADERS_DIR;
import static org.icgc.dcc.download.server.utils.DownloadDirectories.SUMMARY_FILES;
import static org.icgc.dcc.download.server.utils.Releases.getActualReleaseName;
import static org.icgc.dcc.download.server.utils.Responses.throwPathNotFoundException;

import java.util.List;
import java.util.Map.Entry;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.DownloadDataType;
import org.icgc.dcc.common.hadoop.fs.HadoopUtils;
import org.icgc.dcc.download.core.model.DownloadFile;
import org.icgc.dcc.download.server.service.FileSystemService;

@Slf4j
public class ReleaseView extends AbstractFileSystemView {

  public ReleaseView(FileSystem fileSystem, @NonNull FileSystemService fsService,
      @NonNull PathResolver pathResolver) {
    super(fileSystem, fsService, pathResolver);
  }

  public List<DownloadFile> listRelease(@NonNull String releaseName) {
    if (fsService.isLegacyRelease(releaseName)) {
      return listLegacy("/" + releaseName);
    }

    val current = "current".equals(releaseName);
    val actualReleaseName = current ? currentRelease : releaseName;
    val hdfsPath = pathResolver.toHdfsPath("/" + actualReleaseName);
    ensurePathExistence(hdfsPath);

    val releaseFiles = HadoopUtils.lsAll(fileSystem, hdfsPath);
    val downloadFiles = releaseFiles.stream()
        .filter(file -> isDfsEntity(file) == false)
        .map(file -> convert2DownloadFile(file, current))
        .collect(toList());

    downloadFiles.add(createProjectsDir(releaseName));
    downloadFiles.add(createSummaryDir(releaseName));

    return downloadFiles.stream()
        .sorted()
        .collect(toImmutableList());
  }

  public List<DownloadFile> listReleaseProjects(@NonNull String releaseName) {
    if (fsService.isLegacyRelease(releaseName)) {
      val path = "/" + PATH.join(releaseName, "Projects");

      return listLegacy(path);
    }

    val actualReleaseName = getActualReleaseName(releaseName, currentRelease);
    val projects = fsService.getReleaseProjects(actualReleaseName);
    if (!projects.isPresent()) {
      throwPathNotFoundException(format("Release '%s' doesn't exist.", actualReleaseName));
    }
    val releaseDate = getReleaseDate(actualReleaseName);

    return projects.get().stream()
        .map(project -> format("/%s/Projects/%s", releaseName, project))
        .map(path -> createDownloadDir(path, releaseDate))
        .sorted()
        .collect(toImmutableList());
  }

  public List<DownloadFile> listReleaseSummary(@NonNull String releaseName) {
    if (fsService.isLegacyRelease(releaseName)) {
      val path = "/" + PATH.join(releaseName, "Summary");

      return listLegacy(path);
    }

    val actualReleaseName = getActualReleaseName(releaseName, currentRelease);
    val releaseDate = getReleaseDate(actualReleaseName);
    val clinicalSizes = fsService.getClinicalSizes(actualReleaseName);

    val clinicalFiles = clinicalSizes.entrySet().stream()
        .map(entry -> createSummaryFile(releaseName, entry, releaseDate))
        .collect(toImmutableList());
    log.debug("Clinical Files: {}", clinicalFiles);

    val summaryFiles = getSummaryFiles(releaseName);
    log.debug("Summary files: {}", summaryFiles);
    summaryFiles.addAll(clinicalFiles);

    return summaryFiles.stream()
        .sorted()
        .collect(toImmutableList());
  }

  public List<DownloadFile> listProject(@NonNull String releaseName, @NonNull String project) {
    if (fsService.isLegacyRelease(releaseName)) {
      val path = "/" + PATH.join(releaseName, "Projects", project);

      return listLegacy(path);
    }

    val actualReleaseName = getActualReleaseName(releaseName, currentRelease);
    val releaseDate = getReleaseDate(actualReleaseName);
    val projectSizes = fsService.getProjectSizes(actualReleaseName, project);

    // No need to sort the output files, as the input is already sorted
    return projectSizes.entrySet().stream()
        .map(entry -> createProjectFile(entry, releaseName, project, releaseDate))
        .sorted()
        .collect(toImmutableList());

  }

  private DownloadFile createProjectFile(Entry<DownloadDataType, Long> entry, String releaseName, String project,
      long releaseDate) {
    val type = entry.getKey();
    val name = format("%s.%s.tsv.gz", getFileName(type, empty()), project);
    val path = format("/%s/Projects/%s/%s", releaseName, project, name);
    val size = entry.getValue();

    return createDownloadFile(path, size, releaseDate);
  }

  private List<DownloadFile> getSummaryFiles(String releaseName) {
    val actualReleaseName = getActualReleaseName(releaseName, currentRelease);
    val files = HadoopUtils.lsFile(fileSystem, getSummaryFilesPath(actualReleaseName));

    return files.stream()
        .map(file -> createSummaryFile(file, releaseName))
        .collect(toList());
  }

  private DownloadFile createSummaryFile(Path file, String releaseName) {
    log.debug("Creating summary file for '{}'", file);
    val fileName = file.getName();
    val path = format("/%s/Summary/%s", releaseName, fileName);

    return createDownloadFile(file, path);
  }

  private DownloadFile createDownloadFile(Path file, String downloadFilePath) {
    val status = getFileStatus(file);
    val type = isDirectory(fileSystem, file) ? DIRECTORY : FILE;
    val size = type == FILE ? status.getLen() : 0L;
    val creationDate = status.getModificationTime();

    return createDownloadFile(downloadFilePath, type, size, creationDate);
  }

  private Path getSummaryFilesPath(String releaseName) {
    val summaryPath = new Path(PATH.join(pathResolver.getRootDir(), releaseName, SUMMARY_FILES));
    log.debug("'{}' summary path: {}", releaseName, summaryPath);

    return summaryPath;
  }

  private DownloadFile createSummaryDir(String releaseName) {
    return createDownloadDir(format("/%s/Summary", releaseName), releaseName);
  }

  private DownloadFile createProjectsDir(String releaseName) {
    return createDownloadDir(format("/%s/Projects", releaseName), releaseName);
  }

  private DownloadFile createSummaryFile(String releaseName, Entry<DownloadDataType, Long> entry, long releaseDate) {
    val type = entry.getKey();
    val size = entry.getValue();
    val fileName = format("%s.all_projects.tsv.gz", type.getId());
    val path = format("/%s/Summary/%s", releaseName, fileName);

    return createDownloadFile(path, size, releaseDate);
  }

  private void ensurePathExistence(Path hdfsPath) {
    if (!HadoopUtils.exists(fileSystem, hdfsPath)) {
      throwPathNotFoundException(format("File not exists: '%s'", hdfsPath));
    }
  }

  private List<DownloadFile> listLegacy(String relativePath) {
    val path = pathResolver.toHdfsPath(relativePath);
    val allFiles = HadoopUtils.lsAll(fileSystem, path);

    return allFiles.stream()
        .map(file -> createDownloadFile(file, pathResolver.toDfsPath(file)))
        .collect(toImmutableList());
  }

  private static boolean isDfsEntity(Path file) {
    val fileName = file.getName();

    return fileName.equals(DATA_DIR) || fileName.equals(HEADERS_DIR) || fileName.equals(SUMMARY_FILES);
  }

}