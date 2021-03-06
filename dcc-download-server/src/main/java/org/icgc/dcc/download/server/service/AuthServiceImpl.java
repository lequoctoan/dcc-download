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
package org.icgc.dcc.download.server.service;

import static org.icgc.dcc.download.server.utils.Responses.throwForbiddenException;

import java.util.Collection;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  /**
   * Constants.
   */
  private static final String VALID_SCOPE = "portal.export";

  /**
   * Dependencies.
   */
  private final RemoteTokenServices remoteTokenServices;

  @Override
  public boolean isAuthorized(@NonNull String token) {
    try {
      val auth = remoteTokenServices.loadAuthentication(token);
      val scopes = auth.getOAuth2Request().getScope();

      return isAuthorized(scopes);
    } catch (AuthenticationException | InvalidTokenException e) {
      log.warn("Failed to verify token '{}'. Exception:\n{}", token, e);
      throwForbiddenException();
    }

    return false;
  }

  private static boolean isAuthorized(Collection<String> scopes) {
    return scopes.contains(VALID_SCOPE);
  }

}
