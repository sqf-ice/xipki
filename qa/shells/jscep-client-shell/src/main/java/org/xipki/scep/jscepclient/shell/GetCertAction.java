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

package org.xipki.scep.jscepclient.shell;

import java.math.BigInteger;
import java.security.cert.CertStore;
import java.security.cert.X509Certificate;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.jscep.client.Client;
import org.xipki.shell.CmdFailure;
import org.xipki.shell.completer.DerPemCompleter;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "xi", name = "jscep-getcert", description = "download certificate")
@Service
public class GetCertAction extends ClientAction {

  @Option(name = "--serial", aliases = "-s", required = true, description = "serial number")
  private String serialNumber;

  @Option(name = "--outform", description = "output format of the certificate")
  @Completion(DerPemCompleter.class)
  protected String outform = "der";

  @Option(name = "--out", aliases = "-o", required = true,
      description = "where to save the certificate")
  @Completion(FileCompleter.class)
  private String outputFile;

  @Override
  protected Object execute0() throws Exception {
    Client client = getScepClient();
    BigInteger serial = toBigInt(serialNumber);
    CertStore certs = client.getCertificate(getIdentityCert(), getIdentityKey(), serial, null);
    X509Certificate cert = extractEeCerts(certs);

    if (cert == null) {
      throw new CmdFailure("received no certificate from server");
    }

    saveVerbose("saved returned certificate to file", outputFile,
        encodeCert(cert.getEncoded(), outform));
    return null;
  }

}
