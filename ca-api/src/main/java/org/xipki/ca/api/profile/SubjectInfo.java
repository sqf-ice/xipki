/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.api.profile;

import org.bouncycastle.asn1.x500.X500Name;
import org.xipki.util.ParamUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class SubjectInfo {

  private final X500Name grantedSubject;

  private final String warning;

  public SubjectInfo(X500Name grantedSubject, String warning) {
    this.grantedSubject = ParamUtil.requireNonNull("grantedSubject", grantedSubject);
    this.warning = warning;
  }

  public X500Name getGrantedSubject() {
    return grantedSubject;
  }

  public String getWarning() {
    return warning;
  }

}
