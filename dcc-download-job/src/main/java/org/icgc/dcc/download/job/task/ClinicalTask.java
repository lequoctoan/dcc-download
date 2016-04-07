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
package org.icgc.dcc.download.job.task;

import static org.icgc.dcc.common.core.model.FieldNames.DONOR_SAMPLE;
import static org.icgc.dcc.common.core.model.FieldNames.DONOR_SPECIMEN;
import static org.icgc.dcc.common.core.util.Separators.EMPTY_STRING;
import static org.icgc.dcc.common.core.util.Separators.UNDERSCORE;
import lombok.val;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.icgc.dcc.download.core.model.DownloadDataType;
import org.icgc.dcc.download.job.function.ConvertDonor;
import org.icgc.dcc.download.job.function.ConvertNestedDonor;
import org.icgc.dcc.download.job.function.PairByDonorProject;
import org.icgc.dcc.download.job.function.PairByDonorProjectSpecimen;
import org.icgc.dcc.download.job.function.UnwindRow;

import com.google.common.collect.ImmutableList;

public class ClinicalTask extends Task {

  @Override
  public void execute(TaskContext taskContext) {
    val input = readInput(taskContext);
    val donors = filterDonors(input, taskContext.getDonorIds())
        .javaRDD();

    donors.cache();

    val dataTypes = taskContext.getDataTypes();
    if (dataTypes.contains(DownloadDataType.DONOR)) {
      writeDonors(taskContext, donors);
    }

    if (dataTypes.contains(DownloadDataType.DONOR_EXPOSURE)) {
      writeDonorNestedType(taskContext, donors, DownloadDataType.DONOR_EXPOSURE);
    }

    if (dataTypes.contains(DownloadDataType.DONOR_FAMILY)) {
      writeDonorNestedType(taskContext, donors, DownloadDataType.DONOR_FAMILY);
    }

    if (dataTypes.contains(DownloadDataType.DONOR_THERAPY)) {
      writeDonorNestedType(taskContext, donors, DownloadDataType.DONOR_THERAPY);
    }

    if (dataTypes.contains(DownloadDataType.SPECIMEN)) {
      writeDonorNestedType(taskContext, donors, DownloadDataType.SPECIMEN);
    }

    if (dataTypes.contains(DownloadDataType.SAMPLE)) {
      writeSample(taskContext, donors);
    }

    donors.unpersist();
  }

  private void writeDonors(TaskContext taskContext, JavaRDD<Row> donors) {
    val dataType = DownloadDataType.DONOR;
    val header = getHeader(taskContext.getSparkContext(), dataType);
    val records = donors.map(new ConvertDonor());
    val output = header.union(records);

    writeOutput(dataType, taskContext, output);
  }

  private void writeDonorNestedType(TaskContext taskContext, JavaRDD<Row> donors, DownloadDataType dataType) {
    val unwindPath = resolveDonorNestedPath(dataType);
    val donorNestedType = donors.mapToPair(new PairByDonorProject())
        .flatMapValues(new UnwindRow(ImmutableList.of(unwindPath)));

    val header = getHeader(taskContext.getSparkContext(), dataType);
    val records = donorNestedType.map(new ConvertNestedDonor(dataType));
    val output = header.union(records);

    writeOutput(dataType, taskContext, output);
  }

  private void writeSample(TaskContext taskContext, JavaRDD<Row> donors) {
    val dataType = DownloadDataType.SAMPLE;
    val sample = donors.mapToPair(new PairByDonorProject())
        .flatMapValues(new UnwindRow(ImmutableList.of(DONOR_SPECIMEN)))
        .mapToPair(new PairByDonorProjectSpecimen())
        .flatMapValues(new UnwindRow(ImmutableList.of(DONOR_SAMPLE)));

    val header = getHeader(taskContext.getSparkContext(), dataType);
    val records = sample.map(new ConvertNestedDonor(dataType));
    val output = header.union(records);

    writeOutput(dataType, taskContext, output);
  }

  private static String resolveDonorNestedPath(DownloadDataType dataType) {
    val nestedName = dataType.getId();
    val donorName = DownloadDataType.DONOR.getId();

    return nestedName.replace(donorName + UNDERSCORE, EMPTY_STRING);
  }

  private DataFrame readInput(TaskContext taskContext) {
    val sparkContext = taskContext.getSparkContext();
    val sqlContext = new SQLContext(sparkContext);
    val inputPath = taskContext.getInputDir() + "/" + DownloadDataType.DONOR.getId();

    return sqlContext.read().parquet(inputPath);
  }

}