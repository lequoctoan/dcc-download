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
package org.icgc.dcc.download.job.function;

import static org.icgc.dcc.common.core.model.FieldNames.DONOR_SPECIMEN_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_SPECIMEN_ID;

import java.util.Map;

import lombok.val;

import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.sql.Row;

import scala.Tuple2;

import com.google.common.collect.ImmutableMap;

public final class PairByDonorProjectSpecimen implements
    PairFunction<Tuple2<Map<String, String>, Row>, Map<String, String>, Row> {

  @Override
  public Tuple2<Map<String, String>, Row> call(Tuple2<Map<String, String>, Row> tuple)
      throws Exception {
    val row = tuple._2;
    String specimenId = row.getAs(DONOR_SPECIMEN_ID);
    String submissionSpecimenId = row.getAs(SUBMISSION_SPECIMEN_ID);

    val resolvedValues = tuple._1;
    val specimenResolvedValues = ImmutableMap.<String, String> builder()
        .putAll(resolvedValues)
        .put(DONOR_SPECIMEN_ID, specimenId)
        .put(SUBMISSION_SPECIMEN_ID, submissionSpecimenId)
        .build();

    return new Tuple2<Map<String, String>, Row>(specimenResolvedValues, row);
  }

}