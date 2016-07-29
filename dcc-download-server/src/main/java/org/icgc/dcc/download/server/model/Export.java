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
package org.icgc.dcc.download.server.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Locale.ENGLISH;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.download.server.model.ExportAccess.CONTROLLED;
import static org.icgc.dcc.download.server.model.ExportAccess.OPEN;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public enum Export {

  REPOSITORY("repository.tar.gz", OPEN),
  DATA_OPEN("data.open.tar", OPEN),
  DATA_CONTROLLED("data.controlled.tar", CONTROLLED),
  RELEASE("release.tar", OPEN);

  private final String id;
  private final ExportAccess access;

  public String getType() {
    val name = name().toLowerCase(ENGLISH);
    val suffixIndex = name.indexOf("_");

    return suffixIndex == -1 ? name : name.substring(0, suffixIndex);
  }

  public boolean isControlled() {
    return CONTROLLED == access;
  }

  public static Export fromId(@NonNull String id) {
    val export = stream(values())
        .filter(value -> value.getId().equals(id))
        .findFirst();

    if (!export.isPresent()) {
      throw new IllegalArgumentException(format("Failed to resolve export from id '%s'", id));
    }

    return export.get();
  }

}
